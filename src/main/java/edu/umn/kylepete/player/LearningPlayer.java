package edu.umn.kylepete.player;

import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class LearningPlayer extends StateMachineGamer {
    private StateEvaluator evaluator;

    @Override
    public String getName() {
        return "LearningPlayer";
    }

    private Move pickBestMove(MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();

        List<Move> moves = stateMachine.getLegalMoves(state, getRole());
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
            MachineState potentialState = stateMachine.getRandomNextState(state, getRole(), move);

            int evaluation = evaluator.evaluateState(potentialState, getRole(), stateMachine);
            if (evaluation > bestEvaluation) {
                bestMove = move;
                bestEvaluation = evaluation;
            }
        }
        System.out.println("Picking a move with a resulting score: " + bestEvaluation + ". Move: " + bestMove);
        return bestMove;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();
        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

        Move selection = pickBestMove(getCurrentState());
        long stop = System.currentTimeMillis();

        notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
        return selection;
    }

    @Override
    public StateMachine getInitialStateMachine() {
        return new CachedStateMachine(new ProverStateMachine());
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
        // Random gamer does no game previewing.
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        evaluator = new WinLoseDrawStateEvaluator();
    }

    @Override
    public void stateMachineStop() {
        // Random gamer does no special cleanup when the match ends normally.
    }

    @Override
    public void stateMachineAbort() {
        // Random gamer does no special cleanup when the match ends abruptly.
    }

    @Override
    public DetailPanel getDetailPanel() {
        return new SimpleDetailPanel();
    }
}
