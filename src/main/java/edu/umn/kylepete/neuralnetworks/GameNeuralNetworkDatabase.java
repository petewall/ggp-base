package edu.umn.kylepete.neuralnetworks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.DatatypeConverter;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.match.Match;

import external.JSON.JSONArray;
import external.JSON.JSONException;

public class GameNeuralNetworkDatabase {

	public static final String DEFAULT_FILE = "archives/defaultGameDatabase.json";

	private ConcurrentHashMap<String, GameNeuralNetwork> gameDatabase = new ConcurrentHashMap<String, GameNeuralNetwork>();

	public boolean containsGame(Game game){
		return gameDatabase.containsKey(getGameHash(game));
	}

	public GameNeuralNetwork getGameNeuralNetwork(Game game) {
		GameNeuralNetwork gameNetwork = gameDatabase.get(getGameHash(game));
		if(gameNetwork == null){
			gameNetwork = addNewGame(game);
		}
		return gameNetwork;
	}

	public synchronized GameNeuralNetwork addExistingGame(GameNeuralNetwork gameNetwork) {
		Game game = gameNetwork.getGame();
		String key = getGameHash(game);
		if(gameDatabase.containsKey(key)){
			throw new IllegalStateException("The game database already contains the game " + game.getRepositoryURL());
		}else{
			gameDatabase.put(key, gameNetwork);
			return gameNetwork;
		}
	}

	public synchronized GameNeuralNetwork addNewGame(Game game) {
		String key = getGameHash(game);
		if(gameDatabase.containsKey(key)){
			throw new IllegalStateException("The game database already contains the game " + game.getRepositoryURL());
		}else{
			GameNeuralNetwork gameNetwork;
			try {
				gameNetwork = new GameNeuralNetwork(game);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			gameDatabase.put(key, gameNetwork);
			return gameNetwork;
		}
	}

	public void forgetGame(Game game){
		gameDatabase.remove(getGameHash(game));
	}

	public void train(Match match) {
		getGameNeuralNetwork(match.getGame()).train(match);
	}

	public String toJSON() throws JSONException{
		return toJSONArray().toString(4);
	}

	public JSONArray toJSONArray() throws JSONException{
		JSONArray theJSON = new JSONArray();
		for(GameNeuralNetwork net : gameDatabase.values()){
			theJSON.put(net.toJSONObject());
		}
		return theJSON;
	}

	public static GameNeuralNetworkDatabase fromJSON(String theJSON) throws JSONException, InterruptedException {
		return fromJSON(new JSONArray(theJSON));
	}

	public static GameNeuralNetworkDatabase fromJSON(JSONArray theJSON) throws JSONException, InterruptedException {
		GameNeuralNetworkDatabase gameDatabase = new GameNeuralNetworkDatabase();
		for(int i = 0; i < theJSON.length(); i++){
			GameNeuralNetwork gameNetwork = GameNeuralNetwork.fromJSON(theJSON.getJSONObject(i));
			gameDatabase.gameDatabase.put(getGameHash(gameNetwork.getGame()), gameNetwork);
		}
		return gameDatabase;
	}

	public void writeToFile(String filePath) throws IOException, JSONException{
		writeFile(filePath, toJSON());
	}

	public void writeToDefaultFile() throws IOException, JSONException{
		writeToFile(DEFAULT_FILE);
	}

	public static GameNeuralNetworkDatabase readFromFile(String filePath) throws JSONException, InterruptedException, IOException{
		return fromJSON(readFile(filePath));
	}

	public static GameNeuralNetworkDatabase readFromDefaultFile() throws JSONException, InterruptedException, IOException{
		if(Files.exists(Paths.get(DEFAULT_FILE))){
			return readFromFile(DEFAULT_FILE);
		}else{
			return new GameNeuralNetworkDatabase();
		}
	}

	private static String readFile(String filePath) throws IOException{
		return new String(Files.readAllBytes(Paths.get(filePath)));
	}

	private static synchronized void writeFile(String filePath, String contents) throws IOException{
		Files.write(Paths.get(filePath), contents.getBytes());
	}

	static String getGameHash(Game game){
		String compressedRulesheet = game.getRulesheet().replaceAll("\\s+", "");
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(compressedRulesheet.getBytes());
			return DatatypeConverter.printBase64Binary(md.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public Collection<GameNeuralNetwork> getAllGameNetworks() {
		return this.gameDatabase.values();
	}
}
