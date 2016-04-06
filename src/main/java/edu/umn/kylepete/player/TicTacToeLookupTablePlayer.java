package edu.umn.kylepete.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import edu.umn.kylepete.neuralnetworks.TicTacToeStateLookupTableTD;

public class TicTacToeLookupTablePlayer extends StateMachineGamer {

	private Map<Set<GdlSentence>, Double> stateLookupTable;

    @Override
    public String getName() {
        return "TicTacToeLookupTablePlayer";
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {

        long start = System.currentTimeMillis();
        Move selection = null;
        Double max = Double.NEGATIVE_INFINITY;
        //List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
        Map<Move, List<MachineState>> moves = getStateMachine().getNextStates(getCurrentState(), getRole());
        for(Move move : moves.keySet()){
        	MachineState nextState = moves.get(move).get(0);
        	String stateString = renderStateAsSymbolList(nextState.getContents());
        	System.out.println(stateString);
        	System.out.println("Searching for state: " + nextState.getContents());
        	Double value = stateLookupTable.get(nextState.getContents());
        	if(value != null){
        		System.out.println("FOUND matching move with value " + value);
        	}else{
        		System.out.println("State not found");
        		value = 0.5;
        	}
        	if(value > max){
        		max = value;
        		selection = move;
        	}
        }
        System.out.println("Playing move with value " + max);

        long stop = System.currentTimeMillis();

        notifyObservers(new GamerSelectedMoveEvent(new ArrayList<Move>(moves.keySet()), selection, stop - start));
        return selection;
    }

    private static final String renderStateAsSymbolList(Set<GdlSentence> theState) {
        // Strip out the TRUE proposition, since those are implied for states.
        String s = "( ";
        for (GdlSentence sent : theState) {
            String sentString = sent.toString();
            s += sentString.substring(6, sentString.length()-2).trim() + " ";
        }
        return s + ")";
    }

    @Override
    public StateMachine getInitialStateMachine() {
		if (stateLookupTable == null) {
			try {
				stateLookupTable = TicTacToeStateLookupTableTD.getStateLookupTable();
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
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
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
