package edu.umn.kylepete.neuralnetworks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

	// list of game URLs to train and skip. If GAMES_TO_TRAIN is empty, all games will be trained except the GAMES_TO_SKIP.
	private static final List<String> GAMES_TO_TRAIN = Arrays.asList(
//			"http://games.ggp.org/base/games/ticTacToe/v0/",
//			"http://games.ggp.org/base/games/connectFour/v0/",
//			"http://games.ggp.org/base/games/blocker/v0/",
//			"http://games.ggp.org/base/games/nineBoardTicTacToe/v0/",
//			"http://games.ggp.org/base/games/checkers/v1/",
//			"http://games.ggp.org/base/games/pentago/v1/",
//			"http://games.ggp.org/dresden/games/minichess/v0/",
//			"http://games.ggp.org/base/games/speedChess/v1/"
	);

	private static final List<String> GAMES_TO_SKIP = Arrays.asList(
//			"http://games.ggp.org/dresden/games/othello-fourway-teamswitch/v0/", // GC overhead limit exceeded OutOfMemmoryError building network
//			"http://games.ggp.org/base/games/cephalopodMicro/v0/", // taking too long to train
//			"http://games.ggp.org/dresden/games/3qbf-5cnf-20var-40cl.2.qdimacs.satlike/v0/", // taking too long to train
//			"http://games.ggp.org/dresden/games/hanoi7_bugfix/v0/", // taking too long to train
//			"http://games.ggp.org/base/games/snakeAssemblit/v0/", // taking too long to train (8 hours for 100 matches)
//			"http://games.ggp.org/dresden/games/quad_7x7/v0/", // taking too long to train
//			"http://games.ggp.org/dresden/games/uf20-020.cnf.SAT.satlike/v0/", // taking too long to train, 7,354,818 neurons
//			"http://games.ggp.org/dresden/games/cylinder-checkers/v0/", // GC overhead limit exceeded OutOfMemmoryError building network
//			"http://games.ggp.org/dresden/games/nothello/v0/", // GC overhead limit exceeded OutOfMemmoryError building network
//			"http://games.ggp.org/dresden/games/endgame/v0/", // taking too long to train, ran an hour without hitting 100 matches, 462,648 neurons
//			"http://games.ggp.org/dresden/games/checkers-cylinder-mustjump/v0/", // taking too long to build network (30 mins)
//			"http://games.ggp.org/dresden/games/othello-fourway/v0/", // GC overhead limit exceeded OutOfMemmoryError building network
//			"http://games.ggp.org/dresden/games/checkers-mustjump-torus/v0/", // OutOfMemoryError java heap space
//			"http://games.ggp.org/dresden/games/vacuumcleaner_obstacles_5/v0/", // GC overhead limit exceeded OutOfMemmoryError building network
//			"http://games.ggp.org/dresden/games/quad_5x5_8_2/v0/", // taking too long to train, ran 4 hours for 100 matches (624840 neurons)
//			"http://games.ggp.org/dresden/games/guard_intruder/v0/", // cound not load URL: NullPointerException
//			"http://games.ggp.org/dresden/games/vacuumcleaner_obstacles_6/v0/", // taking to long to build network (30 mins)
//			"http://games.ggp.org/dresden/games/small_dominion/v0/", // taking to long to build network
//			"http://games.ggp.org/dresden/games/satlike_20v_91c/v0/" // taking to long to train, 7354818 neurons
	);
	private static final int TEST_SIZE = 0; // 0 means no testing the training error
	private static final int MAX_TRAIN_MINS = 120; // maximum minutes to train a single game

	private static ConcurrentMap<String, Game> cachedGames = new ConcurrentHashMap<String, Game>(100);

	private static GameNeuralNetworkDatabase gameNeuralNetworkDatabase;
	private static Map<String, Integer> gameCounts = new ConcurrentHashMap<String, Integer>();
	private static AtomicInteger totalMatches = new AtomicInteger(0);
	private static AtomicInteger skippedMatches = new AtomicInteger(0);
	private static AtomicInteger errorGames = new AtomicInteger(0);
	private static File archiveFile;
	private static long startTime;

	public static void main(String[] args) throws IOException, JSONException, SymbolFormatException, GdlFormatException, InterruptedException {

		startTime = System.currentTimeMillis();

		archiveFile = ArchiveDownloader.getArchiveFile();
		// first pass over the archive file, populating the game counts with the games to train and not games to skip
		setGameCounts(archiveFile);
		log("Found " + gameCounts.size() + " unique games to train with a total of " + totalMatches.get() + " matches.");
		// sort games by least played
		List<Entry<String, Integer>> gameEntries = new ArrayList<Entry<String, Integer>>(gameCounts.entrySet());
		Collections.sort(gameEntries, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});

		gameNeuralNetworkDatabase = GameNeuralNetworkDatabase.readFromDefaultFile();
		// gameNeuralNetworkDatabase = new GameNeuralNetworkDatabase();

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);

		int i = 0;
		List<Future<?>> futures = new ArrayList<Future<?>>(gameCounts.size());
		for (Entry<String, Integer> entry : gameEntries) {
			String game = entry.getKey();
			i++;
			GameTrainer trainer = new GameTrainer(game, i);
			Future<?> future = executor.submit(trainer);
			futures.add(future);
		}

		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.DAYS);

		for (Future<?> future : futures) {
			try {
				future.get(10, TimeUnit.SECONDS);
			} catch (Exception e) {
				errorGames.incrementAndGet();
				future.cancel(true);
			}
		}

		long endTime = System.currentTimeMillis();
		System.out.println("\n");
		log("Finished all training in " + ((endTime - startTime) / 1000.0 / 60.0) + " minutes");
		System.out.println("Skipped " + errorGames.get() + " games due to training errors.");
		System.out.println("Skipped a total of " + skippedMatches.get() + " matches due to incomplete data or errors.");

		gameCounts.clear();
		for (GameNeuralNetwork gameNet : gameNeuralNetworkDatabase.getAllGameNetworks()) {
			gameCounts.put(gameNet.getGame().getRepositoryURL(), gameNet.getTrainCount());
		}
		gameEntries = new ArrayList<Entry<String, Integer>>(gameCounts.entrySet());
		Collections.sort(gameEntries, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return -o1.getValue().compareTo(o2.getValue());
			}
		});
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Integer> gameEntry : gameEntries) {
			int gameCount = gameEntry.getValue();
			sb.append(String.format("%-80s%d\n", gameEntry.getKey(), gameCount));
		}
		System.out.println("\n");
		System.out.println("Final database has " + gameCounts.size() + " games:");
		System.out.print(sb.toString());
	}

	private static void setGameCounts(File archiveFile) throws IOException, JSONException {
		log("Reading game archives from file " + archiveFile.getAbsolutePath());
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archiveFile), Charset.forName("UTF-8")));
		String line;
		while ((line = br.readLine()) != null) {
			try {
				JSONObject entryJSON = new JSONObject(line);
				JSONObject matchJSON = entryJSON.getJSONObject("data");
				if (isValidMatch(matchJSON)) {
					String gameURL = matchJSON.getString("gameMetaURL");
					if ((GAMES_TO_TRAIN.size() == 0 || GAMES_TO_TRAIN.contains(gameURL)) && !GAMES_TO_SKIP.contains(gameURL)) {
						addGameCount(gameURL);
						totalMatches.incrementAndGet();
					}
				} else {
					skippedMatches.incrementAndGet();
				}
			} catch (Exception e) {
				skippedMatches.incrementAndGet();
			}
		}
		br.close();

	}

	private static double getAverageError(List<Match> testMatches, GameNeuralNetwork gameNetwork) {
		if (testMatches.size() == 0) {
			return 0;
		}
		double totalError = 0;
		for (Match match : testMatches) {
			totalError += gameNetwork.evaluationError(match);
		}
		return totalError / (double) testMatches.size();
	}

	private static void addGameCount(String gameURL) {
		Integer count = gameCounts.get(gameURL);
		if (count == null) {
			count = 0;
		}
		gameCounts.put(gameURL, count + 1);
	}

	private static boolean isValidMatch(JSONObject matchJSON) throws JSONException {
		if (!matchJSON.has("isCompleted") || !matchJSON.getBoolean("isCompleted")) {
			return false;
			// System.err.println("Skipped match because it is not completed");
		} else if (!matchJSON.has("matchHostPK")) {
			return false;
			// System.err.println("Skipped match because it does not have a host key");
		} else if (!matchJSON.has("goalValues")) {
			return false;
			// System.err.println("Skipped match because it does not have goal values");
		} else if (!matchJSON.has("gameMetaURL")) {
			return false;
		}
		return true;
	}

	private static void log(String message, int i) {
		String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
		System.out.println(time + " (Game " + i + ") - " + message);
	}

	private static void log(String message) {
		String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
		System.out.println(time + " - " + message);
	}

	static class GameTrainer implements Runnable {
		private String game;
		private Game cachedGame;
		private GameNeuralNetwork gameNeuralNetwork;
		private int i;

		public GameTrainer(String gameUrl, int gameNumber) {
			this.game = gameUrl;
			this.i = gameNumber;
		}

		@Override
		public void run() {
			cachedGame = cachedGames.get(game);
			if (cachedGame == null) {
				cachedGame = RemoteGameRepository.loadSingleGame(game);
				cachedGames.putIfAbsent(game, cachedGame);
			}
			if (gameNeuralNetworkDatabase.containsGame(cachedGame)) {
				System.out.println();
				log("Skip training game " + i + " " + game + " because it was already trained.", i);
			} else {
				try{
					trainGame();
					long curTime = System.currentTimeMillis();
					log("Trained game " + i + " of " + gameCounts.size() + " in a total of " + ((curTime - startTime) / 1000.0 / 60.0) + " minutes.", i);
					gameNeuralNetworkDatabase.addExistingGame(gameNeuralNetwork);
					gameNeuralNetworkDatabase.writeToDefaultFile();
					log("Wrote game database to " + GameNeuralNetworkDatabase.DEFAULT_FILE, i);
				}catch (Exception e) {
					gameNeuralNetworkDatabase.forgetGame(cachedGame);
					errorGames.incrementAndGet();
					log("Failed to train game " + game + " due to a training exception: " + e.getClass().getName() + (e.getMessage() == null ? "" : " " + e.getMessage()), i);
					// e.printStackTrace();
				}
			}
		}

		private void trainGame() throws Exception {
			int gameCount = gameCounts.get(game);
			System.out.println("\n");
			log("Starting to train game " + game + " from " + gameCount + " matches.", i);
			log("Building neural network...", i);
			ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

			Future<?> future = executor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						gameNeuralNetwork = new GameNeuralNetwork(cachedGame);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			});
			executor.shutdown();
			try {
				future.get(10, TimeUnit.MINUTES);
			} catch (Exception e) {
				future.cancel(true);
				executor.shutdownNow();
				throw e;
			}

			log("Built " + gameNeuralNetwork.toString(), i);

			List<Match> matchList = new ArrayList<Match>(gameCount);
			log("Loading games from archive...", i);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archiveFile), Charset.forName("UTF-8")));
			String line;
			while ((line = br.readLine()) != null) {
				try {
					JSONObject entryJSON = new JSONObject(line);
					JSONObject matchJSON = entryJSON.getJSONObject("data");
					if (isValidMatch(matchJSON)) {
						String gameURL = matchJSON.getString("gameMetaURL");
						if (gameURL.equals(game)) {
							Match match = new Match(matchJSON.toString(), cachedGame, null);
							matchList.add(match);
						}
					} else {
						skippedMatches.incrementAndGet();
					}
				} catch (Exception e) {
					skippedMatches.incrementAndGet();
				}
			}
			br.close();

			long startTime = System.currentTimeMillis();

			log("Training...", i);
			Collections.shuffle(matchList);
			List<Match> testMatches = matchList.subList(0, TEST_SIZE);
			List<Match> trainingMatches = matchList.subList(TEST_SIZE, matchList.size());
			if (testMatches.size() > 0) {
				log("Initial Error: " + getAverageError(testMatches, gameNeuralNetwork), i);
			}

			int nCount = 0;
			for (Match match : trainingMatches) {
				try {
					gameNeuralNetwork.train(match);
				} catch (Exception e) {
					skippedMatches.incrementAndGet();
					log("Skipping game due to training error: " + e.getMessage(), i);
					// e.printStackTrace();
				}
				nCount++;
				long curTime = System.currentTimeMillis();
				double secsTrained = (curTime - startTime) / 1000.0;
				if (nCount % 100 == 0) {
					double error = getAverageError(testMatches, gameNeuralNetwork);
					String msg = "Trained " + nCount + " matches after " + secsTrained + " seconds.";
					if (testMatches.size() > 0) {
						msg = msg + " Error: " + error;
					}
					log(msg, i);
				}
				if((secsTrained / 60.0) > MAX_TRAIN_MINS){
					log("Quit training early because max time reached", i);
					break;
				}
			}
			double error = getAverageError(testMatches, gameNeuralNetwork);
			long curTime = System.currentTimeMillis();
			String msg = "Finished training game (" + nCount + " matches) in " + ((curTime - startTime) / 1000.0 / 60.0) + " minutes.";
			if (testMatches.size() > 0) {
				msg = msg + " Final Error: " + error;
			}
			log(msg, i);
		}
	}
}
