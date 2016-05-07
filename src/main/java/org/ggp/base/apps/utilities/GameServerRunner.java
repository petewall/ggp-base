package org.ggp.base.apps.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.ggp.base.server.GameServer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * GameServerRunner is a utility program that lets you start up a match
 * directly from the command line. It takes the following arguments:
 *
 * args[0] = tournament name, for storing results
 * args[1] = game key, for loading the game
 * args[2] = start clock, in seconds
 * args[3] = play clock, in seconds
 * args[4] = number of times to repeat the game
 * args[5,6,7] = host, port, name for player 1
 * args[8,9,10] = host, port, name for player 2
 * etc...
 *
 * @author Evan Cox
 * @author Sam Schreiber
 */
public final class GameServerRunner
{
    public static void main(String[] args) throws IOException, SymbolFormatException, GdlFormatException, InterruptedException, GoalDefinitionException, JSONException
    {
        // Extract the desired configuration from the command line.
        File pauseTouchFile = new File(System.getProperty("user.home") + File.separator + "pauseTournaments");
        System.out.println("Checking if pause file exists");
        if (pauseTouchFile.exists()) {
            System.out.println("it does!");
        }
        String tourneyName = args[0];
        String gameKey = args[1];
        Game game = GameRepository.getDefaultRepository().getGame(gameKey);
        int startClock = Integer.valueOf(args[2]);
        int playClock = Integer.valueOf(args[3]);
        int repetitions = Integer.valueOf(args[4]);
        if ((args.length - 5) % 3 != 0) {
            throw new RuntimeException("Invalid number of player arguments of the form host/port/name.");
        }
        List<String> hostNames = new ArrayList<String>();
        List<String> playerNames = new ArrayList<String>();
        List<Integer> portNumbers = new ArrayList<Integer>();
        for (int i = 5; i < args.length; i += 3) {
            String hostname = args[i];
            Integer port = Integer.valueOf(args[i + 1]);
            String name = args[i + 2];
            hostNames.add(hostname);
            portNumbers.add(port);
            playerNames.add(name);
        }
        int expectedRoles = Role.computeRoles(game.getRules()).size();
        if (hostNames.size() != expectedRoles) {
            throw new RuntimeException("Invalid number of players for game " + gameKey + ": " + hostNames.size() + " vs " + expectedRoles);
        }
        // use a LinkedHashMap so the iteration order is always the same
        LinkedHashMap<String, Integer> cumulativeScores = new LinkedHashMap<String, Integer>(2);
        for(String player : playerNames){
        	cumulativeScores.put(player, 0);
        }
        long startTime = System.currentTimeMillis();
        int errorCount = 0;
        for(int i = 1; i <= repetitions; i++){
	        Thread.sleep(3000); // wait a few seconds for the players to startup
	        if (pauseTouchFile.exists()) {
	            log("Paused", i);
	            --i;
	            startTime += 3000; // Skip the time spend paused.  This makes the elapsed time still correct.
	            continue;
	        }
        	log("Initializing game with players: " + Arrays.toString(playerNames.toArray()) , i);

            String matchName = tourneyName + ".game" + i;
	        Match match = new Match(matchName, -1, startClock, playClock, game, "");
	        match.setPlayerNamesFromHost(playerNames);

	        // Actually run the match, using the desired configuration.
	        GameServer server = new GameServer(match, hostNames, portNumbers);
	        log("Playing game...", i);
	        server.start();
	        server.join();
	        boolean errors = checkForErrors(match, i);
	        if(errors){
	        	errorCount++;
	        }
	        log("Completed game with scores " + Arrays.toString(match.getGoalValues().toArray()), i);
	        updateScores(cumulativeScores, match);
	        log("Cumulative scores: " + printScores(cumulativeScores), i);
	        saveMatch(match, server, tourneyName, cumulativeScores, i);

	        // rotate players
	        hostNames.add(hostNames.remove(0));
	        playerNames.add(playerNames.remove(0));
	        portNumbers.add(portNumbers.remove(0));

	        long curTime = System.currentTimeMillis();
	        double elapsedMins = (curTime - startTime) / 1000.0 / 60.0;
	        double remainingMins = elapsedMins / i * (repetitions - i);
	        log("Finished " + i + " of " + repetitions + " games in " + String.format("%.2f", elapsedMins) + " minutes. Estimated " + String.format("%.2f", remainingMins) + " minutes remaining.", null);
        }
        log("Successfully finished tournament. " + errorCount + " matches contained errors.", null);
        log("Average player scores: " + printAverageScores(cumulativeScores, repetitions), null);
    }

