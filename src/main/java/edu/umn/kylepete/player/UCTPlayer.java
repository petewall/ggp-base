package edu.umn.kylepete.player;

import java.util.HashMap;
import java.util.List;

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
        }

        public StateNode parent;
        public MachineState state;
        public HashMap<Move, StateNode> children;
        public double totalReward;
        public double visits;
    }
    private StateNode root = null;

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();
        StateMachine theMachine = getStateMachine();

        List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
        Move selection = moves.get(0);
        if (moves.size() > 1) {
            long finishBy = start + timeout - 1000;
            while (System.currentTimeMillis() < finishBy) {
                StateNode current = treePolicy(root);
                double value = defaultPolicy(current);
                backup(current, value);

            }
        }
        long stop = System.currentTimeMillis();

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
                current = bestChild(current, 1.44);
            }
        }
        return current;
    }

    private StateNode expandNodes(StateNode source) throws MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        List<Move> validMoves = stateMachine.getLegalMoves(source.state, getRole());
        for (Move move : validMoves) {
            if (source.children.containsKey(move)) {
                continue;
            }

            MachineState resultingState = stateMachine.getRandomNextState(source.state, getRole(), move);
            StateNode node = new StateNode(source, resultingState);
            source.children.put(move, node);
            return node;
        }

        return null;
    }

    private StateNode bestChild(StateNode current, double explorationConstant) {
        StateNode bestChild = null;
        double best = Double.NEGATIVE_INFINITY;

        for (StateNode child : current.children.values()) {
            double UCB1 = (child.totalReward / child.visits);
            if (explorationConstant != 0) {
                UCB1 += explorationConstant * Math.sqrt((2.0 * Math.log(current.visits)) / child.visits);
            }
            if (UCB1 > best) {
                bestChild = child;
                best = UCB1;
            }
        }

        return bestChild;
    }

    private double defaultPolicy(StateNode current) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
//        function DEFAULTPOLICY(s)
//        while s is non-terminal do
//        choose a ∈ A(s) uniformly at random
//        s ← f(s, a)
//        return reward for state s

        StateNode next = current;
        do {
            MachineState nextState = stateMachine.getRandomNextState(next.state);
            next = new StateNode(next, nextState);
        } while (!stateMachine.isTerminal(next.state));

        return stateMachine.getGoal(next.state, getRole()) / 100.0;
    }

    private void backup(StateNode node, double value) {
        StateNode parent;
        do {
            node.visits++;
            node.totalReward += value;
            parent = node.parent;
        } while (parent != null);
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
