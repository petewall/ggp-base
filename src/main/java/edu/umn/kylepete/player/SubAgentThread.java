package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class SubAgentThread extends Thread {
    private SubAgent agent;
    private long timeout;
    private ScoredMoveSet moveSet;
    private Move winningMove;

    public SubAgentThread(SubAgent agent, long timeout) {
        this.agent = agent;
        this.timeout = timeout;
        this.moveSet = null;
        this.winningMove = null;
    }

    @Override
    public void run() {
        try {
            moveSet = agent.scoreValidMoves(timeout);
        } catch (WinningMoveException e) {
            System.out.println("Found a winning move: " + e.move);
            winningMove = e.move;
        } catch (MoveDefinitionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransitionDefinitionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GoalDefinitionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public ScoredMoveSet getMoveSet() {
        return this.moveSet;
    }

    public boolean foundWinningMove() {
        return this.winningMove != null;
    }

    public Move getWinningMove() {
        return this.winningMove;
    }
}
