package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import edu.umn.kylepete.neuralnetworks.GameNeuralNetwork;

public class MiniMaxNeuralNetworkMovePicker extends MiniMaxMovePicker {

	// 0 = do not apply confidence, scores will not be changed
	// 0.5 = use a medium amount of confidence adjustment
	// 1 = use max confidence adjustment, not recommended because 0 training games will result in all .5 scores
	private static final double CONFIDENCE_INFLUENCE = 0.5;

	private GameNeuralNetwork neuralNetwork;

	public MiniMaxNeuralNetworkMovePicker(GameNeuralNetwork neuralNetwork, int maxDepth) {
		super(maxDepth);
		this.neuralNetwork = neuralNetwork;
	}

	@Override
	protected double evaluateState(Role role, MachineState state) {
		return neuralNetwork.evaluateState(role, state);
	}

	@Override
	public ScoredMoveSet getScoredMoves(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException, WinningMoveException {
		ScoredMoveSet scoredMoves = super.getScoredMoves(state, role, stateMachine);
		if (CONFIDENCE_INFLUENCE > 0) {
			// apply confidence factor based on the number of trained games
			int trainCount = neuralNetwork.getTrainCount();
			// use an exponential curve from 0 limiting at 1 around 1000 games
			double confidenceFactor = 1 - Math.exp(-0.01 * trainCount);
			// adjust the confidence based on the influence
			confidenceFactor = 1 - ((1 - confidenceFactor) * CONFIDENCE_INFLUENCE);
			scoredMoves.applyConfidenceToAllMoves(confidenceFactor);
			scoredMoves.normalize();
		}
		return scoredMoves;
	}

}
