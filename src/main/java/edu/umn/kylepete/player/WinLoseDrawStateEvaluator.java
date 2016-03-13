package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;

public class WinLoseDrawStateEvaluator implements StateEvaluator {

    @Override
    public int evaluateState(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException {
        if (stateMachine.isTerminal(state)) {
            return stateMachine.getGoal(state, role);
        }
        return 50;
    }
}
