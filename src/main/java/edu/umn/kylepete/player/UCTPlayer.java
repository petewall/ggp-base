package edu.umn.kylepete.player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            this.children = new HashSet<StateNode>();
            this.totalReward = 0;
            this.visits = 0;
        }

        public StateNode parent;
        public MachineState state;
        public Set<StateNode> children;
        public double totalReward;
        public double visits;
        public Move entryMove;

        @Override
        public String toString() {
            int depth = 0;
            StateNode temp = this.parent;
            while (temp != null) {
                depth++;
                temp = temp.parent;
            }
            return "Depth(" + depth + "), Reward(" + totalReward + "), Visits(" + visits + ") Children(" + children.size() + ")";
        }
    }
    private StateNode root = null;

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();
        StateMachine theMachine = getStateMachine();

        long finishBy = timeout - 1000;
        int iterations = 0;
        while (System.currentTimeMillis() < finishBy) {
            StateNode current = treePolicy(root);
            double value = defaultPolicy(current);
            backup(current, value);
            iterations++;
        }
        System.out.println("ran " + iterations + " iterations");
        long stop = System.currentTimeMillis();
        System.out.println("ran for " + (stop - start) / 1000.0 + " seconds");

        StateNode bestChild = bestChild(root, 0);
        root = bestChild;
        Move selection = root.entryMove;
        List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
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
            MachineState resultingState = stateMachine.getRandomNextState(source.state, getRole(), move);
            if (source.children.contains(resultingState)) {
                continue;
            }

            StateNode node = new StateNode(source, resultingState);
            source.children.add(node);
            node.entryMove = move;
            return node;
        }

        return null;
    }

    private StateNode bestChild(StateNode current, double explorationConstant) {
        StateNode bestChild = null;
        double best = Double.NEGATIVE_INFINITY;

        for (StateNode child : current.children) {
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

        System.out.println("Starting deep dive on " + current.state);
        MachineState nextState = current.state;
        while (!stateMachine.isTerminal(nextState)) {
            nextState = stateMachine.getRandomNextState(nextState);
            System.out.println("new state: " + nextState);
        }

        return stateMachine.getGoal(nextState, getRole()) / 100.0;
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
