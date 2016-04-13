package edu.umn.kylepete.neuralnetworks;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlSentence;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

public class Neuron {

	public enum NeuronType {
		AND, OR, INPUT, CONSTANT, BIAS
	}

	private NeuronType type;
	private Double value;
	private Double weight;
	private Double deltaWeight;
	private List<Neuron> inputs;
	private String gdlSentence = null;

	public Neuron(NeuronType type, Double weight) {
		this.type = type;
		this.weight = weight;
		this.deltaWeight = 0.0;
		this.inputs = new ArrayList<Neuron>();
	}

	public Neuron(NeuronType type, Double weight, String gdlSentence) {
		this.type = type;
		this.weight = weight;
		this.deltaWeight = 0.0;
		this.inputs = new ArrayList<Neuron>();
		this.gdlSentence = gdlSentence;
	}

	public Neuron(NeuronType type, Double weight, GdlSentence gdlSentence) {
		this(type, weight, gdlSentence == null ? null : gdlSentence.toString());
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
		double activation = activationFunction(sum);
		setValue(activation);
		// System.out.println("activation(" + sum + ") = " + activation);
		return activation;
	}

	private double activationFunction(double in) {
		return (2.0 / (1.0 + Math.exp(-NeuralNetwork.BETA * in))) - 1.0;
	}

	void setValue(Double value) {
		this.value = value;
	}

	public Double getWeight() {
		return weight;
	}

	void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getDeltaWeight() {
		return deltaWeight;
	}

	void setDeltaWeight(Double weight) {
		this.deltaWeight = weight;
	}

	public List<Neuron> getInputs() {
		return inputs;
	}

	public void addInput(Neuron inputNode) {
		this.inputs.add(inputNode);
	}

	public String getGdlSentence() {
		return gdlSentence;
	}

	public void setGdlSentence(String gdlSentence) {
		this.gdlSentence = gdlSentence;
	}

	public void setGdlSentence(GdlSentence gdlSentence) {
		this.gdlSentence = gdlSentence == null ? null : gdlSentence.toString();
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
			sb.append(gdlSentence);
		}
		sb.append(System.lineSeparator());
		indent += "    ";
		for (Neuron input : getInputs()) {
			input.toString(sb, indent);
		}
	}

	public String toJSON() throws JSONException{
		return toJSONObject().toString(4);
	}

	private static final String GDL = "gdl";
	private static final String NEURON_TYPE = "neuronType";
	private static final String WEIGHT = "weight";
	private static final String DELTA_WEIGHT = "deltaWeight";
	private static final String VALUE = "value";
	private static final String INPUT_NEURONS = "inputNeurons";

	public JSONObject toJSONObject() throws JSONException {
		JSONObject theJSON = new JSONObject();
		if(this.gdlSentence != null){
			theJSON.put(GDL, this.gdlSentence);
		}
		theJSON.put(NEURON_TYPE, this.type.toString());
		theJSON.put(WEIGHT, this.weight);
		theJSON.put(DELTA_WEIGHT, this.deltaWeight);
		if (type == NeuronType.CONSTANT || type == NeuronType.BIAS) {
			theJSON.put(VALUE, this.value);
		}
		JSONArray inputJSONArray = new JSONArray();
		for(Neuron input : this.inputs){
			inputJSONArray.put(input.toJSONObject());
		}
		if(inputJSONArray.length() > 0){
			theJSON.put(INPUT_NEURONS, inputJSONArray);
		}
		return theJSON;
	}

	public static Neuron fromJSON(String theJSON) throws JSONException{
		return fromJSON(new JSONObject(theJSON));
	}

	public static Neuron fromJSON(JSONObject theJSON) throws JSONException{
		String gdl = null;
		if(theJSON.has(GDL)){
			 gdl = theJSON.getString(GDL);
		}
		NeuronType type = NeuronType.valueOf(theJSON.getString(NEURON_TYPE));
		Double weight = null;
		if(theJSON.has(WEIGHT)){
			weight = theJSON.getDouble(WEIGHT);
		}
		Neuron neuron = new Neuron(type, weight, gdl);
		if(theJSON.has(VALUE)){
			neuron.setValue(theJSON.getDouble(VALUE));
		}
		if(theJSON.has(DELTA_WEIGHT)){
			neuron.deltaWeight = theJSON.getDouble(DELTA_WEIGHT);
		}
		if(theJSON.has(INPUT_NEURONS)){
			JSONArray inputJSONArray = theJSON.getJSONArray(INPUT_NEURONS);
			for(int i = 0; i < inputJSONArray.length(); i++){
				neuron.addInput(fromJSON(inputJSONArray.getJSONObject(i)));
			}
		}
		return neuron;
	}
}
