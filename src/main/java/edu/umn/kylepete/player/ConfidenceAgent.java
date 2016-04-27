package edu.umn.kylepete.player;

import java.util.ArrayList;
import java.util.List;

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

    private List<SubAgent> subAgents;

    public ConfidenceAgent() {
        subAgents = new ArrayList<SubAgent>();
        subAgents.add(new MultithreadedUCTPlayer());
//        subAgents.add(new UCTPlayer());
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();

        if (getLastMove() != null) {
            for (SubAgent subAgent : subAgents) {
                subAgent.setLastMove(getLastMove());
            }
        }

        Move chosenMove;
        try {
            ScoredMoveSet moveSet = new ScoredMoveSet();
            for (SubAgent subAgent : subAgents) {
                moveSet.combine(subAgent.scoreValidMoves(timeout));
            }
            chosenMove = moveSet.getBestMove();
            System.out.println("Picking from the best of: ");
            for (Move move : moveSet.keySet()) {
                Double score = moveSet.get(move);
                System.out.println(move + " --> " + score);
            }
        } catch (WinningMoveException e) {
            System.out.println("Found a winning move: " + e.move);
            chosenMove = e.move;
        }

        long stop = System.currentTimeMillis();
        List<Move> validMoves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
        notifyObservers(new GamerSelectedMoveEvent(validMoves, chosenMove, stop - start));
        System.out.println("ran for " + (stop - start) / 1000.0 + " seconds");
        return chosenMove;
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        for (SubAgent subAgent : subAgents) {
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
        for (SubAgent subAgent : subAgents) {
            subAgent.stateMachineStop();
        }
    }

    @Override
    public void stateMachineAbort() {
        for (SubAgent subAgent : subAgents) {
            subAgent.stateMachineAbort();
        }
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
        for (SubAgent subAgent : subAgents) {
            subAgent.preview(g, timeout);
        }
    }
}