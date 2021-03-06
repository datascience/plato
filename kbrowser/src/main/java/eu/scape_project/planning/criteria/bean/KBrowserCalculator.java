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
package eu.scape_project.planning.criteria.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.scape_project.planning.model.TargetValueObject;
import eu.scape_project.planning.model.kbrowser.CriteriaLeaf;
import eu.scape_project.planning.model.kbrowser.VPlanLeaf;
import eu.scape_project.planning.model.measurement.Attribute;
import eu.scape_project.planning.model.measurement.CriterionCategory;
import eu.scape_project.planning.model.measurement.Measure;
import eu.scape_project.planning.model.transform.NumericTransformer;
import eu.scape_project.planning.model.transform.OrdinalTransformer;
import eu.scape_project.planning.model.transform.TransformationMode;
import eu.scape_project.planning.model.transform.Transformer;
import eu.scape_project.planning.model.values.INumericValue;
import eu.scape_project.planning.model.values.IOrdinalValue;
import eu.scape_project.planning.model.values.Value;

/**
 * Class responsible for calculating statistics for KBrowser.
 * 
 * @author Markus Hamm
 */
public class KBrowserCalculator implements Serializable {
    private static final long serialVersionUID = 3798396219762966856L;

    /**
     * List of all plan leaves.
     */
    private List<VPlanLeaf> planLeaves;

    /**
     * List of plan leaves which are mapped to measures.
     */
    private List<VPlanLeaf> mappedPlanLeaves;

    /**
     * List of plan leaves for a specified measure.
     */
    private List<VPlanLeaf> measurePlanLeaves;

    /**
     * variable responsible for calculating aggregated criteria impact factors.
     */
    private CriteriaLeaf criteriaAggregator;

    public KBrowserCalculator(List<VPlanLeaf> planLeaves, Long nrRelevantPlans) {
        this.planLeaves = planLeaves;

        // will be updated as soon as a criterion is selected
        this.measurePlanLeaves = new ArrayList<VPlanLeaf>();

        this.criteriaAggregator = new CriteriaLeaf(nrRelevantPlans);

        filterMappedLeaves();
    }

    /**
     * Updates the mapped plan leaf list from all plan leaves filtering all
     * unmapped leaves.
     */
    private void filterMappedLeaves() {
        // filter mapped PlanLeaves
        mappedPlanLeaves = new ArrayList<VPlanLeaf>(planLeaves.size());
        for (VPlanLeaf pl : planLeaves) {
            if (pl.isMapped()) {
                mappedPlanLeaves.add(pl);
            }
        }
    }

    /**
     * Method responsible for filtering all leaves which match the given
     * criterion.
     */
    public void filterMeasure(Measure m) {
        // Reset criterion lists
        measurePlanLeaves.clear();

        // Filter PlanLeaves
        for (VPlanLeaf l : mappedPlanLeaves) {
            if (l.getMeasure().getUri().equals(m.getUri())) {
                measurePlanLeaves.add(l);
            }
        }

        criteriaAggregator.setPlanLeaves(measurePlanLeaves);
    }

    /**
     * Returns the number of plan leaves.
     * 
     * @return the number of plan leaves
     */
    public int getNrPlanLeaves() {
        return planLeaves.size();
    }

    /**
     * Returns the number of mapped plan leaves.
     * 
     * @return the number of mapped plan leaves.
     */
    public int getNrMappedPlanLeaves() {
        return mappedPlanLeaves.size();
    }

    /**
     * Returns the number of measures that are used in at least on plan leaf.
     * 
     * @return number of used measures
     */
    public int getNrCriteriaUsedAtLeastOnce() {
        HashMap<String, String> mappedMeasures = new HashMap<String, String>();

        for (VPlanLeaf l : mappedPlanLeaves) {
            mappedMeasures.put(l.getMeasure().getUri(), l.getMeasure().getUri());
        }

        return mappedMeasures.size();
    }

    /**
     * Returns the number of plan leaves that are mapped to a measure in the
     * provided category.
     * 
     * @param category
     *            category to filter for or null for any category.
     * @return the number of plan leaves having the given category assigned.
     */
    public int getNrPlanLeavesInCategory(CriterionCategory category) {
        if (category == null) {
            return mappedPlanLeaves.size();
        }

        int count = 0;
        for (VPlanLeaf l : mappedPlanLeaves) {
            if (l.getMeasure().getAttribute().getCategory().getUri().equals(category.getUri())) {
                count++;
            }
        }

        return count;
    }

    /**
     * Method responsible for counting the plan leaves having the given property
     * assigned, giving no respect to the fact if the property is measurable or
     * not or if a metric is assigned or not.
     * 
     * @param attribute
     *            property to filter for or null for all properties
     */
    public int getNrPlanLeavesUsingProperty(Attribute attribute) {
        if (attribute == null) {
            return mappedPlanLeaves.size();
        }

        int count = 0;
        for (VPlanLeaf l : mappedPlanLeaves) {
            if (l.getMeasure().getAttribute().getUri().equals(attribute.getUri())) {
                count++;
            }
        }

        return count;
    }

