package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

import edu.umn.kylepete.neuralnetworks.GameNeuralNetwork;

public class MiniMaxNeuralNetworkMovePicker extends MiniMaxMovePicker {

    private GameNeuralNetwork neuralNetwork;

    public MiniMaxNeuralNetworkMovePicker(GameNeuralNetwork neuralNetwork, int maxDepth){
    	super(maxDepth);
    	this.neuralNetwork = neuralNetwork;
    }

	@Override
	protected double evaluateState(Role role, MachineState state) {
		return neuralNetwork.evaluateState(role, state);
	}

}
