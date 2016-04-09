package edu.umn.kylepete.neuralnetworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.prover.Prover;

import edu.umn.kylepete.neuralnetworks.Neuron.NeuronType;
import external.JSON.JSONException;
import external.JSON.JSONObject;

public class NeuralNetwork {

	private static final long SEED = 12345;
	private static final Random random = new Random(SEED);
	private static final double R = 0.001; // the small float to initialize weights in the range (-R, R)
	private static final double ALPHA = 0.2;
	static final double BETA = 1.0; // don't change this or the derivative of sigmoid would change
	private static final double LEARNING_RATE = 0.1;
	private static final double W = 1.0;
	private double a_min;
	private double a_max;
	private int disj = 0; // largest number of children an OR node has
	private int conj = 0; // largest number of children an AND node has

	private List<Neuron> inputNeurons;
	private Neuron outputNeuron;
	private int neurons;
	private int layers;
	private String gdlGoal;
	private Prover prover;

	private NeuralNetwork() {
		this.inputNeurons = new ArrayList<Neuron>();
		this.neurons = 0;
		this.layers = 0;
	}

	public int getTotalNeuronCount() {
		return this.neurons;
	}

	public int getTotalLayerCount() {
		return this.layers;
	}

	public String getGdlGoal() {
		return this.gdlGoal;
	}

	public String getGdlRoleForGoal() {
		GdlSentence gdl;
		try {
			gdl = (GdlSentence) GdlFactory.create(this.gdlGoal);
		} catch (Exception e) {
			throw new RuntimeException("Error parsing GDL goal", e);
		}
		return gdl.get(0).toString();
	}

	public int getGdlGoalValue() {
		GdlSentence gdl;
		try {
			gdl = (GdlSentence) GdlFactory.create(this.gdlGoal);
		} catch (Exception e) {
			throw new RuntimeException("Error parsing GDL goal", e);
		}
		return Integer.parseInt(gdl.get(1).toString());
	}

	public List<Neuron> getInputNeurons() {
		return this.inputNeurons;
	}

	public Neuron getOutputNeuron() {
		return this.outputNeuron;
	}

	public double evaluateState(Set<GdlSentence> state) {
		setInputValues(state);
		return this.outputNeuron.getValue();
	}

	private void setInputValues(Set<GdlSentence> state) {
		for (Neuron input : this.inputNeurons) {
			GdlSentence gdl;
			try {
				gdl = (GdlSentence) GdlFactory.create(input.getGdlSentence());
			} catch (Exception e) {
				throw new RuntimeException("Error parsing GDL from Neuron", e);
			}
			if (prover.prove(gdl, state)) {
				input.setValue(1.0);
			} else {
				input.setValue(-1.0);
			}
		}
	}

	public void train(Set<GdlSentence> state, double expectedValue) {
		// run the network forward to set all the values
		double evaluation = evaluateState(state);

		// backpropagate the error
		double error = evaluation - expectedValue;
		backpropagateRecursive(outputNeuron, error);
	}

