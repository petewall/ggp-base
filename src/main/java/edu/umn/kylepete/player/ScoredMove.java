package edu.umn.kylepete.player;

import org.ggp.base.util.statemachine.Move;

public class ScoredMove implements Comparable<ScoredMove> {
    public Move move;
    public double score;

    public ScoredMove(Move move, double score) {
        this.move = move;
        this.score = score;
    }

    @Override
    public int compareTo(ScoredMove other) {
        return Double.compare(other.score, score);
    }
}
