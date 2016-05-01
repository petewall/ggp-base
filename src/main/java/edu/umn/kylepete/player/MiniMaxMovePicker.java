package edu.umn.kylepete.player;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import edu.umn.kylepete.neuralnetworks.GameNeuralNetwork;

public class MiniMaxMovePicker implements MovePicker {

    private int maxDepth;
    private GameNeuralNetwork neuralNetwork;

    public MiniMaxMovePicker(GameNeuralNetwork neuralNetwork, int maxDepth){
    	this.maxDepth = maxDepth;
    	this.neuralNetwork = neuralNetwork;
    }

    private Role getNextPlayer(Role role, StateMachine stateMachine) {
        List<Role> roles = stateMachine.getRoles();
        for (Role otherRole : roles) {
            if (!role.equals(otherRole)) {
                return otherRole;
            }
        }
        return role;
    }

    private double evaluateMove(Move move, MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        return evaluateMove(0, move, state, role, stateMachine);
    }

    private double evaluateMove(int depth, Move move, MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        MachineState nextState = stateMachine.getRandomNextState(state, role, move);
        if (stateMachine.isTerminal(nextState)) {
            int evaluation = stateMachine.getGoal(nextState, role);
            System.out.println("[MM] Depth " + depth + "/" + maxDepth + ": Goal state.  Returning " + evaluation);
            return evaluation;
        } else if (depth >= maxDepth) {
        	double evaluation = neuralNetwork.evaluateState(role, nextState);
            System.out.println("[MM] Depth " + depth + "/" + maxDepth + ": Hit depth limit. Running neural network. Returning " + evaluation);
            return evaluation;
        } else {
            Role nextPlayer = getNextPlayer(role, stateMachine);
            List<Move> counterMoves = stateMachine.getLegalMoves(nextState, nextPlayer);
            double bestEvaluation = Double.NEGATIVE_INFINITY;
            for (Move counterMove : counterMoves) {
                double evaluation = evaluateMove(depth + 1, counterMove, nextState, nextPlayer, stateMachine);
                if (bestEvaluation < evaluation) {
                    bestEvaluation = evaluation;
                }
            }
            System.out.println("[MM] Depth " + depth + "/" + maxDepth + ": Calculated minimax.  Returning " + (100 - bestEvaluation));
            return 100 - bestEvaluation;
        }
    }

    public ScoredMoveSet getScoredMoves(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
         List<Move> moves = stateMachine.getLegalMoves(state, role);
         System.out.println("[MM] Scoring the list of moves (" + moves + ")");
         ScoredMoveSet moveSet = new ScoredMoveSet();

         for (Move move : moves) {
             double evaluation = evaluateMove(move, state, role, stateMachine);
             System.out.println("[MM] Move " + move + ": " + evaluation);
             moveSet.put(move, evaluation);
         }
         moveSet.normalize();
         return moveSet;
    }

    @Override
    public Move pickBestMove(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	ScoredMoveSet moveSet = getScoredMoves(state, role, stateMachine);
    	return moveSet.getBestMove();
    }
}
