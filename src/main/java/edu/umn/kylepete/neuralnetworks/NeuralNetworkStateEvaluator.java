package edu.umn.kylepete.neuralnetworks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

public class NeuralNetworkStateEvaluator {

	// higher values of P make the higher goal values more influential, meaning riskier behavior
	// for P = 1, all goal values are considered equally
	// for P = 100, only the highest goal value is considered
	private static final double P = 2.0;

	private Map<Role, Set<NeuralNetwork>> networks;

	public NeuralNetworkStateEvaluator(List<Gdl> gameRules) throws InterruptedException{
		this.networks = new HashMap<Role, Set<NeuralNetwork>>();
		AimaProver prover = new AimaProver(gameRules);
		PropNet propNet = OptimizingPropNetFactory.create(gameRules);
		Map<Role, Set<Proposition>> goalMap = propNet.getGoalPropositions();
		for(Role role : goalMap.keySet()){
			Set<NeuralNetwork> networksForRole = networks.get(role);
			if(networksForRole == null){
				networksForRole = new HashSet<NeuralNetwork>();
				networks.put(role, networksForRole);
			}
			for(Proposition goal : goalMap.get(role)){
				networksForRole.add(NeuralNetwork.createFromPropNet(goal, prover));
			}
		}
	}

	public Set<Role> getRoles(){
		return this.networks.keySet();
	}

	public double evaluateState(Role role, Set<GdlSentence> state){
		double sumOfEvaluations = 0.0;
		double totalPossible = 0.0;
		for(NeuralNetwork network : networks.get(role)){
			int goalValue = network.getGdlGoalValue();
			totalPossible += Math.pow(goalValue, P);
			double output = network.evaluateState(state);
//			System.out.println(network.getGdlGoal() + " = " + output);
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

	public double evaluateState(Role role, MachineState state) {
		return evaluateState(role, state.getContents());
	}

	public String printEvaluations(MachineState state){
		StringBuilder sb = new StringBuilder();
		for(Role role : getRoles()){
			sb.append(role);
			sb.append(" = ");
			sb.append(evaluateState(role, state));
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}
}
