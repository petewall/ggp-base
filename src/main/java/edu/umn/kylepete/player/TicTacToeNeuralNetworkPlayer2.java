package edu.umn.kylepete.player;

import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.nd4j.linalg.api.ndarray.INDArray;

import edu.umn.kylepete.neuralnetworks.TicTacToeNeuralNetwork2;
import edu.umn.kylepete.neuralnetworks.TicTacToeStateToCSV;
import edu.umn.kylepete.neuralnetworks.TicTacToeStateToCSV2;

public class TicTacToeNeuralNetworkPlayer2 extends StateMachineGamer {

	private MultiLayerNetwork neuralNetwork;

	@Override
	public String getName() {
		return "TicTacToeNeuralNetworkPlayer2";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		Move selection = null;
		double max = Double.NEGATIVE_INFINITY;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		for (Move move : moves) {
			if (move.toString().contains("noop")) {
				if (selection == null) {
					System.out.println("Selecting 'noop' move");
					selection = move;
				}
				continue;
			}
			List<GdlTerm> terms = move.getContents().toSentence().getBody();
			int moveRow = Integer.parseInt(terms.get(0).toString()) - 1;
			int moveCol = Integer.parseInt(terms.get(1).toString()) - 1;

			INDArray output = neuralNetwork.output(TicTacToeStateToCSV.getStateAsArray(getCurrentState().getContents()));
			Double value = output.getDouble(3*moveRow+moveCol);
			System.out.println("Move " + move + " has value " + value);
			if (value > max) {
				max = value;
				selection = move;
			}
		}
		System.out.println("Playing move with value " + max);

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(new ArrayList<Move>(moves), selection, stop - start));
		return selection;
	}

	@Override
	public StateMachine getInitialStateMachine() {
		if (neuralNetwork == null) {
			try {
				if (TicTacToeNeuralNetwork2.TTT_DATA_SET.exists() == false) {
					TicTacToeStateToCSV2.main(null);
				}
				neuralNetwork = TicTacToeNeuralNetwork2.trainNetwork();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
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
