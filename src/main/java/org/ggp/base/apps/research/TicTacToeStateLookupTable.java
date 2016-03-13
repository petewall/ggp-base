package org.ggp.base.apps.research;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.symbol.factory.SymbolFactory;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.ggp.base.util.symbol.grammar.SymbolList;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

public final class TicTacToeStateLookupTable {

	private static AggregateData data = null;

	public static void main(String[] args) throws IOException, JSONException, SymbolFormatException, GdlFormatException {

		AggregateData data = getData();
		Map<Set<GdlSentence>, Integer> stateValues = data.states;
		for (Set<GdlSentence> state : stateValues.keySet()) {
			System.out.println(state + " --- " + stateValues.get(state));
		}
		System.out.println("Number of games: " + data.gameCount);
		System.out.println("Number of states: " + data.totalStateCount);
		System.out.println("Number of unique states: " + stateValues.size());
	}

	public static Map<Set<GdlSentence>, Integer> getStateLookupTable() throws JSONException, IOException, SymbolFormatException, GdlFormatException {
		if(data == null){
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
		public Map<Set<GdlSentence>, Integer> states = new HashMap<Set<GdlSentence>, Integer>();
		public int gameCount = 0;
		public int totalStateCount = 0;
	}

	private static void processMatch2(Match match, AggregateData data) {
		if (match.isCompleted() && match.getGoalValues().size() == 2) {
			if (match.getGameRepositoryURL().startsWith("http://games.ggp.org/base/games/ticTacToe/v0")) {
				data.gameCount++;
				int player1Score = match.getGoalValues().get(0);
				int player2Score = match.getGoalValues().get(1);
				int value;
				if (player1Score == player2Score) {
					value = 0;
				} else if (player1Score < player2Score) {
					value = 1;
				} else {
					value = -1;
				}
				for (Set<GdlSentence> state : match.getStateHistory()) {
					data.totalStateCount++;
					Integer stateValue = data.states.get(state);
					if (stateValue == null) {
						stateValue = 0;
					}
					stateValue += value;
					data.states.put(state, stateValue);
				}
			}
		}
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
					int player1Score = goalValues.getInt(0);
					int player2Score = goalValues.getInt(1);
					int value;
					if (player1Score == player2Score) {
						value = 0;
					} else if (player1Score < player2Score) {
						value = 1;
					} else {
						value = -1;
					}

					JSONArray theStates = matchJSON.getJSONArray("states");
					for (int i = 0; i < theStates.length(); i++) {
						Set<GdlSentence> theState = new HashSet<GdlSentence>();
						SymbolList stateElements = (SymbolList) SymbolFactory.create(theStates.getString(i));
						for (int j = 0; j < stateElements.size(); j++) {
							theState.add((GdlSentence) GdlFactory.create("( true " + stateElements.get(j).toString() + " )"));
						}
						data.totalStateCount++;
						Integer stateValue = data.states.get(theState);
						if (stateValue == null) {
							stateValue = 0;
						}
						stateValue += value;
						data.states.put(theState, stateValue);
					}
				}
			}
		} catch (JSONException je) {
			je.printStackTrace();
		}
	}
}
