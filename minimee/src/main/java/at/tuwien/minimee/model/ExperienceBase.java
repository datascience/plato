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
package at.tuwien.minimee.model;

import java.util.HashMap;

import eu.scape_project.planning.model.measurement.Measurement;
import eu.scape_project.planning.model.measurement.ToolExperience;

/**
 * This class provides a very simple experience database for measured properties
 * {@link ToolExperience}
 */
public class ExperienceBase {
    private HashMap<String,ToolExperience> toolExperience = new HashMap<String,ToolExperience>();
    
    /**
     * @param tool identifier of the {@link ToolExperience}
     * @return the {@link ToolExperience} corresponding to the identifier.
     * If there was no ToolExperience defined yet for this ID, a new one will
     * be created and put into the map!
     */
    public ToolExperience getToolExperience(String tool) {
        ToolExperience b = toolExperience.get(tool);
        if (b == null) {
            b = new ToolExperience();
            toolExperience.put(tool, b);
        }
        return b;
    }    
    
    /**
     * adds a new {@link Measurement} to the {@link ToolExperience}
     * identified by the provided id
     * @param tool identifies the {@link ToolExperience} to which to add the measurement
     * @param m
     */
    public void addExperience(String tool,Measurement m) {
        ToolExperience b = getToolExperience(tool);
        b.addMeasurement(m);
    }    
    
    /**
     * @param tool identifies the {@link ToolExperience} 
     * @param m identifies the {@link Measurement} which points to a {@link MeasurableProperty} 
     * for which we want to get the average 
     * @return average of the measurements for this property
     *  {@link ToolExperience#getAverage(String)}
     */
    public Measurement getAverage(String tool, Measurement m) {
        return getToolExperience(tool).getAverage(m.getMeasureId());
    }
}
