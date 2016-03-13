package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;

public class TableLookupStateEvaluator implements StateEvaluator {

    public TableLookupStateEvaluator() {
        // Load the lookup table for this game
    }

    @Override
    public int evaluateState(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException {
        // TODO Auto-generated method stub
        return 0;
    }
}
