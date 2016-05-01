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

public class ConfidenceAgent extends StateMachineGamer {
    @Override
    public String getName() {
        return "ConfidenceAgent";
    }

    private Map<SubAgent, SubAgentThread> subAgents;
    private Map<SubAgent, Move> bestMoves;
    private int depth;
    private int maxBranchingFactor;

    public ConfidenceAgent() {
        subAgents = new HashMap<SubAgent, SubAgentThread>();
        subAgents.put(new MultithreadedUCTPlayer(), null);
        subAgents.put(new LearningPlayer(), null);
        bestMoves = new HashMap<SubAgent, Move>();
        depth = 0;
        maxBranchingFactor = 0;
    }

    @Override
    public Move stateMachineSelectMove(final long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        depth++;
        int branchingFactor = getStateMachine().getLegalJointMoves(getCurrentState()).size();
        if (branchingFactor > maxBranchingFactor) {
            maxBranchingFactor = branchingFactor;
        }
        long start = System.currentTimeMillis();

        if (getLastMove() != null) {
            for (SubAgent subAgent : subAgents.keySet()) {
                subAgent.setLastMove(getLastMove());
                subAgent.setCurrentState(getCurrentState());
            }
        }

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
                break;
            }
            ScoredMoveSet subAgentMoveSet = thread.getMoveSet();
            bestMoves.put(subAgent, subAgentMoveSet.getBestMove());
            moveSet.combine(subAgentMoveSet);
        }

        if (chosenMove == null) {
            for (SubAgent subAgent : bestMoves.keySet()) {
                Move bestMove = bestMoves.get(subAgent);
                System.out.println("Best move from " + subAgent.getName() + ": " + bestMove);
            }
            chosenMove = moveSet.getBestMove();
            System.out.println("Picking from the best of: ");
            for (Move move : moveSet.keySet()) {
                Double score = moveSet.get(move);
                System.out.println("    " + move + " --> " + score);
            }
            System.out.println("Chosen move: " + chosenMove);
        }

        long stop = System.currentTimeMillis();
        List<Move> validMoves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
        notifyObservers(new GamerSelectedMoveEvent(validMoves, chosenMove, stop - start));
        System.out.println("ran for " + (stop - start) / 1000.0 + " seconds");
        return chosenMove;
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
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
    public void stateMachineStop() {
        for (SubAgent subAgent : subAgents.keySet()) {
            subAgent.stateMachineStop();
        }
        System.out.println("Game depth: " + depth);
        System.out.println("Max branching factor: " + maxBranchingFactor);
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
}