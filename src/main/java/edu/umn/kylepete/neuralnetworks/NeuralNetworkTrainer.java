package edu.umn.kylepete.neuralnetworks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ggp.base.apps.research.ArchiveDownloader;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

import external.JSON.JSONException;
import external.JSON.JSONObject;

public final class NeuralNetworkTrainer {

	static class AggregateData {
		public GameNeuralNetworkDatabase gameNeuralNetworkDatabase;
		public Map<String, Integer> gameCounts = new HashMap<String, Integer>();
		public int skippedGames = 0;
	}

	public static void main(String[] args) throws IOException, JSONException, SymbolFormatException, GdlFormatException, InterruptedException {

		AggregateData data = new AggregateData();
		data.gameNeuralNetworkDatabase = GameNeuralNetworkDatabase.readFromDefaultFile();

		String line;
		int nCount = 0;
		File archiveFile = ArchiveDownloader.getArchiveFile();
		System.out.println("Reading archives from file " + archiveFile.getAbsolutePath());
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archiveFile), Charset.forName("UTF-8")));
		while ((line = br.readLine()) != null) {

			JSONObject entryJSON = new JSONObject(line);
			String url = entryJSON.getString("url");
			JSONObject matchJSON = entryJSON.getJSONObject("data");
			processMatch(url, matchJSON, data);
			nCount++;
			if (nCount % 1000 == 0) {
				System.out.println("Processed " + nCount + " matches.");
			}
		}
		br.close();

		System.out.println("\n\nDone training.");
		data.gameNeuralNetworkDatabase.writeToDefaultFile();
		System.out.println("Wrote game database to " + GameNeuralNetworkDatabase.DEFAULT_FILE);
		System.out.println("\nSkipped " + data.skippedGames + " games due to incomplete data or errors.");

		List<Entry<String,Integer>> gameEntries = new ArrayList<Entry<String,Integer>>(data.gameCounts.entrySet());
		Collections.sort(gameEntries, new Comparator<Entry<String,Integer>>() {
			@Override
			public int compare(Entry<String,Integer> o1, Entry<String,Integer> o2) {
				return -o1.getValue().compareTo(o2.getValue());
			}
		});
		int totalCount = 0;
		StringBuilder sb = new StringBuilder();
		for(Entry<String,Integer> gameEntry : gameEntries){
			int gameCount = gameEntry.getValue();
			totalCount += gameCount;
			sb.append(String.format("%-80s%d\n", gameEntry.getKey(), gameCount));
		}
		System.out.println("\n");
		System.out.println("Total games trained : " + totalCount);
		System.out.println("Unique games trained: " + gameEntries.size());
		System.out.print(sb.toString());
	}

	private static void addGameCount(String gameURL, AggregateData data) {
		Integer count = data.gameCounts.get(gameURL);
		if (count == null) {
			count = 0;
		}
		data.gameCounts.put(gameURL, count + 1);
	}

	private static void processMatch(String theURL, JSONObject matchJSON, AggregateData data) throws SymbolFormatException, GdlFormatException, JSONException {

		if(!matchJSON.has("isCompleted") || !matchJSON.getBoolean("isCompleted")){
			data.skippedGames++;
			System.err.println("Skipped match because it is not completed");
		} else if(!matchJSON.has("matchHostPK")){
			data.skippedGames++;
			System.err.println("Skipped match because it does not have a host key");
		}else if(!matchJSON.has("goalValues")){
			data.skippedGames++;
			System.err.println("Skipped match because it does not have goal values");
		}else if(!matchJSON.has("gameMetaURL")){
			data.skippedGames++;
			System.err.println("Skipped match because it does not have a game URL");
		}else{

			Match match = new Match(matchJSON.toString(), null, null);
			try {
				String gameURL = match.getGame().getRepositoryURL();
				data.gameNeuralNetworkDatabase.train(match);
				//String gameURL = matchJSON.getString("gameMetaURL");
				addGameCount(gameURL, data);
			} catch (Exception e) {
				data.skippedGames++;
				System.err.println("Skipping game due to error.");
				e.printStackTrace();
			}
		}
	}
}
