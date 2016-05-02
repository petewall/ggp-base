package edu.umn.kylepete.player;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class MiniMaxMovePicker implements MovePicker {

    private int maxDepth;

    public MiniMaxMovePicker(int maxDepth){
    	this.maxDepth = maxDepth;
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
        return evaluateMove(1, move, state, role, stateMachine);
    }

    private double evaluateMove(int depth, Move move, MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        MachineState nextState = stateMachine.getRandomNextState(state, role, move);
        if (stateMachine.isTerminal(nextState)) {
            int goal = stateMachine.getGoal(nextState, role);
            double depthAdjustment = 0.0001 * depth;
            double evaluation = goal * (1-depthAdjustment) + 50 * depthAdjustment;
            log("Goal state " + goal + ".  Returning " + evaluation, depth);
            return evaluation;
        } else if (depth >= maxDepth) {
        	double evaluation = evaluateState(role, nextState);
            log("Hit depth limit. Ran heuristic. Returning " + evaluation, depth);
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
            log("Calculated minimax.  Returning " + (100 - bestEvaluation), depth);
            return 100 - bestEvaluation;
        }
    }

    private void log(String message, Integer depth){
    	StringBuilder sb = new StringBuilder();
    	sb.append("[MM] ");
    	if(depth != null){
            for(int i = 0 ; i < depth; i++){
            	sb.append("    ");
            }
    		sb.append("Depth " + depth + "/" + maxDepth + ": ");
    	}
    	sb.append(message);
    	System.out.println(sb.toString());
    }

    abstract protected double evaluateState(Role role, MachineState state);

    public ScoredMoveSet getScoredMoves(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
         List<Move> moves = stateMachine.getLegalMoves(state, role);
         log("Scoring the list of moves (" + moves + ")", null);
         ScoredMoveSet moveSet = new ScoredMoveSet();

         for (Move move : moves) {
             log("Evaluating move " + move, null);
             double evaluation = evaluateMove(move, state, role, stateMachine);
             moveSet.put(move, evaluation);
         }
         moveSet.normalize();
         StringBuilder sb = new StringBuilder();
         sb.append("Final scores: ");
         for(Move move : moveSet.keySet()){
        	 sb.append(move.toString());
        	 sb.append("=");
        	 sb.append(moveSet.get(move));
        	 sb.append(" ");
         }
         log(sb.toString(), null);
         return moveSet;
    }

    @Override
    public Move pickBestMove(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	ScoredMoveSet moveSet = getScoredMoves(state, role, stateMachine);
    	return moveSet.getBestMove();
    }
}
