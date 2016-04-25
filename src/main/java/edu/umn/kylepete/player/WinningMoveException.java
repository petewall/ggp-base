package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.Move;

@SuppressWarnings("serial")
public class WinningMoveException extends Exception {
    public Move move;

    public WinningMoveException(Move move) {
        this.move = move;
    }
}
