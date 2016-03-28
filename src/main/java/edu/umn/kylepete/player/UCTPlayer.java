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
            this.children = new HashMap<List<Move>, StateNode>();
            this.totalReward = 0;
            this.visits = 0;
            this.depth = (parent != null ? parent.depth + 1 : 0);
        }

        public int depth;
        public StateNode parent;
        public MachineState state;
        public Map<List<Move>, StateNode> children;
        public double totalReward;
        public double visits;

        @Override
        public String toString() {
            return "Depth(" + depth + "), Reward(" + totalReward + "), Visits(" + visits + ") Children(" + children.size() + ")";
        }
    }
    private StateNode root = null;
    private int roleIndex;

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();
        long finishBy = timeout - 1000;

        if (getLastMove() != null) {
            List<Move> theLastMove = getLastMove();
            root = root.children.get(theLastMove);
        }

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
        List<List<Move>> moves = new ArrayList<List<Move>>(root.children.keySet());
        System.out.println("Current root state: " + root.state);
        System.out.println("Valid official moves: " + official + " (" + official.size() + ")");
        System.out.println("Valid moves: " + moves + " (" + moves.size() + ")");

        List<Move> selection = bestMoveSet(root, 0);
        System.out.println("Picking from the best of: ");
        for (List<Move> moveset : root.children.keySet()) {
            System.out.println("    " + root.children.get(moveset) + " --> " + moveset);
        }
        notifyObservers(new GamerSelectedMoveEvent(official, selection.get(roleIndex), stop - start));
        return selection.get(roleIndex);
    }

    private StateNode treePolicy(StateNode current) throws MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        while (!stateMachine.isTerminal(current.state)) {
            int validMoveCount = stateMachine.getLegalJointMoves(current.state).size();
            if (current.children.size() < validMoveCount) {
                return expandNodes(current);
            } else {
                current = current.children.get(bestMoveSet(current, 1.44));
            }
        }
        return current;
    }

    private StateNode expandNodes(StateNode source) throws MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        List<List<Move>> allValidMoves = stateMachine.getLegalJointMoves(source.state);
        for (List<Move> moveset : allValidMoves) {
            MachineState resultingState = stateMachine.getNextState(source.state, moveset);
            if (source.children.containsKey(moveset)) {
                continue;
            }
            StateNode node = new StateNode(source, resultingState);
            source.children.put(moveset, node);
            return node;
        }

        return null;
    }

    private List<Move> bestMoveSet(StateNode current, double explorationConstant) {
        List<Move> bestMoveSet = null;
        double best = Double.NEGATIVE_INFINITY;

        for (List<Move> moveset : current.children.keySet()) {
            StateNode child = current.children.get(moveset);
            double UCB1 = (child.totalReward / child.visits);
            if (explorationConstant != 0) {
                UCB1 += explorationConstant * Math.sqrt((2.0 * Math.log(current.visits)) / child.visits);
            }
            if (UCB1 > best) {
                bestMoveSet = moveset;
                best = UCB1;
            }
        }

        return bestMoveSet;
    }

    private double defaultPolicy(StateNode current) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        int[] depth = new int[1];
        MachineState terminalState = getStateMachine().performDepthCharge(current.state, depth);
        return getStateMachine().getGoal(terminalState, getRole()) / 100.0;
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
        StateMachine stateMachine = getStateMachine();
        MachineState initialState = stateMachine.getInitialState();
        root = new StateNode(null, initialState);
        roleIndex = stateMachine.getRoles().indexOf(getRole());
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
