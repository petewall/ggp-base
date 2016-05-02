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

    public void multiplyAllValues(double factor) {
        for (Move move : this.keySet()) {
            multiplyValue(move, factor);
        }
    }

    /**
     * This must be run *after* normalization, and you must run normalization again
     * 0 = no confidence, score will push to .5
	 * 1 = full confidence, score will not be changed
     * @param confidence
     */
    public void applyConfidence(Move move, double confidenceFactor) {
        Double current = get(move);
        Double result = (current.doubleValue() - 0.5) * confidenceFactor + 0.5;
        put(move, result);
    }

    /**
     * This must be run *after* normalization, and you must run normalization again
     * @param confidence
     */
    public void applyConfidenceToAllMoves(double confidenceFactor) {
    	for (Move move : this.keySet()) {
    		applyConfidence(move, confidenceFactor);
        }
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
            if (total > 0) {
                divideValue(move, total);
            }
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
                bestScore = this.get(move).doubleValue();
            }
        }
        return bestMove;
    }

    @Override
    public Double put(Move move, Double value) {
        if (value.isNaN()) {
            throw new IllegalArgumentException();
        }
        if (value.isInfinite()) {
            throw new IllegalArgumentException();
        }
        return super.put(move, value);
    }
}
