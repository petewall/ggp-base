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

    private Move pickBestMove(MachineState state, int depth) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();

        List<Move> moves = stateMachine.getLegalMoves(state, getRole());
        Move bestMove = moves.get(0);
        int bestEvaluation = -1;

        for (Move move : moves) {
            // FIXME, this is always picking random moves for our opponents.
            MachineState nextState = stateMachine.getNextState(getCurrentState(), stateMachine.getRandomJointMove(getCurrentState(), getRole(), move));
            if (depth > 0) {
                return pickBestMove(nextState, depth - 1);
            } else {
                int evaluation = evaluator.evaluateState(nextState, stateMachine);
                if (evaluation > bestEvaluation) {
                    bestMove = move;
                    bestEvaluation = evaluation;
                }
            }
        }
        System.out.println("Picking a move (depth " + depth + " with a resulting score: " + bestEvaluation);
        return bestMove;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();
        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

        Move selection = pickBestMove(getCurrentState(), 1);
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
        evaluator = new StateEvaluator(getRole());
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
