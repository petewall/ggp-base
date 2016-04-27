package edu.umn.kylepete.player;

import java.util.HashMap;

import org.ggp.base.util.statemachine.Move;

@SuppressWarnings("serial")
public class ScoredMoveSet extends HashMap<Move, Double> {
    public void incrementValue(Move move, Double summand) {
        incrementValue(move, summand.doubleValue());
    }

    public void incrementValue(Move move, double summand) {
        double current = this.get(move).doubleValue();
        this.put(move, new Double (current + summand));
    }

    public void multiplyValue(Move move, double factor) {
        double current = this.get(move).doubleValue();
        this.put(move, new Double (current * factor));
    }

    public void divideValue(Move move, double denominator) {
        double current = this.get(move).doubleValue();
        this.put(move, new Double (current / denominator));
    }

    public void normalize() {
        double total = 0;
        for (Double score : this.values()) {
            total += score.doubleValue();
        }
        for (Move move : this.keySet()) {
            divideValue(move, total);
        }
    }

    public void combine(ScoredMoveSet otherSet) {
        for (Move move : otherSet.keySet()) {
            if (this.containsKey(move)) {
                this.incrementValue(move, otherSet.get(move));
            } else {
                this.put(move, otherSet.get(move));
            }
        }
    }

    public Move getBestMove() {
        Move bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Move move : this.keySet()) {
            if (this.get(move).compareTo(bestScore) > 0) {
                bestMove = move;
            }
        }
        return bestMove;
    }
}