	private void backpropagateRecursive(Neuron output, double outputError) {
		for (Neuron input : output.getInputs()) {
			double inputError = input.getWeight() * outputError * input.getValue() * (1 - input.getValue());
			input.setWeight(input.getWeight() - LEARNING_RATE * outputError * input.getValue());
			backpropagateRecursive(input, inputError);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NeuralNetwork for goal: " + gdlGoal);
		sb.append(System.lineSeparator());
		sb.append("    Total layers: " + layers);
		sb.append(System.lineSeparator());
		sb.append("    Total neurons: " + neurons);
		sb.append(System.lineSeparator());
		sb.append("    Max conj: " + conj);
		sb.append(System.lineSeparator());
		sb.append("    Max disj: " + disj);
		sb.append(System.lineSeparator());
		sb.append("    A_min: " + a_min);
		sb.append(System.lineSeparator());
		sb.append("    A_max: " + a_max);
		sb.append(System.lineSeparator());
		this.outputNeuron.toString(sb, "    ");
		return sb.toString();
	}

	public void populateNetworkStats() {
		populateNetworkStatsRecursive(outputNeuron, 1);
		int maxChildren = Math.max(disj, conj);

		double l_a = (double) (maxChildren - 1) / (double) (maxChildren + 1);
		a_min = (l_a - 1) * ALPHA + 1;
		a_max = -a_min;
	}

	private void populateNetworkStatsRecursive(Neuron node, int depth) {
		if(node == null){
			return;
		}
		this.neurons++;
		if (depth > this.layers) {
			this.layers = depth;
		}
		if(node.getNeuronType() == NeuronType.INPUT){
			this.inputNeurons.add(node);
		}
		int children = node.getInputs().size();
		for (Neuron child : node.getInputs()) {
			if (child.getNeuronType() == NeuronType.BIAS) {
				// don't count BIAS nodes in the conj and disj
				children--;
			}
		}
		if (node.getNeuronType() == NeuronType.AND && children > this.conj) {
			this.conj = children;
		} else if (node.getNeuronType() == NeuronType.OR && children > this.disj) {
			this.disj = children;
		}

		for (Neuron child : node.getInputs()) {
			populateNetworkStatsRecursive(child, depth + 1);
		}
	}

	public static NeuralNetwork createFromPropNet(Proposition goalProposition, Prover prover) {
		NeuralNetwork network = new NeuralNetwork();
		network.prover = prover;
		network.gdlGoal = goalProposition.getName().toString();
		network.outputNeuron = network.createNeuronForCompRecursive(goalProposition);
		network.outputNeuron.setWeight(null);
		network.populateNetworkStats();
		network.addBiasRecursive(network.outputNeuron);
		return network;
	}

	private Neuron createNeuronForCompRecursive(Component comp) {
		if (comp instanceof Transition || comp instanceof Constant) {
			return null;
		}

		boolean not = false;
		GdlSentence gdl = null;
		while (comp instanceof Proposition || comp instanceof Not) {
			if (comp instanceof Not) {
				not = !not;
			}
			if (comp instanceof Proposition) {
				gdl = ((Proposition) comp).getName();
			}
			comp = comp.getSingleInput();
		}

		// add a small random float to avoid symmetry while learning
		double w = randomize(W);
		if (not) {
			w = -w;
		}
		NeuronType type = null;
		Neuron newNode = new Neuron(type, w, gdl);
		if (comp instanceof And) {
			type = NeuronType.AND;
		} else if (comp instanceof Or) {
			type = NeuronType.OR;
		} else if (comp instanceof Constant) {
			type = NeuronType.CONSTANT;
			if (comp.getValue()) {
				newNode.setValue(1.0);
			} else {
				newNode.setValue(-1.0);
			}
		} else if (comp instanceof Transition) {
			type = NeuronType.INPUT;
			//this.inputNeurons.add(newNode); this is now done in populate stats
		}
		newNode.setNeuronType(type);
		if (type != NeuronType.INPUT && type != NeuronType.CONSTANT) {
			for (Component input : comp.getInputs()) {
				Neuron inputNeuron = createNeuronForCompRecursive(input);
				if (inputNeuron != null) {
					newNode.addInput(inputNeuron);
				}
			}
		}

		return newNode;
	}

	private void addBiasRecursive(Neuron neuron) {
		int numInputs = neuron.getInputs().size();
		if (numInputs > 0) {
			double w = (1.0 + a_min) * W / 2.0;
			if (neuron.getNeuronType() == NeuronType.AND) {
				w = w * (1.0 - numInputs);
			} else if (neuron.getNeuronType() == NeuronType.OR) {
				w = w * (numInputs - 1.0);
			}
			w = randomize(w);
			this.neurons++;
			Neuron bias = new Neuron(NeuronType.BIAS, w);
			bias.setValue(1.0);
			neuron.addInput(bias);
			for (Neuron input : neuron.getInputs()) {
				addBiasRecursive(input);
			}
		}
	}

	private double randomize(double x) {
		return x + random.nextDouble() * (R * 2) - R;
	}

	public String toJSON() throws JSONException {
		return toJSONObject().toString(4);
	}

	private static final String GOAL = "goal";
	private static final String OUTPUT_NEURON = "outputNeuron";

	public JSONObject toJSONObject() throws JSONException {
		JSONObject theJSON = new JSONObject();
		theJSON.put(GOAL, this.gdlGoal);
		theJSON.put(OUTPUT_NEURON, this.outputNeuron.toJSONObject());
		return theJSON;
	}

	public static NeuralNetwork fromJSON(String theJSON, Prover prover) throws JSONException {
		return fromJSON(new JSONObject(theJSON), prover);
	}

	public static NeuralNetwork fromJSON(JSONObject theJSON, Prover prover) throws JSONException {
		NeuralNetwork network = new NeuralNetwork();
		network.gdlGoal = theJSON.getString(GOAL);
		network.outputNeuron = Neuron.fromJSON(theJSON.getJSONObject(OUTPUT_NEURON));
		network.prover = prover;
		network.populateNetworkStats();
		return network;
	}
}
