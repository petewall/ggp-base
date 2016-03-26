package edu.umn.kylepete.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class UCTPlayer extends StateMachineGamer {
    @Override
    public String getName() {
        return "UCTPlayer";
    }

    private class StateNode {
        public StateNode(StateNode parent, MachineState state) {
            this.parent = parent;
            this.state = state;
            this.children = new HashMap<Move, StateNode>();
            this.totalReward = 0;
            this.visits = 0;
            this.depth = (parent != null ? parent.depth + 1 : 0);
        }

        public int depth;
        public StateNode parent;
        public MachineState state;
        public Map<Move, StateNode> children;
        public double totalReward;
        public double visits;

        @Override
        public String toString() {
            return "Depth(" + depth + "), Reward(" + totalReward + "), Visits(" + visits + ") Children(" + children.size() + ")";
        }
    }
    private StateNode root = null;

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();
        long finishBy = timeout - 5000;
        int iterations = 0;
        while (System.currentTimeMillis() < finishBy) {
            StateNode current = treePolicy(root);
            double value = defaultPolicy(current);
            backup(current, value);
            iterations++;
        }
        System.out.println("ran " + iterations + " iterations.");
        long stop = System.currentTimeMillis();
        System.out.println("ran for " + (stop - start) / 1000.0 + " seconds");

        List<Move> official = getStateMachine().getLegalMoves(root.state, getRole());
        List<Move> moves = new ArrayList<Move>(root.children.keySet());
        System.out.println("Current root state: " + root.state);
        System.out.println("Valid official moves: (" + official.size() + ")" + official);
        System.out.println("Valid moves: (" + moves.size() + ")" + moves);

        Move selection = bestMove(root, 0);
        StateNode bestChild = root.children.get(selection);
        System.out.println("Picking from the best of: ");
        for (Move move : root.children.keySet()) {
            System.out.println("    " + root.children.get(move) + " --> " + move);
        }
        root = bestChild;
        System.out.println("New state: " + root.state);
        notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
        return selection;
    }

    private StateNode treePolicy(StateNode current) throws MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        while (!stateMachine.isTerminal(current.state)) {
            List<Move> validMoves = stateMachine.getLegalMoves(current.state, getRole());
            if (current.children.size() < validMoves.size()) {
                return expandNodes(current);
            } else {
                current = current.children.get(bestMove(current, 1.44));
            }
        }
        return current;
    }

    private StateNode expandNodes(StateNode source) throws MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        List<Move> validMoves = stateMachine.getLegalMoves(source.state, getRole());
//        if (source.depth == 1) {
//            System.out.println("the valid moves: " + validMoves);
//        }
        for (Move move : validMoves) {
            MachineState resultingState = stateMachine.getRandomNextState(source.state, getRole(), move);
//            if (source.depth == 1) {
//                System.out.println("Potential move: " + move);
//                System.out.println("    Resulting state: " + resultingState);
//            }
            if (source.children.containsKey(move)) {
//                System.out.println("Already contained!");
                continue;
            }
            StateNode node = new StateNode(source, resultingState);

//            if (node.depth == 2) {
//                System.out.println("Putting new child for move: " + move);
//                System.out.println("   " + node);
//            }
            source.children.put(move, node);
            return node;
        }

        return null;
    }

    private Move bestMove(StateNode current, double explorationConstant) {
        Move bestMove = null;
        double best = Double.NEGATIVE_INFINITY;

        for (Move move : current.children.keySet()) {
            StateNode child = current.children.get(move);
            double UCB1 = (child.totalReward / child.visits);
            if (explorationConstant != 0) {
                UCB1 += explorationConstant * Math.sqrt((2.0 * Math.log(current.visits)) / child.visits);
            }
            if (UCB1 > best) {
                bestMove = move;
                best = UCB1;
            }
        }

        return bestMove;
    }

    private double defaultPolicy(StateNode current) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
//        function DEFAULTPOLICY(s)
//        while s is non-terminal do
//        choose a ∈ A(s) uniformly at random
//        s ← f(s, a)
//        return reward for state s

        int[] depth = new int[1];
        MachineState terminalState = stateMachine.performDepthCharge(current.state, depth);
//        MachineState nextState = current.state;
//        while (!stateMachine.isTerminal(nextState)) {
//            nextState = stateMachine.getRandomNextState(nextState);
////            System.out.println("new state: " + nextState);
//        }

        return stateMachine.getGoal(terminalState, getRole()) / 100.0;
    }

    private void backup(StateNode node, double value) {
        StateNode current = node;
        do {
            current.visits++;
            current.totalReward += value;
            current = current.parent;
        } while (current != null);
    }

    @Override
    public StateMachine getInitialStateMachine() {
        return new CachedStateMachine(new ProverStateMachine());
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
        // Random gamer does no game previewing.
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        MachineState initialState = getStateMachine().getInitialState();
        root = new StateNode(null, initialState);
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
