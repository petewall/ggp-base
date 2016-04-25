package edu.umn.kylepete.player;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class SubAgent extends StateMachineGamer {
//    public abstract void setRoleIndex(int index);
    public abstract List<ScoredMove> scoreValidMoves(long timeout) throws WinningMoveException, MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException;
//    public abstract void prepareForGame(long timeout, StateMachine stateMachine);
    public abstract void setStateMachine(StateMachine newStateMachine);

    public void setLastMove(List<Move> newLastMove) {
        lastMove = newLastMove;
    }
}
