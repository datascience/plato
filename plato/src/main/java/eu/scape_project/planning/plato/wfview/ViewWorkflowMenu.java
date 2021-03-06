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
package eu.scape_project.planning.plato.wfview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Named;

/**
 * Class for grouping of AbstractViews according to their group, 
 * enables to display menus with sub-items   
 * 
 * @author Michael Kraxner
 *
 */
@Named("menu")
@ConversationScoped
public class ViewWorkflowMenu implements Serializable{
	private static final long serialVersionUID = 1L;

	private Map<String, List<AbstractView>> menuItems = new HashMap<String, List<AbstractView>>();
	private List<String> mainmenuItems = new ArrayList<String>(); 

	/**
	 * Adds all steps to the menu.
	 * - Creates {@link #mainmenuItems} for every defined group of the steps,
	 *   and adds the steps as sub menu entries in {@link #menuItems}
	 * - There is a special group "" (empty string) for steps with no defined group.
	 *   it's items are added to {@link #menuItems}, but this group is not listed in mainmenuItems.
	 *   
	 * @param steps
	 */
	public void init(List<AbstractView> steps) {
		if (steps == null || steps.isEmpty()) {
			return;
		}
		mainmenuItems.clear();
		menuItems.clear();
		for (AbstractView step : steps) {
			String group = step.getGroup(); 
			if (group == null) {
				group = "";
			}
			if (!menuItems.containsKey(group)) {
				if (!"".equals(group)) {
					// do not add the empty group to the list of mainmenuItems
					mainmenuItems.add(group);
				}
				menuItems.put(group, new ArrayList<AbstractView>());
			}
			menuItems.get(group).add(step);
		}
	}
	
	/**
	 * Returns the list of views corresponding to the given mainmenu item
	 * 
	 * @param mainmenu
	 * @return
	 */
	public List<AbstractView> getViewItems(String mainmenu){
		return menuItems.get(mainmenu);
	}
	
	/**
	 * Returns a list of all mainmenu items (excluding the empty list)
	 *  
	 * @return
	 */
	public List<String> getMainmenuItems() {
		return mainmenuItems;
	}
	
	
	
	
	
}
