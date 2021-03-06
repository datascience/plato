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
 * 
 * This work originates from the Planets project, co-funded by the European Union under the Sixth Framework Programme.
 ******************************************************************************/
package eu.scape_project.planning.model.tree;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;



/**
 * Stores a template or fragment-tree.
 * 
 * @author Kevin Stadler
 */
@Entity
public class TemplateTree implements Serializable {

    private static final long serialVersionUID = -2789020719146177603L;

    @Id
    @GeneratedValue
    private int id;
    
    /**
     * Name of the tree, e.g. "Public Templates", "Organization A's Fragments" etc..
     */
    private String name;

    /**
     * Owner of this template-tree. null if public template
     */
    // TODO properly bind private template/fragment trees to a user and enable private templates/fragments.
    // Needs a better UI too - separate page for that.
    // TODO how can we store that a template belongs to an organization rather than a user? Add a new property?
    private String owner;

    /**
     * Root node of this template-tree. The root node's name is usually the name of the template tree + " Root"
     * @see #setName(String)
     */
    @ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.EAGER)
    private Node root;

    /**
     * Default constructor for Hibernate
     */
    public TemplateTree() {
    }

    /**
     * Constructor for first creation of the template-tree. Gives the tree a 
     * name and initializes a root node.
     * @param name the name of the tree
     * @param owner owner of the tree or null - parameter-type should be changed once IF-roles are in place, see{@link #owner}
     */
    public TemplateTree(String name, String owner) {
        this.setName(name); // This call will initialize a root node as well
        this.setOwner(owner);
    }

    public String getName() {
        return name;
    }

    public Node getRoot() {
        return root;
    }

    /**
     * Needed for Template-Import-Digester
     * @see ProjectImporter#addTreeParsingRulesToDigester(Digester)
     */
    public void addChild(TreeNode n) {
        this.getRoot().addChild(n);
    }

    public String getOwner() {
        return owner;
    }

    /**
     * Sets the name of this template tree and creates a new root node called 
     * this.name + " Root" if it doesn't exist yet.
     */
    public void setName(String name) {
        this.name = name;
        if (this.root == null) {
            this.root = new Node();
            this.root.setName(name);
        }
    }   

    public void setRoot(Node root) {
        this.root = root;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
