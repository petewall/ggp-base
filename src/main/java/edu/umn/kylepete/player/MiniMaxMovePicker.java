package edu.umn.kylepete.player;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MiniMaxMovePicker implements MovePicker {

    private int maxDepth = 5;

    private Role getNextPlayer(Role role, StateMachine stateMachine) {
        List<Role> roles = stateMachine.getRoles();
        for (Role otherRole : roles) {
            if (!role.equals(otherRole)) {
                return otherRole;
            }
        }
        return role;
    }

    private int evaluateMove(Move move, MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        return evaluateMove(0, move, state, role, stateMachine);
    }

    private int evaluateMove(int depth, Move move, MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        MachineState nextState = stateMachine.getRandomNextState(state, role, move);
        if (stateMachine.isTerminal(nextState)) {
            int evaluation = stateMachine.getGoal(nextState, role);
            System.out.println("[MM] Depth " + depth + "/" + maxDepth + ": Goal state.  Returning " + evaluation);
            return evaluation;
        } else if (depth >= maxDepth) {
            // FIXME, use a better heuristic eventually
            System.out.println("[MM] Depth " + depth + "/" + maxDepth + ": Hit depth limit.  Returning 50");
            return 50;
        } else {
            Role nextPlayer = getNextPlayer(role, stateMachine);
            List<Move> counterMoves = stateMachine.getLegalMoves(nextState, nextPlayer);
            int bestEvaluation = Integer.MIN_VALUE;
            for (Move counterMove : counterMoves) {
                int evaluation = evaluateMove(depth + 1, counterMove, nextState, nextPlayer, stateMachine);
                if (bestEvaluation < evaluation) {
                    bestEvaluation = evaluation;
                }
            }
            System.out.println("[MM] Depth " + depth + "/" + maxDepth + ": Calculated minimax.  Returning " + (100 - bestEvaluation));
            return 100 - bestEvaluation;
        }
    }

    @Override
    public Move pickBestMove(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        int bestEvaluation = Integer.MIN_VALUE;
        List<Move> moves = stateMachine.getLegalMoves(state, role);
        System.out.println("[MM] Picking from the list of moves (" + moves + ")");
        Move bestMove = moves.get(0);

        if (moves.size() == 1) {
            System.out.println("[MM] Only one valid move.  Returing that: " + bestMove);
            return bestMove;
        }

        for (Move move : moves) {
            int evaluation = evaluateMove(move, state, role, stateMachine);
            System.out.println("[MM] Move " + moves + ": " + evaluation);
            if (bestEvaluation < evaluation) {
                bestEvaluation = evaluation;
                bestMove = move;
            }
        }
        return bestMove;
    }
}
