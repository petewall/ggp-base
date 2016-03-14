package edu.umn.kylepete.player;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class WinLoseDrawStateEvaluator implements MovePicker {

    @Override
    public Move pickBestMove(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        List<Move> moves = stateMachine.getLegalMoves(state, role);
        System.out.println("Picking a move from list: " + moves);

        Move bestMove = moves.get(0);
        if (moves.size() == 1) {
            System.out.println("Only one valid move.  Returing that: " + bestMove);
            return bestMove;
        }

        int bestEvaluation = Integer.MIN_VALUE;
        for (Move move : moves) {
//            // Get everybody else's moves
//            List<List<Move>> opponentMoves = stateMachine.getLegalJointMoves(state, getRole(), move);
            MachineState potentialState = stateMachine.getRandomNextState(state, role, move);

            int evaluation = evaluateState(potentialState, role, stateMachine);
            System.out.println("Move " + move + ": " + evaluation);
            if (evaluation > bestEvaluation) {
                bestMove = move;
                bestEvaluation = evaluation;
            }
        }
        System.out.println("Picking a move with a resulting score: " + bestEvaluation + ". Move: " + bestMove);
        return bestMove;

    }

    public int evaluateState(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException {
        if (stateMachine.isTerminal(state)) {
            return stateMachine.getGoal(state, role);
        }
        return 50;
    }
}
