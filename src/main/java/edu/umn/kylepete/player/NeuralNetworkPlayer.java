package edu.umn.kylepete.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import edu.umn.kylepete.neuralnetworks.GameNeuralNetwork;
import edu.umn.kylepete.neuralnetworks.GameNeuralNetworkDatabase;

public class NeuralNetworkPlayer extends StateMachineGamer {

	private GameNeuralNetworkDatabase gameNeuralNetworkDatabase;
	private GameNeuralNetwork gameNeuralNetwork;

	@Override
	public String getName() {
		return "NeuralNetworkPlayer";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		long start = System.currentTimeMillis();
		Move selection = null;
		Double max = Double.NEGATIVE_INFINITY;
		// List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Map<Move, List<MachineState>> moves = getStateMachine().getNextStates(getCurrentState(), getRole());
		for (Move move : moves.keySet()) {
			MachineState nextState = moves.get(move).get(0);
			System.out.println("Evaluating move: " + move);
//			System.out.println("Evaluating State: " + nextState.getContents());
			Double value = gameNeuralNetwork.evaluateState(getRole(), nextState);
			if (value != null) {
				System.out.println("  " + value);
			}
			if (value > max) {
				max = value;
				selection = move;
			}
		}
		System.out.println("Playing move with value " + max);

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(new ArrayList<Move>(moves.keySet()), selection, stop - start));
		return selection;
	}

	@Override
	public StateMachine getInitialStateMachine() {
		try {
			this.gameNeuralNetworkDatabase = GameNeuralNetworkDatabase.readFromDefaultFile();
			this.gameNeuralNetwork = this.gameNeuralNetworkDatabase.getGameNeuralNetwork(this.getMatch().getGame());
			System.out.println("Found game knowledge trained from " + this.gameNeuralNetwork.getTrainCount() + " games.");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Random gamer does no game previewing.
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// Random gamer does no metagaming at the beginning of the match.
	}

	@Override
	public void stateMachineStop() {
		// Random gamer does no special cleanup when the match ends normally.
		this.gameNeuralNetworkDatabase.train(getMatch());
		try {
			this.gameNeuralNetworkDatabase.writeToDefaultFile();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
