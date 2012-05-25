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
package eu.scape_project.planning.xml.plan;

import org.apache.commons.digester3.AbstractObjectCreationFactory;
import org.xml.sax.Attributes;

/**
 * Helper class for {@link eu.scape_project.planning.xml.ProjectImporter} to create a NumericTransformer-threshold value 
 * with the data of its XML representation. 
 * 
 * @author Michael Kraxner
 *
 */
public class NumericTransformerThresholdFactory extends
        AbstractObjectCreationFactory<Double> {

    @Override
    public Double createObject(Attributes arg0) throws Exception {
        return  Double.parseDouble(arg0.getValue("value"));
    }

}
