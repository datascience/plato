/*******************************************************************************
 * Copyright 2006 - 2012 Vienna University of Technology,
 * Department of Software Technology and Interactive Systems, IFS
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.scape_project.planning.plato.wf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateful;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.scape_project.planning.exception.PlanningException;
import eu.scape_project.planning.manager.ByteStreamManager;
import eu.scape_project.planning.manager.StorageException;
import eu.scape_project.planning.model.DigitalObject;
import eu.scape_project.planning.model.PlanState;
import eu.scape_project.planning.model.User;
import eu.scape_project.planning.model.measurement.Criterion;
import eu.scape_project.planning.model.measurement.EvaluationScope;
import eu.scape_project.planning.model.tree.Leaf;
import eu.scape_project.planning.model.tree.Node;
import eu.scape_project.planning.model.tree.ObjectiveTree;
import eu.scape_project.planning.model.tree.TreeNode;
import eu.scape_project.planning.xml.ProjectExporter;
import eu.scape_project.planning.xml.TreeLoader;

/**
 * Business logic for workflow step Identify Requirements
 * 
 * @author Markus Hamm, Michael Kraxner
 */
@Stateful
@ConversationScoped
public class IdentifyRequirements extends AbstractWorkflowStep {

    private static final long serialVersionUID = 1014116842177860229L;

    @Inject
    private Logger log;

    @Inject
    private User user;

    @Inject
    private ByteStreamManager bytestreamManager;

    @Inject
    private TreeLoader treeLoader;

    @Inject
    private ProjectExporter projectExporter;

    /**
     * Nodes to delete after accepting the performed changes (before save).
     */
    private List<TreeNode> nodesToDelete = new ArrayList<TreeNode>();

    public IdentifyRequirements() {
        this.requiredPlanState = PlanState.RECORDS_CHOSEN;
        this.correspondingPlanState = PlanState.TREE_DEFINED;
    }

    /**
     * Adds a new Leaf to the given Node.
     * 
     * @param Node
     *            to attach the Leaf to.
     * @return New attached Leaf.
     */
    public Leaf addNewLeaf(Node node) {
        Leaf newLeaf = new Leaf();
        node.addChild(newLeaf);
        // this node has been changed()
        node.touch();
        return newLeaf;
    }

    /**
     * Adds a new Node to the given Node.
     * 
     * @param Node
     *            to attach the Node to.
     * @return New attached Node.
     */
    public Node addNewNode(Node node) {
        Node newNode = new Node();
        node.addChild(newNode);
        // this node has been changed()
        node.touch();
        return node;
    }

    /**
     * Method responsible for removing a given node from its objective tree.
     * 
     * @param node
     *            Node to remove.
     */
    public void removeTreeNode(TreeNode node) {
        if (node.getParent() != null) {
            // parent has been changed
            ((Node) node.getParent()).touch();
            ((Node) node.getParent()).removeChild(node);
        }
        nodesToDelete.add(node);
    }

    /**
     * Converts a given leaf to a node.
     * 
     * @param leaf
     *            Leaf to convert.
     */
    public void convertToNode(Leaf leaf) {
        leaf.getParent().convertToNode(leaf);
    }

    /**
     * Converts a given node to a leaf.
     * 
     * @param node
     *            Node to convert.
     */
    public void convertToLeaf(Node node) {
        node.getParent().convertToLeaf(node);
    }

    /**
     * Method responsible for retrieving a copy of a previously uploaded file
     * including its data.
     * 
     * @param digitalObject
     *            DigitalObject stored in file system for which the data should
     *            be retrieved.
     * @return Copy of the given DigitalObject including its data
     * @throws StorageException
     *             is thrown if any error occurs at retrieving the result file.
     */
    public DigitalObject fetchAttachedFile(DigitalObject digitalObject) throws StorageException {
        return digitalObjectManager.getCopyOfDataFilledDigitalObject(digitalObject);
    }

    /**
     * Method responsible for attaching additional documentation files.
     * 
     * @param digitalObject
     *            Digital object to attach
     * @throws StorageException
     *             is thrown if any error occurs at storing the given file.
     */
    public void attachFile(DigitalObject digitalObject) throws StorageException {
        digitalObjectManager.moveDataToStorage(digitalObject);
        plan.getRequirementsDefinition().getUploads().add(digitalObject);

        addedBytestreams.add(digitalObject.getPid());
    }

    /**
     * Method responsible for removing a previously attached file.
     * 
     * @param digitalObject
     *            Digital object to remove.
     */
    public void removeAttachedFile(DigitalObject digitalObject) {
        plan.getRequirementsDefinition().getUploads().remove(digitalObject);

        bytestreamsToRemove.add(digitalObject.getPid());
    }

