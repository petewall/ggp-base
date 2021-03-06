package edu.umn.kylepete.player;

import java.io.IOException;
import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import edu.umn.kylepete.neuralnetworks.GameNeuralNetwork;
import edu.umn.kylepete.neuralnetworks.GameNeuralNetworkDatabase;
import external.JSON.JSONException;

public class LearningPlayer extends SubAgent {
    private MiniMaxMovePicker picker;

    @Override
    public String getName() {
        return "LearningPlayer";
    }

    @Override
    public ScoredMoveSet scoreValidMoves(long timeout) throws WinningMoveException, MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        return picker.getScoredMoves(getCurrentState(), getRole(), getStateMachine());
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();
        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

        Move selection = picker.pickBestMove(getCurrentState(), getRole(), getStateMachine());
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
		try {
			GameNeuralNetwork gameNeuralNetwork = GameNeuralNetworkDatabase.readFromFile("gameDatabase_8games.json").getGameNeuralNetwork(getMatch().getGame());
			System.out.println("Found game knowledge trained from " + gameNeuralNetwork.getTrainCount() + " games.");
			picker = new MiniMaxNeuralNetworkMovePicker(gameNeuralNetwork, 2);
		} catch (InterruptedException | JSONException | IOException e) {
			throw new RuntimeException(e);
		}
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

    public static void main(String[] args)
    {
        if (args.length != 1) {
            System.err.println("Usage: GamePlayer <port>");
            System.exit(1);
        }

        try {
            GamePlayer player = new GamePlayer(Integer.valueOf(args[0]), new LearningPlayer());
            player.run();
        } catch (NumberFormatException e) {
            System.err.println("Illegal port number: " + args[0]);
            e.printStackTrace();
            System.exit(2);
        } catch (IOException e) {
            System.err.println("IO Exception: " + e);
            e.printStackTrace();
            System.exit(3);
        }
    }
}
