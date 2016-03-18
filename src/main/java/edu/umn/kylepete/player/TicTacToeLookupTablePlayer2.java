package edu.umn.kylepete.player;

import java.util.List;
import java.util.Map;

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

import edu.umn.kylepete.neuralnetworks.TicTacToeBoard;
import edu.umn.kylepete.neuralnetworks.TicTacToeStateLookupTable2;

public class TicTacToeLookupTablePlayer2 extends StateMachineGamer {

	private Map<TicTacToeBoard, TicTacToeBoard> stateLookupTable;

	@Override
	public String getName() {
		return "TicTacToeLookupTablePlayer2";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		Move selection = null;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		TicTacToeBoard stateValues = stateLookupTable.get(TicTacToeBoard.fromGdlState(getCurrentState().getContents()));
		if (stateValues != null) {
			System.out.println("FOUND matching state");
			int max = Integer.MIN_VALUE;
			for (Move move : moves) {
				List<GdlTerm> terms = move.getContents().toSentence().getBody();
				int moveRow = Integer.parseInt(terms.get(0).toString()) - 1;
				int moveCol = Integer.parseInt(terms.get(1).toString()) - 1;

				Integer value = stateValues.getValue(moveRow, moveCol);
				System.out.println("Move " + move + " has value " + value);
				if (value > max) {
					max = value;
					selection = move;
				}
			}
			System.out.println("Playing move with value " + max);
		} else {
			System.out.println("State not found, playing random");
			selection = moves.get(0);
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	@Override
	public StateMachine getInitialStateMachine() {
		if (stateLookupTable == null) {
			try {
				stateLookupTable = TicTacToeStateLookupTable2.getStateLookupTable();
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
