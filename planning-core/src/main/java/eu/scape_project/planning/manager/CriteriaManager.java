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
package eu.scape_project.planning.manager;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remove;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

import eu.scape_project.planning.model.measurement.Attribute;
import eu.scape_project.planning.model.measurement.Measure;
import eu.scape_project.planning.model.scales.BooleanScale;
import eu.scape_project.planning.model.scales.FloatScale;
import eu.scape_project.planning.model.scales.FreeStringScale;
import eu.scape_project.planning.model.scales.OrdinalScale;
import eu.scape_project.planning.model.scales.PositiveFloatScale;
import eu.scape_project.planning.model.scales.PositiveIntegerScale;
import eu.scape_project.planning.model.scales.Scale;

/**
 * For administration of metrics, measurable properties and criteria This should
 * be the interface to a Measurement Property Registry (MPR) - the registry
 * should be queried for all measurement entities - this would prevent entities
 * being overwritten by accident, and ease notification on changed entities -
 * changes to already known entities should trigger events for preservation
 * watch
 * 
 * @author kraxner
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Startup
@Named("criteriaManager")
public class CriteriaManager implements Serializable {
    private static final long serialVersionUID = -2305838596050068452L;
    
    public static final String MEASURES_DIR = "data/measures";
    public static final String MEASURES_FILE = "attributes_measures.rdf";

    @Inject
    private Logger log;

    @PersistenceContext
    private EntityManager em;
    
    private Model model;

    public CriteriaManager() {
    	model = ModelFactory.createMemModelMaker().createDefaultModel();
    }

    /**
     * cache for lookup of all currently known measures by their id
     * 
     */
    private Map<String, Measure> knownMeasures = new HashMap<String, Measure>();

    /**
     * cache for lookup of all currently known attributes by their id
     */
    private Map<String, Attribute> knownAttributes = new HashMap<String, Attribute>();

    /**
     * Returns a list of all known criteria IMPORTANT: this list MUST NOT be
     * altered!
     * 
     * @return
     */
    @Lock(LockType.READ)
    public Collection<Measure> getAllMeasures() {
        return knownMeasures.values();
    }

    /**
     * returns a list of all known properties IMPORTANT: this list MUST NOT be
     * altered!
     * 
     * @return
     */
    @Lock(LockType.READ)
    public Collection<Attribute> getAllAttributes() {
        return knownAttributes.values();
    }

    /**
     * Returns the criterion for the given criterionUri
     * 
     * @param uri
     * @return
     */
    @Lock(LockType.READ)
    public Measure getMeasure(String measureUri) {
        for (Measure measure : knownMeasures.values()) {
            if (measure.getUri().equals(measureUri)) {
                return measure;
            }
        }
        return null;
    }

    /**
     * loads all existing properties, metrics, and criteria from the database
     */
    private void load() {
    }
    
    private void resolveAttributes() {
        String statement=      
			"SELECT ?a ?an ?ad WHERE { " +
			"?a rdf:type pw:Attribute . " +
			"?a rdfs:label ?an . " +
			"?a pw:description ?ad . }";

        String commonNS = 
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX pw: <http://scape-project.eu/pw/vocab/>  ";
        
        Query query = QueryFactory.create(commonNS + statement, Syntax.syntaxARQ);
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();

        while ((results != null) && (results.hasNext())) {
        	QuerySolution qs = results.next();
        	
            Attribute a = new Attribute();
            
            a.setDescription(qs.getLiteral("ad").getString());
            a.setName(qs.getLiteral("an").getString());
            a.setUri(qs.getResource("a").toString());
            
            knownAttributes.put(a.getUri(), a);
        }
        
    }
    
    private void resolveMeasures () {
        String statement=
        		"SELECT ?m ?mn ?md ?a ?s WHERE { " +
        		"?m rdf:type pw:Measure . " +
        		"?m pw:attribute ?a . " +
        		"?m rdfs:label ?mn . " +
        		"?m pw:description ?md ." +
        		"?m pw:scale ?s . }";

        String commonNS = 
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX pw: <http://scape-project.eu/pw/vocab/>  ";
        
        Query query = QueryFactory.create(commonNS + statement, Syntax.syntaxARQ);
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();

        while ((results != null) && (results.hasNext())) {
        	QuerySolution qs = results.next();
        	
            Resource attribute = qs.getResource("a");
            
            String attributeUri = attribute.toString(); 
            
            Measure m = new Measure();
            
            m.setUri(qs.getResource("m").toString());
            m.setName(qs.getLiteral("mn").getString());
            m.setDescription(qs.getLiteral("md").getString());
            
            m.setScale(createScale(qs.getResource("s").getLocalName()));
            
            Attribute a = knownAttributes.get(attributeUri);
            
            m.setAttribute(a);
            
            knownMeasures.put(m.getUri(), m);
        }
    }
    
    Scale createScale(String scaleName) {
    	
    	if ("Boolean".equalsIgnoreCase(scaleName)) {
    		return new BooleanScale();
    	} else if ("Free Text".equalsIgnoreCase(scaleName)) {
    		return new FreeStringScale();
    	} else if ("Number".equalsIgnoreCase(scaleName)) {
    		return new FloatScale();
    	} else if ("Positive Number".equalsIgnoreCase(scaleName)) {
    		return new PositiveFloatScale();
    	} else if ("Positive Integer".equalsIgnoreCase(scaleName)) {
    		return new PositiveIntegerScale();
    	} else if ("Ordinal".equalsIgnoreCase(scaleName)) {
    		return new OrdinalScale();
    	}
    	
    	return null;
    }

    /**
     * FIXME: reload from RDF
     * 
     * Reads the XML file from {@link #DESCRIPTOR_FILE} and adds the contained
     * criteria to the database. For criteria that already exist in the database
     * (as designated by URI), the information is updated.
     * 
     * @see eu.scape_project.planning.application.ICriteriaManager#reload()
     *      ATTENTION: From all available CRUD operation only CReate and Update
     *      are covered. Delete operations are not executed. Thus, if you have
     *      deleted Properties in your XML they are not deleted in database as
     *      well.
     */
    @Lock(LockType.WRITE)
    public void reload() {
    	String dir = MEASURES_DIR + "/" + MEASURES_FILE; 
    	
    	model = FileManager.get().loadModel(dir);
    	
        resolveAttributes();
        resolveMeasures();    	
    }

    // Method used for testing purposes (mocking the EntityManager)
    public void setEm(EntityManager em) {
        this.em = em;
    }

    @PostConstruct
    public void init() {
        load();
        if (knownMeasures.isEmpty()) {
            reload();
        }

    }

    @Remove
    public void destroy() {
    }
    
    public static void main (String[] args) {
    	CriteriaManager criteriaManager = new CriteriaManager();
    	
    	criteriaManager.init();
    }
}