    /**
     * Returns the number of plan leaves that are mapped to the set measure.
     * 
     * @return the number of plan leaves
     */
    public int getNrCriterionPlanLeaves() {
        return measurePlanLeaves.size();
    }

    /**
     * Returns the average weight of the plan leaves that are mapped to the set
     * measure.
     * 
     * @return the average weight
     */
    public double getCPLAverageWeight() {
        double sum = 0;
        int count = 0;

        for (VPlanLeaf l : measurePlanLeaves) {
            sum = sum + l.getWeight();
            count++;
        }

        if (count > 0) {
            return sum / count;
        } else {
            return 0;
        }
    }

    /**
     * Returns the number of measurements of the set measure.
     * 
     * @return the number of measurements
     */
    public int getNrCPLMeasurementsObtained() {
        int count = 0;
        for (VPlanLeaf l : measurePlanLeaves) {
            count = count + l.getMeasuredValues().size();
        }
        return count;
    }

    /**
     * Method responsible for returning the minimum numeric measurement of
     * criterion plan leaves.
     * 
     * @return 0 if no numeric measurements are found. Otherwise minimum numeric
     *         measurment of criterion plan leaves is returned.
     */
    public double getCPLNumericMeasurementsMin() {
        Boolean atLeastOneMeasuredValue = false;
        double min = Double.MAX_VALUE;

        for (VPlanLeaf l : measurePlanLeaves) {
            for (Value val : l.getMeasuredValues()) {
                if (val instanceof INumericValue) {
                    atLeastOneMeasuredValue = true;
                    INumericValue numVal = (INumericValue) val;
                    if (numVal.value() < min) {
                        min = numVal.value();
                    }
                }
            }
        }

        if (atLeastOneMeasuredValue) {
            return min;
        } else {
            return 0;
        }
    }

    /**
     * Method responsible for returning the maximum numeric measurment of
     * criterion plan leaves.
     * 
     * @return 0 if no numeric measurements are found. Otherwise maximum numeric
     *         measurment of criterion plan leaves is returned.
     */
    public double getCPLNumericMeasurementsMax() {
        Boolean atLeastOneMeasuredValue = false;
        double max = -Double.MAX_VALUE;

        for (VPlanLeaf l : measurePlanLeaves) {
            for (Value val : l.getMeasuredValues()) {
                if (val instanceof INumericValue) {
                    atLeastOneMeasuredValue = true;
                    INumericValue numVal = (INumericValue) val;
                    if (numVal.value() > max) {
                        max = numVal.value();
                    }
                }
            }
        }

        if (atLeastOneMeasuredValue) {
            return max;
        } else {
            return 0;
        }
    }

    /**
     * Method responsible for returning the avarage numeric measurment of
     * criterion plan leaves.
     * 
     * @return 0 if no numeric measurements are found. Otherwise average numeric
     *         measurment of criterion plan leaves is returned.
     */
    public double getCPLNumericMeasurementsAvg() {
        int count = 0;
        double sum = 0d;

        for (VPlanLeaf l : measurePlanLeaves) {
            for (Value val : l.getMeasuredValues()) {
                if (val instanceof INumericValue) {
                    count++;
                    INumericValue numVal = (INumericValue) val;
                    sum = sum + numVal.value();
                }
            }
        }

        if (count > 0) {
            return sum / count;
        } else {
            return 0;
        }
    }

    /**
     * Method responsible for returning all ordinal MeasuredValues + the number
     * of occurrence.
     * 
     * @return A list of Strings in the form "MeasuredValue : #occurrence".
     */
    public Map<String, Integer> getCPLOrdinalMeasurements() {
        Map<String, Integer> measuredOrdinalValues = new HashMap<String, Integer>();
        // List<String> ordinalMeasurements = new ArrayList<String>();

        // collect ordinal values
        // We have to lower-case values here to get reasonable results (in plans
        // upper- and lower-case strings are used)
        for (VPlanLeaf l : measurePlanLeaves) {
            for (Value val : l.getMeasuredValues()) {
                if (val instanceof IOrdinalValue) {
                    IOrdinalValue valOrd = (IOrdinalValue) val;
                    String ordinalString = new String(valOrd.getValue().toLowerCase());

                    if (measuredOrdinalValues.containsKey(ordinalString)) {
                        Integer oldV = measuredOrdinalValues.get(ordinalString);
                        measuredOrdinalValues.put(ordinalString, oldV + 1);
                    } else {
                        measuredOrdinalValues.put(ordinalString, 1);
                    }
                }
            }
        }

        // // Convert Map<String, Integer> into List<String> with all
        // information encoded.
        // // This is done for output in UI - because in JSF it is hard to
        // output a Map.
        // for (String key : measuredOrdinalValues.keySet()) {
        // ordinalMeasurements.add(key + " : " +
        // measuredOrdinalValues.get(key));
        // }
        //
        // return ordinalMeasurements;

        return measuredOrdinalValues;
    }

