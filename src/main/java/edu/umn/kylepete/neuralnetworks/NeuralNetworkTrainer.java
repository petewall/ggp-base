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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.ggp.base.apps.research.ArchiveDownloader;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.RemoteGameRepository;
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

	private static Set<Match> matchArchive = Collections.newSetFromMap(new ConcurrentHashMap<Match, Boolean>(100000));
	private static ConcurrentMap<String, Game> cachedGames = new ConcurrentHashMap<String, Game>(100);
	private static AtomicInteger skippedMatches = new AtomicInteger(0);

	public static void main(String[] args) throws IOException, JSONException, SymbolFormatException, GdlFormatException, InterruptedException {

		long startTime = System.currentTimeMillis();

		AggregateData data = new AggregateData();
//		data.gameNeuralNetworkDatabase = GameNeuralNetworkDatabase.readFromDefaultFile();
		data.gameNeuralNetworkDatabase = new GameNeuralNetworkDatabase();

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
		String line;
		int nCount = 0;
		File archiveFile = ArchiveDownloader.getArchiveFile();
		System.out.println("Reading archives from file " + archiveFile.getAbsolutePath());
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archiveFile), Charset.forName("UTF-8")));
		while ((line = br.readLine()) != null) {

			MatchProcessor processor = new MatchProcessor(line);
			executor.execute(processor);

		}
		br.close();

		executor.shutdown();
		while (!executor.isTerminated()) {
			Thread.sleep(1000);
			System.out.println("Loaded " + executor.getCompletedTaskCount() + " matches from archive to memory");
		}

		for (Match match : matchArchive) {
			try {
				data.gameNeuralNetworkDatabase.train(match);
				addGameCount(match.getGameRepositoryURL(), data);
			} catch (Exception e) {
				data.skippedGames++;
				skippedMatches.incrementAndGet();
				System.err.println("Skipping game due to error: " + e.getMessage());
//				e.printStackTrace();
			}
			nCount++;
			if (nCount % 1000 == 0) {
				long curTime = System.currentTimeMillis();
				System.out.println("Trained " + nCount + " matches in " + ((curTime - startTime) / 1000.0) + " seconds");
			}
		}

		long endTime = System.currentTimeMillis();
		System.out.println("\n\nFinished training in " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Found " + matchArchive.size() + " matches");
		data.gameNeuralNetworkDatabase.writeToDefaultFile();
		System.out.println("Wrote game database to " + GameNeuralNetworkDatabase.DEFAULT_FILE);
		System.out.println("\nSkipped " + skippedMatches.get() + " games due to incomplete data or errors.");

		List<Entry<String, Integer>> gameEntries = new ArrayList<Entry<String, Integer>>(data.gameCounts.entrySet());
		Collections.sort(gameEntries, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return -o1.getValue().compareTo(o2.getValue());
			}
		});
		int totalCount = 0;
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Integer> gameEntry : gameEntries) {
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

	static class MatchProcessor implements Runnable {
		private String jsonString;

		public MatchProcessor(String jsonString) {
			this.jsonString = jsonString;
		}

		@Override
		public void run() {
			try {
				JSONObject entryJSON = new JSONObject(this.jsonString);
				JSONObject matchJSON = entryJSON.getJSONObject("data");
				processMatch(matchJSON);
			} catch (Exception e) {
				// throw new RuntimeException(e);
				// skip
				skippedMatches.incrementAndGet();
			}
		}

		private void processMatch(JSONObject matchJSON) throws SymbolFormatException, GdlFormatException, JSONException {

			if (!matchJSON.has("isCompleted") || !matchJSON.getBoolean("isCompleted")) {
				// data.skippedGames++;
				skippedMatches.incrementAndGet();
				// System.err.println("Skipped match because it is not completed");
			} else if (!matchJSON.has("matchHostPK")) {
				// data.skippedGames++;
				skippedMatches.incrementAndGet();
				// System.err.println("Skipped match because it does not have a host key");
			} else if (!matchJSON.has("goalValues")) {
				// data.skippedGames++;
				skippedMatches.incrementAndGet();
				// System.err.println("Skipped match because it does not have goal values");
			} else if (!matchJSON.has("gameMetaURL")) {
				// data.skippedGames++;
				skippedMatches.incrementAndGet();
				// System.err.println("Skipped match because it does not have a game URL");
			} else {
				String gameURL = matchJSON.getString("gameMetaURL");
				Game cachedGame = cachedGames.get(gameURL);
				if (cachedGame == null) {
					cachedGame = RemoteGameRepository.loadSingleGame(gameURL);
					cachedGames.putIfAbsent(gameURL, cachedGame);
				}

				Match match = new Match(matchJSON.toString(), cachedGame, null);
				matchArchive.add(match);
			}
		}
	}

}
