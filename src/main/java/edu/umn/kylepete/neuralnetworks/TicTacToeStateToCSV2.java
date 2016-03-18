package edu.umn.kylepete.neuralnetworks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.nd4j.linalg.api.ndarray.INDArray;

import external.JSON.JSONException;

public class TicTacToeStateToCSV2 {


	public static void main(String[] args) throws JSONException, IOException, SymbolFormatException, GdlFormatException {
		Map<TicTacToeBoard, TicTacToeBoard> stateTable = TicTacToeStateLookupTable2.getStateLookupTable();

		FileWriter writer = new FileWriter(TicTacToeNeuralNetwork2.TTT_DATA_SET);
		String newLine = System.getProperty("line.separator");

		System.out.println("Writing TicTacToe CSV data to " + TicTacToeNeuralNetwork2.TTT_DATA_SET);
		for(Entry<TicTacToeBoard, TicTacToeBoard> entry : stateTable.entrySet()){
			INDArray stateArray = entry.getKey().toINDArray();
			INDArray values = entry.getValue().toINDArray();

			for(int i = 0; i < stateArray.length(); i++){
				writer.write(Integer.toString(stateArray.getInt(i)));
				writer.write(',');
			}
			for(int i = 0; i < values.length(); i++){
				writer.write(Integer.toString(values.getInt(i)));
				if(i < values.length() - 1){
					writer.write(',');
				}
			}

			writer.write(newLine);
		}

		writer.close();
		System.out.println("Done writing TicTacToe CSV data to " + TicTacToeNeuralNetwork2.TTT_DATA_SET);
	}

}