    /**
     * Method responsible for importing a requirements tree from a given
     * FreeMind file.
     * 
     * @param file
     *            FreeMind file to import
     * @param includesUnits
     *            Indicates if the given FreeMind tree includes units
     * @return True if import was successful. False otherwise
     */
    public boolean importTreeFromFreeMind(DigitalObject file, boolean includesUnits) {
        log.debug("Start FreeMind import.");
        log.debug("FileName: " + file.getFullname());
        log.debug("HasUnits is: " + includesUnits);

        InputStream istream = new ByteArrayInputStream(file.getData().getData());
        ObjectiveTree newtree = treeLoader.loadFreeMindObjectiveTree(istream, includesUnits, true);

        if (newtree == null) {
            log.error("File is corrupted and new Tree cannot be built out of it.");
            return false;
        }

        // delete old tree
        nodesToDelete.add(plan.getTree().getRoot());

        // set new tree as plan tree
        plan.getTree().setRoot(newtree.getRoot());

        // make sure all scales are set according to measurement infos
        plan.getTree().adjustScalesToMeasurements();
        plan.getTree().setWeightsInitialized(false);

        log.debug("Imported FreeMind file successfully.");

        return true;
    }

    /**
     * Method responsible for exporting the current requirements tree as
     * FreeMind XML.
     * 
     * @return Current requirements tree in FreeMind XML representation. A
     *         String including the XML-data is returned.
     */
    public String exportTreeAsFreeMindXML() {
        return projectExporter.exportTreeToFreemind(plan);
    }

    /**
     * Method responsible for assigning a criterion to a defined
     * leaf/requirement.
     * 
     * @param criterion
     *            Criterion to assign.
     * @param leaf
     *            Wanted leaf.
     */
    public void assignCriterionToLeaf(Criterion criterion, Leaf leaf) {
        leaf.setCriterion(criterion);
        leaf.setScale(criterion.getScale());
        leaf.setSingle(criterion.getProperty().getEvaluationScope() == EvaluationScope.ALTERNATIVE);
        leaf.touchIncludingScale();
    }

    /**
     * Method responsible for detaching a criterion from a leaf.
     * 
     * @param leaf
     *            Leaf to detach the criterion from
     */
    public void detachCriterionFromLeaf(Leaf leaf) {
        leaf.setCriterion(null);
        leaf.touch();
    }

    @Override
    protected void saveStepSpecific() {
        resetTransformers();
        resetLeafValues();

        // this means the reference to the root has been changed, e.g. by
        // useTemplate (I think thats the only case, actually) -
        // so we need to get this ID back (otherwise, each subsequent SAVE will
        // result in a new entity persist)
        // simplest way: reload
        if (plan.getTree().getRoot().getId() == 0) {
            plan.getTree().setRoot(em.merge(plan.getTree().getRoot()));
        }

        prepareChangesForPersist.prepare(plan);

        saveEntity(plan.getRequirementsDefinition());
        saveEntity(plan.getTree());
        deleteNodesToDelete();
    }

    @Override
    public void discard() throws PlanningException {
        super.discard();
        // We have to extend the standard discard function by the mandatory
        // clean-up of "nodes to delete"
        nodesToDelete.clear();
    }

    /**
     * Resets the transformer of all leaves to the default transformer.
     * 
     * @see eu.scape_project.planning.IdentifyRequirements.workflow.IdentifyRequirementsAction#resetTransformers()
     */
    private void resetTransformers() {
        TreeNode root = plan.getTree().getRoot();
        for (Leaf leaf : root.getAllLeaves()) {
            /*
             * maybe the scaletype is not set yet -> leaf.setDefaultTransformer
             * has to handle null-values itself
             */
            if ((leaf.getScale() == null) || (leaf.getScale().isDirty())) {
                leaf.setDefaultTransformer();
            }
        }
    }

    /**
     * Method responsible for resetting leaf values.
     */
    private void resetLeafValues() {
        for (Leaf leaf : plan.getTree().getRoot().getAllLeaves()) {
            leaf.resetValues(plan.getAlternativesDefinition().getConsideredAlternatives());
        }
    }

    /**
     * Method responsible for deleting the nodes deleted in this step from
     * database.
     */
    private void deleteNodesToDelete() {
        for (TreeNode n : nodesToDelete) {
            if (n.getId() != 0) {
                removeEntity(n);
            }
        }

        nodesToDelete.clear();
    }
}
