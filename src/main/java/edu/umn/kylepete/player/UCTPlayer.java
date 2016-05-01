package edu.umn.kylepete.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class UCTPlayer extends SubAgent {
    @Override
    public String getName() {
        return "UCTPlayer";
    }

    public boolean trackThreads = false;

    protected class StateNode {
        public StateNode(StateNode parent, MachineState state, double currentReward, int visits) {
            this(parent.depth, state, currentReward, visits);
            this.parent = parent;
        }

        public StateNode(int depth, MachineState state, double currentReward, int visits) {
            this.state = state;
            this.children = new HashMap<List<Move>, StateNode>();
            this.totalReward = currentReward;
            this.visits = visits;
            this.depth = depth;
        }

        public StateNode(int depth, MachineState state) {
            this(depth, state, 0, 0);
        }

        public StateNode(StateNode parent, MachineState state) {
            this(parent, state, 0, 0);
        }

        /**
         * Perform a deep copy of this StateNode's tree using a new parent object
         * @param newParent
         * @return
         */
        public StateNode makeCopy() {
            StateNode copy = new StateNode(this.depth, this.state, this.totalReward, this.visits);
            for (List<Move> moveSet : this.children.keySet()) {
                StateNode childCopy = this.children.get(moveSet).makeCopy();
                childCopy.parent = copy;
                copy.children.put(moveSet, childCopy);
            }
            return copy;
        }

        /**
         * Merge the values of two trees together
         * @param copy
         */
        public void merge(StateNode copy) {
            this.totalReward += copy.totalReward;
            this.visits += copy.visits;
            for (List<Move> moveSet : copy.children.keySet()) {
                StateNode child = copy.children.get(moveSet);
                if (this.children.containsKey(moveSet)) {
                    this.children.get(moveSet).merge(child);
                } else {
                    //System.out.println("Moveset wasn't found: " + moveSet);
                    this.children.put(moveSet, child);
                }
            }
        }

        private void trackThread() {
            if (trackThreads) {
                if (threadName != null)
                    threadName = Thread.currentThread().getName();
                if (!Thread.currentThread().getName().equals(threadName)) {
                    System.out.println("Thread confusion!");
                }
            }
        }

        private String threadName = null;
        public int depth;
        public StateNode parent;
        public MachineState state;
        public Map<List<Move>, StateNode> children;
        public double totalReward;
        public int visits;
        private List<List<Move>> moves;

        public StateNode addChild(List<Move> moveSet, MachineState state) {
            trackThread();
            if (!this.children.containsKey(moveSet)) {
                this.children.put(moveSet, new StateNode(this, state));
            }
            return this.children.get(moveSet);
        }

        public StateNode getOrAddChild(StateMachine stateMachine, List<Move> moveSet) throws TransitionDefinitionException {
            trackThread();
            if (!this.children.containsKey(moveSet)) {
                MachineState state = stateMachine.getNextState(this.state, moveSet);
                addChild(moveSet, state);
            }
            return this.children.get(moveSet);
        }

        public List<List<Move>> getMoves(StateMachine stateMachine) throws MoveDefinitionException {
            trackThread();
            if (moves == null) {
                moves = stateMachine.getLegalJointMoves(this.state);
            }
            return moves;
        }

        public List<List<Move>> getMoves(StateMachine stateMachine, Move move) throws MoveDefinitionException {
            trackThread();
            List<List<Move>> allMoves = getMoves(stateMachine);
            List<List<Move>> moveSets = new ArrayList<List<Move>>();
            for (List<Move> moveset : allMoves) {
                if (moveset.get(roleIndex).equals(move)) {
                    moveSets.add(moveset);
                }
            }
            return moveSets;
        }

        public List<Move> getMyMoves(StateMachine stateMachine) throws MoveDefinitionException {
            trackThread();
            Set<Move> myMoves = new HashSet<Move>();
            List<List<Move>> allMoves = getMoves(stateMachine);
            for (List<Move> moveset : allMoves) {
                myMoves.add(moveset.get(roleIndex));
            }
            return new ArrayList<Move>(myMoves);
        }

        @Override
        public String toString() {
            return "Depth(" + depth + "), Reward(" + totalReward + "), Visits(" + visits + "), ratio(" + Math.floor((totalReward / visits) * 100) / 100 + "), Children(" + children.size() + ")";
        }
    }

    protected AtomicLong depthCount = new AtomicLong();
    protected long gameIterations = 0;
    protected StateNode root = null;
    protected int roleIndex;
    protected long finishBy;
    private static boolean checkForDecisiveMoves = true;

    protected int runTheWork() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        depthCount.set(0);
        int iterations = 0;
        while (System.currentTimeMillis() < finishBy) {
            StateNode current = treePolicy(this.root);
            double value = defaultPolicy(current);
            backup(current, value);
            iterations++;
        }
        return iterations;
    }

    private double getDistributionConfidence(int total, int visits, int branchingFactor) {
        return 1 - Math.max(0, 1.0 / branchingFactor - (double)visits / total);
    }

    private double getComplexityConfidence(int total, int branchingFactor, long avgDepth) {
        return total / Math.pow(avgDepth, branchingFactor);
    }

    @Override
    public ScoredMoveSet scoreValidMoves(long timeout) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        ScoredMoveSet scoredMoves = new ScoredMoveSet();
        ScoredMoveSet confidenceFactors = new ScoredMoveSet();
        finishBy = timeout - 1000;

        if (getLastMove() != null) {
            List<Move> theLastMove = getLastMove();
            this.root = this.root.children.get(theLastMove);
        }

        int totalIterations = runTheWork();
        System.out.println("ran " + totalIterations + " iterations.");
        gameIterations += totalIterations;

        for (List<Move> moveset : this.root.children.keySet()) {
            Move move = moveset.get(roleIndex);
            double ucb1 = getUCB1(this.root, moveset, 0);
            System.out.println("    " + moveset + ": " + root.children.get(moveset));
            scoredMoves.put(move, ucb1);

            double distributionConfidence = getDistributionConfidence(totalIterations, root.children.get(moveset).visits, root.children.size());
            confidenceFactors.put(move, distributionConfidence);
        }
        scoredMoves.normalize();

        for (Move move : scoredMoves.keySet()) {
            double before = scoredMoves.get(move);
            double confidence = confidenceFactors.get(move);
            scoredMoves.applyConfidence(move, confidence);
            double after = scoredMoves.get(move);
            System.out.println("    Applying confidence to move " + move + ": " + before + " * " + confidence + " == " + after);
        }
        scoredMoves.normalize();
        return scoredMoves;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        long start = System.currentTimeMillis();

        ScoredMoveSet moveSet = scoreValidMoves(timeout);
        Move chosenMove = moveSet.getBestMove();
        System.out.println("Picking from the best of: ");
        checkDepths(this.root, true);

        long stop = System.currentTimeMillis();
        List<Move> validMoves = getStateMachine().getLegalMoves(this.root.state, getRole());
        notifyObservers(new GamerSelectedMoveEvent(validMoves, chosenMove, stop - start));
        System.out.println("ran for " + (stop - start) / 1000.0 + " seconds");
        return chosenMove;
    }

    protected void checkDepths(StateNode source, boolean printNodes) {
        Integer depth = null;
        for (List<Move> moveset : source.children.keySet()) {
            StateNode node = source.children.get(moveset);
            if (depth == null) {
                depth = new Integer(node.depth);
            } else if (depth.intValue() != node.depth) {
                System.out.println("The depths don't agree!");
            }
            if (printNodes) {
                System.out.println("    " + node + " --> " + moveset);
            }
        }
    }

    protected StateNode treePolicy(StateNode source) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();
        return treePolicy(stateMachine, source);
    }

    protected StateNode treePolicy(StateMachine stateMachine, StateNode source) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateNode current = source;

        while (!stateMachine.isTerminal(current.state)) {
            if (checkForDecisiveMoves) {
                Move decisiveMove = findDecisiveMoves(stateMachine, current);
                if (decisiveMove != null) {
                    List<Move> moveSet = stateMachine.getRandomJointMove(current.state, getRole(), decisiveMove);
                    return current.getOrAddChild(stateMachine, moveSet);
                }
            }

            List<List<Move>> allValidMoves = current.getMoves(stateMachine);
            if (current.children.size() < allValidMoves.size()) {
                StateNode newNode = expandNodes(stateMachine, current, allValidMoves);
                if (newNode != null) {
                    return newNode;
                }
//                System.out.println("The new node was null!");
            }
            List<Move> moveset = bestMoveSet(current, 1.44);
            current = current.children.get(moveset);
            if (current == null) {
                System.out.println("The best child was null!");
            }
        }
        return current;
    }

    protected Move findDecisiveMoves(StateMachine stateMachine, StateNode source) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
        if (stateMachine.isTerminal(source.state)) {
            return null;
        }

        List<Move> validMoves = source.getMyMoves(stateMachine);
        for (Move move : validMoves) {
            List<List<Move>> moveSets = source.getMoves(stateMachine, move);
            //List<List<Move>> moveSets = stateMachine.getLegalJointMoves(source.state, getRole(), move);
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
//                System.out.println("Found a decisive winning move!");
                return move;
            }
            if (losing) {
//                System.out.println("Found a decisive losing move!");
                return move;
            }
        }
        return null;
    }

    protected StateNode expandNodes(StateMachine stateMachine, StateNode source, List<List<Move>> allValidMoves) throws MoveDefinitionException, TransitionDefinitionException {
        for (List<Move> moveset : allValidMoves) {
            if (source.children.containsKey(moveset)) {
                continue;
            }
            MachineState resultingState = stateMachine.getNextState(source.state, moveset);
            StateNode node = source.addChild(moveset, resultingState);
//            StateNode node = new StateNode(source, resultingState);
//            source.children.put(moveset, node);
            return node;
        }

//        System.out.println("Someone expanded them all!");
        return null;
    }

    protected double getUCB1(StateNode node, List<Move> moveset, double explorationConstant) {
        double UCB1 = 0;
        // if visits is 0, that means another thread has expanded the child nodes, but hasn't yet visited them
        // FIXME: What's the appropriate response here?  For now, I'm skipping...
        StateNode child = node.children.get(moveset);
        if (child.visits > 0) {
            UCB1 = (child.totalReward / child.visits);
            if (explorationConstant != 0) {
                UCB1 += explorationConstant * Math.sqrt((2.0 * Math.log(node.visits)) / child.visits);
            }
//            values.add(new Double(UCB1));
        }
        return UCB1;
    }

    protected List<Move> bestMoveSet(StateNode current, double explorationConstant) {
        // FIXME: Should this be picking the opposite for the opponent's turn?
        //        If that encourages deeper search on moves that the opponent is likely to make, it might be better.

        List<Move> bestMoveSet = null;
        double best = Double.NEGATIVE_INFINITY;
//        List<Double> values = new ArrayList<Double>();

        for (List<Move> moveset : current.children.keySet()) {
            double UCB1 = getUCB1(current, moveset, explorationConstant);
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
        return defaultPolicy(getStateMachine(), current);
    }

    protected double defaultPolicy(StateMachine stateMachine, StateNode current) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
//        System.out.println(Thread.currentThread().getName() + "DefaultPolicy: " + current);
        MachineState startState = current.state;
        int[] depth = new int[1];
        MachineState terminalState = stateMachine.performDepthCharge(startState, depth);
        depthCount.addAndGet(depth[0]);
        return stateMachine.getGoal(terminalState, getRole()) / 100.0;
    }

    protected void backup(StateNode node, double value) {
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
        this.root = new StateNode(0, initialState);
        this.gameIterations = 0;
        this.roleIndex = getStateMachine().getRoles().indexOf(getRole());
    }

    @Override
    public void stateMachineStop() {
        this.root = null;
        System.out.println("Total iterations: " + gameIterations);
    }

    @Override
    public void stateMachineAbort() {
        this.root = null;
    }

    @Override
    public DetailPanel getDetailPanel() {
        return new SimpleDetailPanel();
    }
}
