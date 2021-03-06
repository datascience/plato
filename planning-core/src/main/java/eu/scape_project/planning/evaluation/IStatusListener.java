/*******************************************************************************
 * Copyright 2006 - 2014 Vienna University of Technology,
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
package eu.scape_project.planning.evaluation;

/**
 * An interface to be used in several places in Plato where longer-running
 * processes should provide feedback on the go.
 * 
 * @see RunExperimentsAction#run(Object)
 * @see EvaluateExperimentsAction#evaluateAll()
 */
public interface IStatusListener {

    /**
     * Updates the status of this listener.
     * 
     * @param msg
     *            the status message
     */
    void updateStatus(String msg);
}
