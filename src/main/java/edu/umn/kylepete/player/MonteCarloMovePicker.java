package edu.umn.kylepete.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloMovePicker implements MovePicker {

    public class WinLossRate {
        int wins;
        int losses;

        public WinLossRate() {
            this.wins = 0;
            this.losses = 0;
        }

        public void win() {
            wins++;
        }

        public void loss() {
            losses++;
        }

        public void recordResult(boolean result) {
            if (result) {
                win();
            } else {
                loss();
            }
        }

        public double getWinLossRatio() {
            return (double)wins / losses;
        }
    }

    public Map<MachineState, WinLossRate> monteCarloMemory;

    private void recordResult(MachineState state, boolean result) {
        if (!monteCarloMemory.containsKey(state)) {
            monteCarloMemory.put(state, new WinLossRate());
        }
        monteCarloMemory.get(state).recordResult(result);
    }

    public MonteCarloMovePicker() {
        monteCarloMemory = new HashMap<MachineState, WinLossRate>();
    }

    public boolean runMonteCarloGame(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        boolean result;
        if (stateMachine.isTerminal(state)) {
            result = stateMachine.getGoal(state, role) == 100;
        } else {
            MachineState nextState = stateMachine.getRandomNextState(state);
            result = runMonteCarloGame(nextState, role, stateMachine);
        }
        recordResult(state, result);
        return result;
    }

    /**
     * Pick the best move based on Monte Carlo Tree Search
     *
     */
    @Override
    public Move pickBestMove(MachineState state, Role role, StateMachine stateMachine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        List<Move> moves = stateMachine.getLegalMoves(state, role);
        System.out.println("Picking a move from list: " + moves);

        Move bestMove = moves.get(0);
        if (moves.size() == 1) {
            System.out.println("Only one valid move.  Returing that: " + bestMove);
            return bestMove;
        }

        double bestRatio = 0;
        for (Move move : moves) {
            MachineState potentialState = stateMachine.getRandomNextState(state, role, move);
            runMonteCarloGame(potentialState, role, stateMachine);
            double ratio = monteCarloMemory.get(potentialState).getWinLossRatio();
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestMove = move;
            }
        }

        System.out.println("Picking a move with a estimated win ratio of: " + bestRatio + ". Move: " + bestMove);
        return bestMove;
    }
}
