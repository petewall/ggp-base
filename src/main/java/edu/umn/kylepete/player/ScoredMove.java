package edu.umn.kylepete.player;

import java.util.List;

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

    public static void normalize(List<ScoredMove> moveList) {
        double total = 0;
        for (ScoredMove move : moveList) {
            total += move.score;
        }
        for (ScoredMove move : moveList) {
            move.score = move.score / total;
        }
    }

    public static void applyConfidenceFactor(List<ScoredMove> moveList, double confidenceFactor) {
        for (ScoredMove move : moveList) {
            move.score *= confidenceFactor;
        }
    }

    public static int indexOf(Move move, List<ScoredMove> moveList) {
        for (int i = 0; i < moveList.size(); ++i) {
            if (moveList.get(i).move.equals(move)) {
                return i;
            }
        }
        return -1;
    }

    public static List<ScoredMove> combine(List<ScoredMove> moveList, List<ScoredMove> otherMoveList) {
        if (moveList == null) {
            return otherMoveList;
        } else {
            for (ScoredMove move : moveList) {
                int i = indexOf(move.move, otherMoveList);
                move.score += otherMoveList.get(i).score;
            }
        }
        return moveList;
    }
}
