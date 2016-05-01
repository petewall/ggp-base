package edu.umn.kylepete.player;

import java.io.IOException;
import java.util.ArrayList;

import org.ggp.base.player.GamePlayer;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class MultithreadedUCTPlayer extends UCTPlayer {
    @Override
    public String getName() {
        return "MultithreadedUCTPlayer";
    }

    private ArrayList<WorkerThread> threadPool;
    private class WorkerThread extends Thread {
        private StateMachine stateMachine;
        public int iterations;
        public StateNode rootCopy;

        public WorkerThread(int id) {
            super("Worker Thread " + id);
            iterations = 0;
            stateMachine = new CachedStateMachine(new ProverStateMachine());
            stateMachine.initialize(getMatch().getGame().getRules());
        }

        @Override
        public void run() {
            try {
//                System.out.println(getName() + ": Making copies!");
                rootCopy = root.makeCopy();
//                System.out.println(getName() + ": Done.");
                while (System.currentTimeMillis() < finishBy) {
                    StateNode current = treePolicy(stateMachine, rootCopy);
                    double value = defaultPolicy(stateMachine, current);
                    backup(current, value);
                    iterations++;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    protected int runTheWork() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        int totalIterations = 0;
        trackThreads = true;
        for (WorkerThread thread : threadPool) {
            thread.start();
        }
        trackThreads = false;
        for (int i = 0; i < threadPool.size(); ++i) {
            try {
                WorkerThread thread = threadPool.get(i);
                thread.join();
                totalIterations += thread.iterations;
//                System.out.println(thread.getName() + ": Merging to root");
                this.root.merge(thread.rootCopy);
//                System.out.println(thread.getName() + ": Done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            threadPool.set(i, new WorkerThread(i));
        }
        return totalIterations;
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        super.stateMachineMetaGame(timeout);
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Starting " + cores + " threads");
        threadPool = new ArrayList<WorkerThread>(cores);
        for (int i = 0; i < cores; ++i) {
            threadPool.add(new WorkerThread(i));
        }
    }

    @Override
    public void stateMachineStop() {
        for (Thread thread : threadPool) {
            thread.interrupt();
        }
        this.root = null;
        System.out.println("Total iterations: " + gameIterations);
    }

    @Override
    public void stateMachineAbort() {
        for (Thread thread : threadPool) {
            thread.interrupt();
        }
        this.root = null;
    }

    public static void main(String[] args)
    {
        if (args.length != 1) {
            System.err.println("Usage: GamePlayer <port>");
            System.exit(1);
        }

        try {
            GamePlayer player = new GamePlayer(Integer.valueOf(args[0]), new MultithreadedUCTPlayer());
            player.run();
        } catch (NumberFormatException e) {
            System.err.println("Illegal port number: " + args[0]);
            e.printStackTrace();
            System.exit(2);
        } catch (IOException e) {
            System.err.println("IO Exception: " + e);
            e.printStackTrace();
            System.exit(3);
        }
    }
}
