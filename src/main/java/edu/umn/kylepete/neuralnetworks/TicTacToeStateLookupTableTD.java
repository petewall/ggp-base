package edu.umn.kylepete.neuralnetworks;

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

import org.ggp.base.apps.research.ArchiveDownloader;
import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.symbol.factory.SymbolFactory;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.ggp.base.util.symbol.grammar.SymbolList;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

public final class TicTacToeStateLookupTableTD {

	private static AggregateData data = null;

	public static void main(String[] args) throws IOException, JSONException, SymbolFormatException, GdlFormatException {

		AggregateData data = getData();
		Map<Set<GdlSentence>, Double> stateValues = data.states;
		for (Set<GdlSentence> state : stateValues.keySet()) {
			System.out.println(state + " --- " + stateValues.get(state));
		}
		System.out.println("Number of games: " + data.gameCount);
		System.out.println("Number of states: " + data.totalStateCount);
		System.out.println("Number of unique states: " + stateValues.size());
	}

	public static Map<Set<GdlSentence>, Double> getStateLookupTable() throws JSONException, IOException, SymbolFormatException, GdlFormatException {
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
		public Map<Set<GdlSentence>, Double> states = new HashMap<Set<GdlSentence>, Double>();
		public Map<Set<GdlSentence>, Integer> stateCounts = new HashMap<Set<GdlSentence>, Integer>();
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

					JSONArray theStates = matchJSON.getJSONArray("states");
					Set<GdlSentence> prevState = null;
					for (int i = 0; i < theStates.length(); i++) {
						double reward = 0.5; // neutral reward
						if (i == theStates.length() - 1) {
							// this is the last state, so get player 2's end of game reward
							reward = goalValues.getInt(1) / 100.0;
						}
						Set<GdlSentence> theState = new HashSet<GdlSentence>();
						SymbolList stateElements = (SymbolList) SymbolFactory.create(theStates.getString(i));
						for (int j = 0; j < stateElements.size(); j++) {
							theState.add((GdlSentence) GdlFactory.create("( true " + stateElements.get(j).toString() + " )"));
						}
						data.totalStateCount++;

						Double stateValue = data.states.get(theState);
						if (stateValue == null) {
							stateValue = reward;
							data.states.put(theState, reward);
							data.stateCounts.put(theState, 0);
						}

						if (prevState != null) {
							Double prevStateValue = data.states.get(prevState);
							Integer prevStateCount = data.stateCounts.get(prevState);
							prevStateCount += 1;

							prevStateValue = prevStateValue + 1/(prevStateCount + 1.0) * (stateValue - prevStateValue);

							data.states.put(prevState, prevStateValue);
							data.stateCounts.put(prevState, prevStateCount);
						}
						prevState = theState;
					}
				}
			}
		} catch (JSONException je) {
			je.printStackTrace();
		}
	}
}
