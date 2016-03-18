package edu.umn.kylepete.neuralnetworks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.apps.research.ArchiveDownloader;
import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.symbol.factory.SymbolFactory;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.ggp.base.util.symbol.grammar.SymbolList;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

public final class TicTacToeStateLookupTable2 {

	private static AggregateData data = null;

	public static void main(String[] args) throws IOException, JSONException, SymbolFormatException, GdlFormatException {

		AggregateData data = getData();
		Map<TicTacToeBoard, TicTacToeBoard> stateValues = data.states;
		for (TicTacToeBoard state : stateValues.keySet()) {
			System.out.println(state + "     |\n     |\n     V\n" + stateValues.get(state) + "\n\n\n");
		}
		System.out.println("Number of games: " + data.gameCount);
		System.out.println("Number of states: " + data.totalStateCount);
		System.out.println("Number of unique states: " + stateValues.size());
	}

	public static Map<TicTacToeBoard, TicTacToeBoard> getStateLookupTable() throws JSONException, IOException, SymbolFormatException, GdlFormatException {
		if (data == null) {
			data = getData();
		}
		return data.states;
	}

	private static AggregateData getData() throws JSONException, IOException, SymbolFormatException, GdlFormatException {
		AggregateData data = new AggregateData();

		String line;
		int nCount = 0;
		File archiveFile = ArchiveDownloader.getArchiveFile();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archiveFile), Charset.forName("UTF-8")));
		while ((line = br.readLine()) != null) {

			JSONObject entryJSON = new JSONObject(line);
			String url = entryJSON.getString("url");
			JSONObject matchJSON = entryJSON.getJSONObject("data");
			processMatch(url, matchJSON, data);
			// if(matchJSON.getString("gameMetaURL").startsWith("http://games.ggp.org/base/games/ticTacToe/v0")){
			// Match match = new Match(matchJSON.toString(), null, null);
			// processMatch2(match, data);
			// }
			nCount++;
			if (nCount % 1000 == 0) {
				System.out.println("Processed " + nCount + " matches.");
			}
		}
		br.close();

		System.out.println("Number of games: " + data.gameCount);
		System.out.println("Number of states: " + data.totalStateCount);
		System.out.println("Number of unique states: " + data.states.size());
		return data;
	}

	static class AggregateData {
		public Map<TicTacToeBoard, TicTacToeBoard> states = new HashMap<TicTacToeBoard, TicTacToeBoard>();
		public int gameCount = 0;
		public int totalStateCount = 0;
	}

	private static void processMatch(String theURL, JSONObject matchJSON, AggregateData data) throws SymbolFormatException, GdlFormatException {
		try {
			// And for completed signed matches...
			if (matchJSON.has("isCompleted") && matchJSON.getBoolean("isCompleted") && matchJSON.has("matchHostPK") && matchJSON.has("goalValues")) {
				String gameURL = matchJSON.getString("gameMetaURL");
				JSONArray goalValues = matchJSON.getJSONArray("goalValues");
				// Add a data point to the average length of 9xTTT matches, if it's a 9xTTT match
				// if (gameURL.startsWith("http://games.ggp.org/base/games/nineBoardTicTacToe/") && goalValues.length() == 2) {
				if (gameURL.startsWith("http://games.ggp.org/base/games/ticTacToe/v0") && goalValues.length() == 2) {

					data.gameCount++;
					int playerXScore = goalValues.getInt(0);
					int playerOScore = goalValues.getInt(1);
					boolean playerXwins = false;
					boolean playerOwins = false;
					if (playerXScore < playerOScore) {
						playerOwins = true;
					} else if (playerXScore > playerOScore) {
						playerXwins = true;
					}

					if(!playerOwins && !playerXwins){
						return;
					}

					JSONArray theStates = matchJSON.getJSONArray("states");
					JSONArray theMoves = matchJSON.getJSONArray("moves");
					for (int i = 0; i < theMoves.length(); i++) {
						GdlTerm playerOmove = GdlFactory.createTerm(theMoves.getJSONArray(i).getString(1));
						if (!playerOmove.toString().contains("noop")) {
							Set<GdlSentence> theState = new HashSet<GdlSentence>();
							SymbolList stateElements = (SymbolList) SymbolFactory.create(theStates.getString(i));
							for (int j = 0; j < stateElements.size(); j++) {
								theState.add((GdlSentence) GdlFactory.create("( true " + stateElements.get(j).toString() + " )"));
							}

							data.totalStateCount++;
							List<GdlTerm> terms = playerOmove.toSentence().getBody();
							int moveRow = Integer.parseInt(terms.get(0).toString()) - 1;
							int moveCol = Integer.parseInt(terms.get(1).toString()) - 1;
							TicTacToeBoard board = TicTacToeBoard.fromGdlState(theState);
							TicTacToeBoard stateValues = data.states.get(board);
							if (stateValues == null) {
								stateValues = new TicTacToeBoard(3, 3);
							}
							if(playerXwins){
								stateValues.playX(moveRow, moveCol);
							}else if(playerOwins){
								stateValues.playO(moveRow, moveCol);
							}
							data.states.put(board, stateValues);
						}
					}
				}
			}
		} catch (JSONException je) {
			je.printStackTrace();
		}
	}
}
