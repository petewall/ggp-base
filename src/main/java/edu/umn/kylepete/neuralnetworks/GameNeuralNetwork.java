package edu.umn.kylepete.neuralnetworks;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

public class GameNeuralNetwork {

	// higher values of P make the higher goal values more influential, meaning riskier behavior
	// for P = 1, all goal values are considered equally
	// for P = 100, only the highest goal value is considered
	private static final double P = 2.0;

	// LAMBDA is the rate of goal decay as the game states get further way from the terminal
	// 1 means no decay, all the states will be trained with the full goal value
	// 0 means full day, only the terminal state is rewarded
	private static final double LAMBDA = 0.7;

	private Game game;
	private Prover prover;
	private int trainCount = 0; // TODO adjust learning rate based on train count
	Map<String, Set<NeuralNetwork>> networks;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("GameNeuralNetwork for game ");
		if(game.getName() != null){
			sb.append(game.getName());
		}else{
			sb.append(GameNeuralNetworkDatabase.getGameHash(game));
		}
		sb.append(" ");
		if(game.getRepositoryURL() != null){
			sb.append(" (");
			sb.append(game.getRepositoryURL());
			sb.append(")");
		}
		int totalNetworks = 0;
		int totalNeurons = 0;
		StringBuilder netSb = new StringBuilder();
		for(Set<NeuralNetwork> netSet : networks.values()){
			for(NeuralNetwork net : netSet){
				totalNetworks++;
				totalNeurons += net.getTotalNeuronCount();
				netSb.append("\n  ");
				netSb.append(net.toString());
			}
		}
		sb.append("\n  Neural Network count: " + totalNetworks + "  Total neurons: " + totalNeurons);
		sb.append(netSb.toString());
		return sb.toString();
	}

	private GameNeuralNetwork(){
		this.networks = new LinkedHashMap<String, Set<NeuralNetwork>>();
	}

	public GameNeuralNetwork(Game game) throws InterruptedException {
		this();
		if(game == null){
			throw new IllegalArgumentException("game cannot be null");
		}
		if(game.getRules() == null || game.getRules().size() == 0){
			throw new IllegalArgumentException("game rules cannot be null or empty");
		}
		this.game = game;
		this.prover = new AimaProver(game.getRules());
		PropNet propNet = OptimizingPropNetFactory.create(game.getRules());
		Map<Role, Set<Proposition>> goalMap = propNet.getGoalPropositions();
		for (Role role : goalMap.keySet()) {
			Set<NeuralNetwork> networksForRole = networks.get(role);
			if (networksForRole == null) {
				networksForRole = new LinkedHashSet<NeuralNetwork>();
				networks.put(role.toString(), networksForRole);
			}
			for (Proposition goal : goalMap.get(role)) {
				networksForRole.add(NeuralNetwork.createFromPropNet(goal, prover));
			}
		}
	}

	public int getTrainCount(){
		return this.trainCount;
	}

	public Game getGame(){
		return this.game;
	}

	public Set<String> getRoles() {
		return this.networks.keySet();
	}

	public double evaluateState(String role, Set<GdlSentence> state) {
		double sumOfEvaluations = 0.0;
		double totalPossible = 0.0;
		for (NeuralNetwork network : networks.get(role)) {
			int goalValue = network.getGdlGoalValue();
			totalPossible += Math.pow(goalValue, P);
			double output = network.evaluateState(state);
			// System.out.println(network.getGdlGoal() + " = " + output);
			// normalize network output [-1, 1] to the range [0, 1]
			output = (output + 1.0) / 2.0;
			// multiply by the goal value
			output = output * Math.pow(goalValue, P);
			sumOfEvaluations += output;
		}
		double finalEvaluation = sumOfEvaluations / totalPossible * 100;
		// as a final adjustment, we ensure our estimate is always less than 100 and greater than 0
		return finalEvaluation * 0.98 + 1;
	}

	public double evaluateState(String role, MachineState state) {
		return evaluateState(role, state.getContents());
	}

	public double evaluateState(Role role, Set<GdlSentence> state) {
		return evaluateState(role.toString(), state);
	}

	public double evaluateState(Role role, MachineState state) {
		return evaluateState(role.toString(), state.getContents());
	}

	public String printEvaluations(MachineState state) {
		return printEvaluations(state.getContents());
	}

	public String printEvaluations(Set<GdlSentence> state) {
		StringBuilder sb = new StringBuilder();
		for (String role : getRoles()) {
			sb.append(role);
			sb.append(" = ");
			sb.append(evaluateState(role, state));
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	public double evaluationError(Match match){
		if (!match.isCompleted()) {
			return 0.0;
		}

		List<Role> roles = Role.computeRoles(match.getGame().getRules());
		List<Integer> goals = match.getGoalValues();
		if (roles.size() != goals.size()) {
			return 0.0;
		}

		double totalError = 0.0;
		List<Set<GdlSentence>> stateHistory = match.getStateHistory();
		int totalStates = stateHistory.size();
		for (int s = 0; s < totalStates; s++) {
			Set<GdlSentence> state = stateHistory.get(s);
			for (int r = 0; r < roles.size(); r++) {
				int goal = goals.get(r);
				Set<NeuralNetwork> roleNets = networks.get(roles.get(r).toString());
				for (NeuralNetwork roleNet : roleNets) {
					double expectedGoal = -1.0;
					if (goal == roleNet.getGdlGoalValue()) {
						expectedGoal = 1.0;
					}
					expectedGoal = expectedGoal * Math.pow(LAMBDA, totalStates - 1 - s);
					double evaluation = roleNet.evaluateState(state);
					// mean squared error
					double error = 0.5 * Math.pow(expectedGoal - evaluation, 2);
					totalError += error;
				}
			}
		}

		return totalError;
	}

	public void train(Match match) {
		if (!match.isCompleted()) {
			return;
		}

		List<Role> roles = Role.computeRoles(match.getGame().getRules());
		List<Integer> goals = match.getGoalValues();
		List<Set<GdlSentence>> stateHistory = match.getStateHistory();

		if (roles.size() != goals.size()) {
			return;
		}
		int totalStates = stateHistory.size();
		for (int s = 0; s < totalStates; s++) {
			Set<GdlSentence> state = stateHistory.get(s);
			for (int r = 0; r < roles.size(); r++) {
				int goal = goals.get(r);
				Set<NeuralNetwork> roleNets = networks.get(roles.get(r).toString());
				for (NeuralNetwork roleNet : roleNets) {
					double expectedGoal = -1.0;
					if (goal == roleNet.getGdlGoalValue()) {
						expectedGoal = 1.0;
					}
					// TODO could perform mini-max search to see if this state is provable
					// temporal difference decay
					expectedGoal = expectedGoal * Math.pow(LAMBDA, totalStates - 1 - s);
					//Double evalBefore = roleNet.evaluateState(state);
					roleNet.train(state, expectedGoal, trainCount);
					//Double evalAfter = roleNet.evaluateState(state);
					//System.out.println("Training net " + roleNet.getGdlGoal() + " with expectedGoal " + expectedGoal + "   Before: " + evalBefore + "   After: " + evalAfter);
				}
			}
		}
		trainCount++;
	}



	public String toJSON() throws JSONException {
		return toJSONObject().toString(4);
	}

	private static final String GAME = "game";
	private static final String TRAIN_COUNT = "trainCount";

	public JSONObject toJSONObject() throws JSONException {
		JSONObject theJSON = new JSONObject();
		theJSON.put(TRAIN_COUNT, this.trainCount);
		theJSON.put(GAME, new JSONObject(this.game.serializeToJSON()));
		for(String role : networks.keySet()){
			JSONArray networksJSONArray = new JSONArray();
			for(NeuralNetwork network : networks.get(role)){
				networksJSONArray.put(network.toJSONObject());
			}
			theJSON.put(role, networksJSONArray);
		}
		return theJSON;
	}


	public static GameNeuralNetwork fromJSON(String theJSON) throws JSONException, InterruptedException {
		return fromJSON(new JSONObject(theJSON));
	}

	public static GameNeuralNetwork fromJSON(JSONObject theJSON) throws JSONException, InterruptedException {
		GameNeuralNetwork gameNetwork = new GameNeuralNetwork();
		gameNetwork.trainCount = theJSON.getInt(TRAIN_COUNT);
		gameNetwork.game = Game.loadFromJSON(theJSON.getString(GAME));
		gameNetwork.prover = new AimaProver(gameNetwork.game.getRules());
		Iterator<?> iter = theJSON.keys();
		while(iter.hasNext()){
			String role = (String)iter.next();
			if(!role.equals(GAME) && !role.equals(TRAIN_COUNT)){
				Set<NeuralNetwork> networksForRole = gameNetwork.networks.get(role);
				if (networksForRole == null) {
					networksForRole = new LinkedHashSet<NeuralNetwork>();
					gameNetwork.networks.put(role, networksForRole);
				}
				JSONArray networksJSONArray = theJSON.getJSONArray(role);
				for(int i = 0; i < networksJSONArray.length(); i++){
					networksForRole.add(NeuralNetwork.fromJSON(networksJSONArray.getJSONObject(i), gameNetwork.prover));
				}
			}
		}
		return gameNetwork;
	}
}