	private static boolean checkForErrors(Match match, int gameNum) {
    	Set<String> playersWithErrors = new HashSet<String>();
		for(List<String> errors : match.getErrorHistory()){
			for(int i = 0; i < errors.size(); i++){
				if(errors.get(i) != null && !errors.get(i).isEmpty()){
					playersWithErrors.add(match.getPlayerNamesFromHost().get(i));
				}
			}
		}
		if(playersWithErrors.size() > 0){
			log("WARNING: This match contains errors from the following players: " + Arrays.toString(playersWithErrors.toArray()), gameNum);
			return true;
		}
		return false;
	}

	private static void updateScores(LinkedHashMap<String, Integer> cumulativeScores, Match match) {
		for(int i = 0; i < match.getGoalValues().size(); i++){
			String player = match.getPlayerNamesFromHost().get(i);
			cumulativeScores.put(player, cumulativeScores.get(player) + match.getGoalValues().get(i));
		}

	}

	private static String printScores(LinkedHashMap<String, Integer> cumulativeScores) {
		StringBuilder sb = new StringBuilder();
		for(String player : cumulativeScores.keySet()){
			sb.append(player);
			sb.append("=");
			sb.append(cumulativeScores.get(player));
			sb.append(" ");
		}
		return sb.toString();
	}

    private static String printAverageScores(LinkedHashMap<String, Integer> cumulativeScores, int repetitions) {
    	StringBuilder sb = new StringBuilder();
		for(String player : cumulativeScores.keySet()){
			sb.append(player);
			sb.append("=");
			double avg = cumulativeScores.get(player) / (double)repetitions;
			sb.append(String.format("%.4f", avg));
			sb.append(" ");
		}
		return sb.toString();
	}

	private static void saveMatch(Match match, GameServer server, String tourneyName, LinkedHashMap<String, Integer> cumulativeScores, int gameNum) throws IOException, GoalDefinitionException, JSONException{
        // Open up the directory for this tournament.
        // Create a "scores" file if none exists.
        File tourneyDir = new File(tourneyName);
        if (!tourneyDir.exists()) {
        	tourneyDir.mkdir();
        }

        File scoresFile = new File(tourneyDir, "scores");
        if (!scoresFile.exists()) {
        	scoresFile.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(scoresFile));
            Iterator<String> i = cumulativeScores.keySet().iterator();
            while(i.hasNext()){
            	bw.write(i.next());
            	if(i.hasNext()){
            		bw.write(", ");
            	}
            }
            bw.write("\n");
            bw.flush();
            bw.close();
        }

        // Open up the XML file for this match, and save the match there.
//        f = new File(tourneyName + "/" + match.getMatchId() + ".xml");
//        if (f.exists()) f.delete();
//        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
//        bw.write(match.toXML());
//        bw.flush();
//        bw.close();

        // Open up the JSON file for this match, and save the match there.
        File matchFile = new File(tourneyDir,"game" + gameNum + ".json");
        log("Saving match to file " + matchFile.getPath(), gameNum);
        if (matchFile.exists()) matchFile.delete();
        BufferedWriter bw = new BufferedWriter(new FileWriter(matchFile));
        bw.write(new JSONObject(match.toJSON()).toString(4));
        bw.flush();
        bw.close();

        // Save the goals in the "/scores" file for the tournament.
        bw = new BufferedWriter(new FileWriter(scoresFile, true));
        Iterator<Integer> i = cumulativeScores.values().iterator();
        while(i.hasNext()){
        	bw.write(i.next().toString());
        	if(i.hasNext()){
        		bw.write(", ");
        	}
        }
        bw.write("\n");
        bw.flush();
        bw.close();
    }

	private static void log(String message, Integer i) {
		StringBuilder sb = new StringBuilder();
		sb.append(new SimpleDateFormat("HH:mm:ss").format(new Date()));
		if(i != null){
			sb.append(" (Game " + i + ")");
		}
		sb.append(" - ");
		sb.append(message);
		System.out.println(sb.toString());
	}
}
