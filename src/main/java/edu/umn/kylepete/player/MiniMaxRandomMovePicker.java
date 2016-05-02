package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

public class MiniMaxRandomMovePicker extends MiniMaxMovePicker {

	public MiniMaxRandomMovePicker(int maxDepth) {
		super(maxDepth);
	}

	@Override
	protected double evaluateState(Role role, MachineState state) {
		// always return the same neutral evaluation, which will randomly get selected
		return 50;
	}

}
