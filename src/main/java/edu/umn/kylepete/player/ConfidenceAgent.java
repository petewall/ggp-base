package edu.umn.kylepete.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import external.JSON.JSONException;
import external.JSON.JSONObject;

public class ConfidenceAgent extends StateMachineGamer {
    @Override
    public String getName() {
        return "ConfidenceAgent";
    }

    private Map<SubAgent, SubAgentThread> subAgents;
    private Map<SubAgent, Move> bestMoves;
    private int depth;
    private int minBranchingFactor;
    private double avgBranchingFactor;
    private int maxBranchingFactor;

    public ConfidenceAgent() {

    }

    @Override
    public Move stateMachineSelectMove(final long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, JSONException {
        // Start the clock
        long start = System.currentTimeMillis();
        JSONObject jsonStats = new JSONObject();

        // Update statistical records
        depth++;
        jsonStats.put("depth", depth);
        int branchingFactor = getStateMachine().getLegalJointMoves(getCurrentState()).size();
        int possibleMoves = getStateMachine().getLegalMoves(getCurrentState(), getRole()).size();
        jsonStats.put("possibleMoves", possibleMoves);
        jsonStats.put("branchingFactor", branchingFactor);
        if (branchingFactor > maxBranchingFactor) {
            maxBranchingFactor = branchingFactor;
        }
        avgBranchingFactor = avgBranchingFactor + ((branchingFactor - avgBranchingFactor) / depth);
        if (branchingFactor < minBranchingFactor) {
            minBranchingFactor = branchingFactor;
        }

        // Update the previous move for all agents
        if (getLastMove() != null) {
            for (SubAgent subAgent : subAgents.keySet()) {
                subAgent.setLastMove(getLastMove());
                subAgent.setCurrentState(getCurrentState());
            }
        }

        // Prepare variables
        Move chosenMove = null;
        final ScoredMoveSet moveSet = new ScoredMoveSet();

        // Start searching (create threads)
        for (final SubAgent subAgent : subAgents.keySet()) {
            SubAgentThread thread = new SubAgentThread(subAgent, timeout);
            subAgents.put(subAgent, thread);
            thread.start();
        }

        // Gather results (wait on threads)
        for (final SubAgent subAgent : subAgents.keySet()) {
            SubAgentThread thread = subAgents.get(subAgent);
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Thread for " + subAgent.getName() + " interrupted.");
                continue;
            }

            if (thread.foundWinningMove()) {
            	chosenMove = thread.getWinningMove();
                bestMoves.put(subAgent, chosenMove);
                log("Found a winning move: " + chosenMove);
            }else{
                ScoredMoveSet subAgentMoveSet = thread.getMoveSet();
                bestMoves.put(subAgent, subAgentMoveSet.getBestMove());
                moveSet.combine(subAgentMoveSet);
            }
        }

        // Pick the best move (if a winning move wasn't already found)
        if (chosenMove == null) {
            chosenMove = moveSet.getBestMove();
            log("Picking the best combined of: ");
            for (Move move : moveSet.keySet()) {
                Double score = moveSet.get(move);
                log("    " + move + " --> " + score);
            }
            log("Chosen move: " + chosenMove);
        }

        jsonStats.put("chosenMove", chosenMove.toString());
        boolean allAgentsAgree = true;
        for (SubAgent subAgent : bestMoves.keySet()) {
            Move bestMove = bestMoves.get(subAgent);
            log("Best move from " + subAgent.getName() + ": " + bestMove);
            jsonStats.put("bestMove_" + subAgent.getName(), bestMove.toString());
            boolean agentMoveChosen = true;
            if(!bestMove.toString().equals(chosenMove.toString())){
            	agentMoveChosen = false;
            	allAgentsAgree = false;
            }
            jsonStats.put("chosenMove_" + subAgent.getName(), agentMoveChosen);
        }
        jsonStats.put("allAgentsAgree", allAgentsAgree);
        log("MOVE_STATS: " + jsonStats.toString());

        // Wrap up and submit the move
        long stop = System.currentTimeMillis();
        List<Move> validMoves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
        notifyObservers(new GamerSelectedMoveEvent(validMoves, chosenMove, stop - start));
        log("ran for " + (stop - start) / 1000.0 + " seconds");
        return chosenMove;
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	subAgents = new HashMap<SubAgent, SubAgentThread>();
    	subAgents.put(new MultithreadedUCTPlayer(), null);
    	subAgents.put(new LearningPlayer(), null);
    	bestMoves = new HashMap<SubAgent, Move>();
    	depth = 0;
    	minBranchingFactor = Integer.MAX_VALUE;
    	avgBranchingFactor = 0;
    	maxBranchingFactor = Integer.MIN_VALUE;

    	for (SubAgent subAgent : subAgents.keySet()) {
            subAgent.setMatch(getMatch());
            subAgent.setRoleName(getRoleName());
            subAgent.setStateMachine(getStateMachine());
            subAgent.stateMachineMetaGame(timeout);
        }
    }

    @Override
    public StateMachine getInitialStateMachine() {
        return new CachedStateMachine(new ProverStateMachine());
    }

    @Override
    public void stateMachineStop() throws JSONException {
        for (SubAgent subAgent : subAgents.keySet()) {
            subAgent.stateMachineStop();
        }
        log("Game depth: " + depth);
        log("Min branching factor: " + minBranchingFactor);
        log("Avg branching factor: " + avgBranchingFactor);
        log("Max branching factor: " + maxBranchingFactor);

        JSONObject jsonStats = new JSONObject();
        jsonStats.put("gameDepth", depth);
        jsonStats.put("minBranchingFactor", minBranchingFactor);
        jsonStats.put("avgBranchingFactor", avgBranchingFactor);
        jsonStats.put("maxBranchingFactor", maxBranchingFactor);
        log("GAME_STATS: " + jsonStats.toString());
    }

    @Override
    public void stateMachineAbort() {
        for (SubAgent subAgent : subAgents.keySet()) {
            subAgent.stateMachineAbort();
        }
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
        for (SubAgent subAgent : subAgents.keySet()) {
            subAgent.preview(g, timeout);
        }
    }

    private void log(String message){
    	System.out.println("[Confidence] " + message);
    }
}