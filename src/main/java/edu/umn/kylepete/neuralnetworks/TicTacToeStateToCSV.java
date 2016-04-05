package edu.umn.kylepete.neuralnetworks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import external.JSON.JSONException;

public class TicTacToeStateToCSV {


	public static void main(String[] args) throws JSONException, IOException, SymbolFormatException, GdlFormatException {
		Map<Set<GdlSentence>, Double> stateTable = TicTacToeStateLookupTableTD.getStateLookupTable();

		FileWriter writer = new FileWriter(TicTacToeNeuralNetwork.TTT_DATA_SET);
		String newLine = System.getProperty("line.separator");

		System.out.println("Writing TicTacToe CSV data to " + TicTacToeNeuralNetwork.TTT_DATA_SET);
		for(Entry<Set<GdlSentence>, Double> entry : stateTable.entrySet()){
			INDArray stateArray = getStateAsArray(entry.getKey());
			Double value = entry.getValue();

			for(int i = 0; i < stateArray.length(); i++){
				writer.write(Integer.toString(stateArray.getInt(i)));
				writer.write(',');
			}

			writer.write(Double.toString(value));
			writer.write(newLine);

//			System.out.println();
//			System.out.println(board[0][0] + " | " + board[0][1] + " | " + board[0][2]);
//			System.out.println("---------");
//			System.out.println(board[1][0] + " | " + board[1][1] + " | " + board[1][2]);
//			System.out.println("---------");
//			System.out.println(board[2][0] + " | " + board[2][1] + " | " + board[2][2]);
//			System.out.println("value: " + value);


		}

		writer.close();
		System.out.println("Done writing TicTacToe CSV data to " + TicTacToeNeuralNetwork.TTT_DATA_SET);

	}

	public static INDArray getStateAsArray(Set<GdlSentence> state){

		int[][] board = new int[3][3];
		for(GdlSentence cell : state){
			if(cell.toString().contains("control")){
				continue;
			}
			List<GdlTerm> terms = cell.getBody().get(0).toSentence().getBody();
			int row = Integer.parseInt(terms.get(0).toString());
			int col = Integer.parseInt(terms.get(1).toString());
			char piece = terms.get(2).toString().charAt(0);
			int pieceNum;
			if(piece == 'x'){
				pieceNum = -1;
			}else if(piece == 'o'){
				pieceNum = 1;
			}else{
				pieceNum = 0;
			}
			board[row-1][col-1] = pieceNum;
		}

		float[] row = new float[9];
		for(int i = 0; i < 3; i++){
			for(int y = 0; y < 3; y++){
				row[i*3+y] = board[i][y];
			}
		}
		return Nd4j.create(row);
	}

}
