package edu.umn.kylepete.neuralnetworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.statemachine.Role;

import edu.umn.kylepete.neuralnetworks.Neuron.NeuronType;

public class NeuralNetwork {

	static final long SEED = 12345;
	static final Random random = new Random(SEED);
	static final double ALPHA = 0.2;
	static final double BETA = 1.0;
	private static final double W = 1.0;
	private double a_min;
	private double a_max;
	private int disj = 0; // largest number of children an OR node has
	private int conj = 0; // largest number of children an AND node has

	private List<Neuron> inputNeurons;
	private Neuron outputNeuron;
	private int neurons;
	private int layers;
	private GdlSentence gdlGoal;
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

	public GdlSentence getGdlGoal(){
		return this.gdlGoal;
	}

	public Role getGdlRoleForGoal(){
		return Role.create(gdlGoal.get(0).toString());
	}

	public int getGdlGoalValue(){
        return Integer.parseInt(gdlGoal.get(1).toString());
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

	private void setInputValues(Set<GdlSentence> state){
		for(Neuron input : this.inputNeurons){
			if(prover.prove(input.getGdlSentence(), state)){
				input.setValue(1.0);
			}else{
				input.setValue(-1.0);
			}
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

	public void populateNetworkStats(){
		populateNetworkStatsRecursive(outputNeuron, 1);
		int maxChildren = Math.max(disj, conj);

		double l_a = (double)(maxChildren - 1) / (double)(maxChildren + 1);
		a_min = (l_a -1) * ALPHA + 1;
		a_max = -a_min;
	}

	private void populateNetworkStatsRecursive(Neuron node, int depth){
		if(node != null){
			this.neurons++;
			if(depth > this.layers){
				this.layers = depth;
			}
		}
		int children = node.getInputs().size();
		if(children > 0){
			if(node.getNeuronType() == NeuronType.AND){
				if(children > this.conj){
					this.conj = children;
				}
			} else if(node.getNeuronType() == NeuronType.OR){
				if(children > this.disj){
					this.disj = children;
				}
			} else{
				throw new IllegalStateException("Found a neuron with " + children + " input nodes but it is type " + node.getNeuronType().toString());
			}
			for(Neuron child : node.getInputs()){
				populateNetworkStatsRecursive(child, depth + 1);
			}
		}
	}

	// public static NeuralNetwork createFromGdl(List<Gdl> gdlDescription) throws InterruptedException{
	// return createFromPropNet(OptimizingPropNetFactory.create(gdlDescription));
	// }

	public static NeuralNetwork createFromPropNet(Proposition goalProposition, Prover prover) {
		NeuralNetwork network = new NeuralNetwork();
		network.prover = prover;
		network.gdlGoal = goalProposition.getName();
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
			if(comp.getValue()){
				newNode.setValue(1.0);
			}else{
				newNode.setValue(-1.0);
			}
		} else if (comp instanceof Transition) {
			type = NeuronType.INPUT;
			this.inputNeurons.add(newNode);
		}
		newNode.setNeuronType(type);
		if (type != NeuronType.INPUT && type != NeuronType.CONSTANT) {
			for (Component input : comp.getInputs()) {
				Neuron inputNeuron = createNeuronForCompRecursive(input);
				if(inputNeuron != null){
					newNode.addInput(inputNeuron);
				}
			}
		}

		return newNode;
	}

	private void addBiasRecursive(Neuron neuron){
		int numInputs = neuron.getInputs().size();
		if (numInputs > 0) {
			double w = (1.0 + a_min) * W / 2.0;
			if(neuron.getNeuronType() == NeuronType.AND){
				w = w * (1.0 - numInputs);
			}else if(neuron.getNeuronType() == NeuronType.OR){
				w = w * (numInputs - 1.0);
			}
			w = randomize(w);
			this.neurons++;
			Neuron bias = new Neuron(NeuronType.BIAS, w);
			bias.setValue(1.0);
			neuron.addInput(bias);
			for(Neuron input : neuron.getInputs()){
				addBiasRecursive(input);
			}
		}
	}

	private double randomize(double x){
		double r = 0.0000001; // the small float will be in the range (-r, r)
		return x + random.nextDouble() * (r * 2) - r;
	}

}
