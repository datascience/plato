/**
 * 
 */
package eu.scape_project.pw.planning.plato.wf;

import javax.ejb.Stateful;
import javax.enterprise.context.ConversationScoped;

import eu.planets_project.pp.plato.model.PlanState;

/**
 * Business logic for workflow step Take Go Decision
 * 
 * @author Markus Hamm, Michael Kraxner
 * 
 */
@Stateful
@ConversationScoped
public class TakeGoDecision extends AbstractWorkflowStep {
    private static final long serialVersionUID = 2756349796229463787L;

    public TakeGoDecision() {
        requiredPlanState = PlanState.ALTERNATIVES_DEFINED;
        correspondingPlanState = PlanState.GO_CHOSEN;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.scape_project.pw.planning.plato.wf.AbstractWorkflowStep#saveStepSpecific
     * ()
     */
    @Override
    protected void saveStepSpecific() {
        // TODO Auto-generated method stub
    }

}
