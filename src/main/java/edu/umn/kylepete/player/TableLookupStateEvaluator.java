package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class TableLookupStateEvaluator implements MovePicker {

    public TableLookupStateEvaluator() {
        // Load the lookup table for this game
    }

    @Override
    public Move pickBestMove(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException {
        // TODO Auto-generated method stub
        return stateMachine.getRandomMove(state, role);
    }
}
