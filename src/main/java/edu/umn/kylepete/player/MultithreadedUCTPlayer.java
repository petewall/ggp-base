package edu.umn.kylepete.player;

import java.util.ArrayList;

import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MultithreadedUCTPlayer extends UCTPlayer {
    @Override
    public String getName() {
        return "MultithreadedUCTPlayer";
    }

    private ArrayList<WorkerThread> threadPool;
    private class WorkerThread extends Thread {
        public int iterations;

        public WorkerThread(int id) {
            super("Worker Thread " + id);
            iterations = 0;
        }

        @Override
        public void run() {
            try {
                while (System.currentTimeMillis() < finishBy) {
                    StateNode current = treePolicy(root);
                    double value = defaultPolicy(current);
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
        for (WorkerThread thread : threadPool) {
            thread.start();
        }
        for (int i = 0; i < threadPool.size(); ++i) {
            try {
                WorkerThread thread = threadPool.get(i);
                thread.join();
                totalIterations += thread.iterations;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
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
//        cores = 2;
        System.out.println("Starting " + cores + " threads");
        threadPool = new ArrayList<WorkerThread>(cores);
        for (int i = 0; i < cores; ++i) {
            threadPool.add(new WorkerThread(i));
        }
    }
}
