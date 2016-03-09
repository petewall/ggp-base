package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;

public class StateEvaluator {
    private Role myRole;

    public StateEvaluator(Role myRole) {
        this.myRole = myRole;
    }

    public int evaluateState(MachineState state, StateMachine stateMachine) throws GoalDefinitionException {
        if (stateMachine.isTerminal(state)) {
            return stateMachine.getGoal(state, myRole);
        }
        return 50;
    }
}
