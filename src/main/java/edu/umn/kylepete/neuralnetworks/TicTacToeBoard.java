package edu.umn.kylepete.neuralnetworks;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class TicTacToeBoard {

	private int[][] board;

	public TicTacToeBoard(int rows, int cols) {
		board = new int[rows][cols];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				board[r][c] = 0;
			}
		}
	}

	public void playX(int row, int col) {
		board[row][col] += -1;
	}

	public void playO(int row, int col) {
		board[row][col] += 1;
	}

	public int getValue(int row, int col) {
		return board[row][col];
	}

	public INDArray toINDArray() {
		float[] row = new float[9];
		for (int i = 0; i < 3; i++) {
			for (int y = 0; y < 3; y++) {
				row[i * 3 + y] = getValue(i, y);
			}
		}
		return Nd4j.create(row);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(board);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TicTacToeBoard other = (TicTacToeBoard) obj;
		if (!Arrays.deepEquals(board, other.board))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String newLine = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		int rows = board.length;
		int cols = board[0].length;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int value = board[r][c];
				if (value == 1) {
					sb.append(" O ");
				} else if (value == -1) {
					sb.append(" X ");
				} else if (value == 0) {
					sb.append("   ");
				} else {
					sb.append(value);
				}
				if (c < cols - 1) {
					sb.append("|");
				}
			}
			sb.append(newLine);
			if (r < rows - 1) {
				int numDash = cols * 3 + cols - 1;
				for (int i = 0; i < numDash; i++) {
					sb.append("-");
				}
				sb.append(newLine);
			}
		}
		return sb.toString();
	}

	public static TicTacToeBoard fromGdlState(Set<GdlSentence> gdlState) {
		TicTacToeBoard board = new TicTacToeBoard(3, 3);
		for (GdlSentence cell : gdlState) {
			if (cell.toString().contains("control")) {
				continue;
			}
			List<GdlTerm> terms = cell.getBody().get(0).toSentence().getBody();
			int row = Integer.parseInt(terms.get(0).toString()) - 1;
			int col = Integer.parseInt(terms.get(1).toString()) - 1;
			char piece = terms.get(2).toString().charAt(0);

			if (piece == 'x') {
				board.playX(row, col);
			} else if (piece == 'o') {
				board.playO(row, col);
			}
		}
		return board;
	}
}
