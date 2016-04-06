package edu.umn.kylepete.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.google.common.util.concurrent.AtomicDouble;

public class UCTPlayer extends StateMachineGamer {
    @Override
    public String getName() {
        return "UCTPlayer";
    }

    protected class StateNode {
        public StateNode(StateNode parent, MachineState state) {
            this.parent = parent;
            this.state = state;
            this.children = new HashMap<List<Move>, StateNode>();
            this.totalReward = new AtomicDouble(0);
            this.visits = new AtomicInteger(0);
            this.depth = (parent != null ? parent.depth + 1 : 0);
        }

        public int depth;
        public StateNode parent;
        public MachineState state;
        public Map<List<Move>, StateNode> children;
        public AtomicDouble totalReward;
        public AtomicInteger visits;
        private List<List<Move>> moves;

        public synchronized StateNode addChild(List<Move> moveSet, MachineState state) {
            if (!this.children.containsKey(moveSet)) {
                this.children.put(moveSet, new StateNode(this, state));
            }
            return this.children.get(moveSet);
        }

        public synchronized StateNode getOrAddChild(List<Move> moveSet) throws TransitionDefinitionException {
            if (!this.children.containsKey(moveSet)) {
                MachineState state = getStateMachine().getNextState(this.state, moveSet);
                this.children.put(moveSet, new StateNode(this, state));
            }
            return this.children.get(moveSet);
        }

        public synchronized List<List<Move>> getMoves() throws MoveDefinitionException {
            if (moves == null) {
                moves = getStateMachine().getLegalJointMoves(this.state);
            }
            return moves;
        }

        @Override
        public String toString() {
            return "Depth(" + depth + "), Reward(" + totalReward + "), Visits(" + visits + ") Children(" + children.size() + ")";
        }
    }
    protected StateNode root = null;
    protected int roleIndex;
    protected long finishBy;
    private static boolean checkForDecisiveMoves = false;

    protected int runTheWork() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        int iterations = 0;
        while (System.currentTimeMillis() < finishBy) {
            StateNode current = treePolicy(root);
            double value = defaultPolicy(current);
            backup(current, value);
            iterations++;
        }
        return iterations;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        int totalIterations;
        long start = System.currentTimeMillis();
        finishBy = timeout - 1000;

        if (getLastMove() != null) {
            List<Move> theLastMove = getLastMove();
            root = root.children.get(theLastMove);
        }

        List<Move> validMoves = getStateMachine().getLegalMoves(root.state, getRole());
        Move chosenMove = null;

        totalIterations = runTheWork();
        System.out.println("Picking from the best of: ");
        for (List<Move> moveset : root.children.keySet()) {
            System.out.println("    " + root.children.get(moveset) + " --> " + moveset);
        }
        List<Move> selection = bestMoveSet(root, 0);
        chosenMove = selection.get(roleIndex);

        System.out.println("ran " + totalIterations + " iterations.");
        long stop = System.currentTimeMillis();
        System.out.println("ran for " + (stop - start) / 1000.0 + " seconds");

        notifyObservers(new GamerSelectedMoveEvent(validMoves, chosenMove, stop - start));
        return chosenMove;
    }

    protected StateNode treePolicy(StateNode source) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateNode current = source;
        StateMachine stateMachine = getStateMachine();

        while (!stateMachine.isTerminal(current.state)) {
            if (checkForDecisiveMoves) {
                Move decisiveMove = findDecisiveMoves(current);
                if (decisiveMove != null) {
                    List<Move> moveSet = stateMachine.getRandomJointMove(current.state, getRole(), decisiveMove);
                    return current.getOrAddChild(moveSet);
                }
            }

            List<List<Move>> allValidMoves = current.getMoves();
            if (current.children.size() < allValidMoves.size()) {
                StateNode newNode = expandNodes(current, allValidMoves);
                if (newNode != null) {
                    return newNode;
                }
                System.out.println("The new node was null!");
            }
            List<Move> moveset = bestMoveSet(current, 1.44);
            current = current.children.get(moveset);
            if (current == null) {
                System.out.println("The best child was null!");
            }
        }
        return current;
    }

    protected Move findDecisiveMoves(StateNode source) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
        StateMachine stateMachine = getStateMachine();
        if (stateMachine.isTerminal(source.state)) {
            return null;
        }

        List<Move> validMoves = stateMachine.getLegalMoves(source.state, getRole());
        for (Move move : validMoves) {
            List<List<Move>> moveSets = stateMachine.getLegalJointMoves(source.state, getRole(), move);
            boolean winning = true;
            boolean losing = true;
            for (List<Move> moveSet : moveSets) {
                MachineState resultingState = stateMachine.getNextState(source.state, moveSet);
//                source.addChild(moveSet, resultingState);
                if (!stateMachine.isTerminal(resultingState)) {
                    winning = false;
                    losing = false;
                    break;
                }
                if (stateMachine.getGoal(resultingState, getRole()) != 100) {
                    winning = false;
                    break;
                }
                if (stateMachine.getGoal(resultingState, getRole()) == 100) {
                    losing = false;
                    break;
                }
            }
            if (winning) {
                System.out.println("Found a decisive winning move!");
                return move;
            }
            if (losing) {
                System.out.println("Found a decisive losing move!");
                return move;
            }
        }
        return null;
    }

    protected synchronized StateNode expandNodes(StateNode source, List<List<Move>> allValidMoves) throws MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        for (List<Move> moveset : allValidMoves) {
            if (source.children.containsKey(moveset)) {
                continue;
            }
            MachineState resultingState = stateMachine.getNextState(source.state, moveset);
            StateNode node = new StateNode(source, resultingState);
            source.children.put(moveset, node);
            return node;
        }

        System.out.println("Someone expanded them all!");
        return null;
    }

    protected synchronized List<Move> bestMoveSet(StateNode current, double explorationConstant) {
        // FIXME: Should this be picking the opposite for the opponent's turn?
        //        If that encourages deeper search on moves that the opponent is likely to make, it might be better.

        List<Move> bestMoveSet = null;
        double best = Double.NEGATIVE_INFINITY;
        List<Double> values = new ArrayList<Double>();

        for (List<Move> moveset : current.children.keySet()) {
            StateNode child = current.children.get(moveset);
            double UCB1 = 0;
            // if visits is 0, that means another thread has expanded the child nodes, but hasn't yet visited them
            // FIXME: What's the appropriate response here?  For now, I'm skipping...
            if (child.visits.intValue() > 0) {
                UCB1 = (child.totalReward.doubleValue() / child.visits.intValue());
                if (explorationConstant != 0) {
                    UCB1 += explorationConstant * Math.sqrt((2.0 * Math.log(current.visits.doubleValue())) / child.visits.intValue());
                }
                values.add(new Double(UCB1));
            }
            if (Double.isNaN(UCB1)) {
                System.out.println("Whuh oh!");
            }
            if (UCB1 > best) {
                bestMoveSet = moveset;
                best = UCB1;
            }
        }
        return bestMoveSet;
    }

    protected double defaultPolicy(StateNode current) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
//        System.out.println(Thread.currentThread().getName() + "DefaultPolicy: " + current);
        MachineState startState = current.state;
        MachineState terminalState = getStateMachine().performDepthCharge(startState, null);
        return getStateMachine().getGoal(terminalState, getRole()) / 100.0;
    }

    protected void backup(StateNode node, double value) {
        StateNode current = node;
        do {
            current.visits.incrementAndGet();
            current.totalReward.addAndGet(value);
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