    /**
     * Method responsible for creating a table including all
     * transformer-mappings.
     * 
     * @return table including all transformer-mappings.
     */
    public KBrowserTransformerTable getCPSTransformerTable() {
        KBrowserTransformerTable table = new KBrowserTransformerTable();

        for (VPlanLeaf l : measurePlanLeaves) {
            Transformer trans = l.getTransformer();
            if (trans == null) {
                continue;
            }

            // OrdinalTransformer
            if (trans instanceof OrdinalTransformer) {
                table.setOrdinal(true);
                OrdinalTransformer oTrans = (OrdinalTransformer) trans;
                Map<String, TargetValueObject> oTransMapping = oTrans.getMapping();
                Map<Double, String> tableMapping = new HashMap<Double, String>();

                for (String str : oTransMapping.keySet()) {
                    Double tv = oTransMapping.get(str).getValue();
                    table.addTargetValue(tv);

                    if (tableMapping.containsKey(tv)) {
                        String oldValue = tableMapping.get(tv);
                        String newValue = oldValue + ", " + str;
                        tableMapping.put(tv, newValue);
                    } else {
                        tableMapping.put(tv, str);
                    }
                }

                table.addTransformerMapping(tableMapping, "mappings");
            }

            // NumericTransformer
            if (trans instanceof NumericTransformer) {
                table.setOrdinal(false);
                // numeric transformer always have the same thresholds - fixed
                // set
                table.addTargetValue(1.0);
                table.addTargetValue(2.0);
                table.addTargetValue(3.0);
                table.addTargetValue(4.0);
                table.addTargetValue(5.0);

                NumericTransformer nTrans = (NumericTransformer) trans;
                Map<Double, String> tableMapping = new HashMap<Double, String>();
                tableMapping.put(1.0, nTrans.getThreshold1().toString());
                tableMapping.put(2.0, nTrans.getThreshold2().toString());
                tableMapping.put(3.0, nTrans.getThreshold3().toString());
                tableMapping.put(4.0, nTrans.getThreshold4().toString());
                tableMapping.put(5.0, nTrans.getThreshold5().toString());

                TransformationMode tMode = nTrans.getMode();
                table.addTransformerMapping(tableMapping, "thresholds (" + tMode.getName().toLowerCase() + ")");
            }
        }

        return table;
    }

    /**
     * Method responsible for returning all criterion plan leaves - evaluations.
     * 
     * @return all criterion plan leaves - evaluations.
     */
    public List<Double> getCPLEvaluations() {
        List<Double> evaluations = new ArrayList<Double>();

        // for each leaf transform its measured values into transformed values
        for (VPlanLeaf l : measurePlanLeaves) {
            evaluations.addAll(l.getAlternativeResults());
        }

        return evaluations;
    }

    // ---------- Impact Factors ----------

    public double getCPL_IF1() {
        return criteriaAggregator.getImportanceFactorIF1();
    }

    public double getCPL_IF2() {
        return criteriaAggregator.getImportanceFactorIF2();
    }

    public double getCPL_IF3() {
        return criteriaAggregator.getImportanceFactorIF3();
    }

    public double getCPL_IF4() {
        return criteriaAggregator.getImportanceFactorIF4();
    }

    public double getCPL_IF5() {
        return criteriaAggregator.getImportanceFactorIF5();
    }

    public double getCPL_IF6() {
        return criteriaAggregator.getImportanceFactorIF6();
    }

    public double getCPL_IF7() {
        return criteriaAggregator.getImportanceFactorIF7();
    }

    public double getCPL_IF8() {
        return criteriaAggregator.getImportanceFactorIF8();
    }

    public double getCPL_IF9() {
        return criteriaAggregator.getImportanceFactorIF9();
    }

    public double getCPL_IF10() {
        return criteriaAggregator.getImportanceFactorIF10();
    }

    public double getCPL_IF11() {
        return criteriaAggregator.getImportanceFactorIF11();
    }

    public double getCPL_IF12() {
        return criteriaAggregator.getImportanceFactorIF12();
    }

    public double getCPL_IF13() {
        return criteriaAggregator.getImportanceFactorIF13();
    }

    public double getCPL_IF14() {
        return criteriaAggregator.getImportanceFactorIF14();
    }

    public double getCPL_IF15() {
        return criteriaAggregator.getImportanceFactorIF15();
    }

    public double getCPL_IF16() {
        return criteriaAggregator.getImportanceFactorIF16();
    }

    public double getCPL_IF17() {
        return criteriaAggregator.getImportanceFactorIF17();
    }

    public double getCPL_IF18() {
        return criteriaAggregator.getImportanceFactorIF18();
    }
}
