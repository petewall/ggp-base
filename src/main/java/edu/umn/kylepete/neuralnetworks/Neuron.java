package edu.umn.kylepete.neuralnetworks;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlSentence;

public class Neuron {

	public enum NeuronType {
		AND, OR, INPUT, CONSTANT, BIAS
	}

	private NeuronType type;
	private Double value;
	private Double weight;
	private List<Neuron> inputs;
	private GdlSentence gdlSentence = null;

	public Neuron(NeuronType type, double weight) {
		this.type = type;
		this.weight = weight;
		this.inputs = new ArrayList<Neuron>();
	}

	public Neuron(NeuronType type, double weight, GdlSentence gdlSentence) {
		this.type = type;
		this.weight = weight;
		this.inputs = new ArrayList<Neuron>();
		this.gdlSentence = gdlSentence;
	}

	public NeuronType getNeuronType() {
		return type;
	}

	public void setNeuronType(NeuronType type) {
		this.type = type;
	}

	public Double getValue() {
		if (type == NeuronType.INPUT || type == NeuronType.CONSTANT || type == NeuronType.BIAS) {
			return value;
		}
		double sum = 0.0;
		for (Neuron input : getInputs()) {
			sum += input.getValue() * input.getWeight();
		}
		double activation =  activationFunction(sum);
		setValue(activation);
		// System.out.println("activation(" + sum + ") = " + activation);
		return activation;
	}

	private double activationFunction(double in) {
		return (2.0 / (1.0 + Math.exp(-NeuralNetwork.BETA * in))) - 1.0;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public List<Neuron> getInputs() {
		return inputs;
	}

	public void addInput(Neuron inputNode) {
		this.inputs.add(inputNode);
	}

	public GdlSentence getGdlSentence() {
		return gdlSentence;
	}

	public void setGdlSentence(GdlSentence gdlSentence) {
		this.gdlSentence = gdlSentence;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, "");
		return sb.toString();
	}

	public void toString(StringBuilder sb, String indent) {
		sb.append(indent);
		if (weight != null) {
			sb.append(weight);
			sb.append(" = ");
		}
		sb.append(type.toString());
		if (gdlSentence != null) {
			sb.append(" ");
			sb.append(gdlSentence.toString());
		}
		sb.append(System.lineSeparator());
		indent += "    ";
		for (Neuron input : getInputs()) {
			input.toString(sb, indent);
		}
	}
}
