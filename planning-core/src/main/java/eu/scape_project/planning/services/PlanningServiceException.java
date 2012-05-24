/*******************************************************************************
 * Copyright 2012 Vienna University of Technology
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
package eu.scape_project.planning.services;

public class PlanningServiceException extends Exception {

    private static final long serialVersionUID = -3107662456923200745L;

    public PlanningServiceException() {
    }

    public PlanningServiceException(String arg0) {
        super(arg0);
    }

    public PlanningServiceException(Throwable arg0) {
        super(arg0);
    }

    public PlanningServiceException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

}