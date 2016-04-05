package edu.umn.kylepete.neuralnetworks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.game.TestGameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.prover.aima.knowledge.KnowledgeBase;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class NeuralNetworkTest extends Assert {

	protected final ProverStateMachine sm = new ProverStateMachine();
	protected final GdlConstant C1 = GdlPool.getConstant("1");
	protected final GdlConstant C2 = GdlPool.getConstant("2");
	protected final GdlConstant C3 = GdlPool.getConstant("3");
	protected final GdlConstant C50 = GdlPool.getConstant("50");
	protected final GdlConstant C100 = GdlPool.getConstant("100");


	public static void main(String[] args) throws InterruptedException, MoveDefinitionException, TransitionDefinitionException {
		GameRepository gameRepo = GameRepository.getDefaultRepository();
		// for(String gameKey : gameRepo.getGameKeys()){
		// System.out.println(gameKey);
		// }

		Game game = gameRepo.getGame("ticTacToe");
		List<Gdl> rules = game.getRules();



		// rules = new PropNetFlattener(rules).flatten();
//		KnowledgeBase knowledgeBase = new KnowledgeBase(Sets.newHashSet(rules));
//		List<GdlRule> goals = knowledgeBase.fetch(GdlPool.getProposition(GdlPool.GOAL));
//		List<GdlRule> goals2 = knowledgeBase.fetch(GdlPool.getRelation(GdlPool.GOAL));
		// printProp(goals.get(0), knowledgeBase, "");

		// ProverStateMachine sm = new ProverStateMachine();
		// sm.initialize(rules);
		 AimaProver prover = new AimaProver(rules);
		// MachineState state = sm.getInitialState();
		// sm.getNextStates(state);

		// System.out.println(rules);
		// List<GdlRule> flatDescription = new PropNetFlattener(rules).flatten();
		// System.out.println(flatDescription);

		// SentenceDomainModel model = SentenceDomainModelFactory.createWithCartesianDomains(rules);
		// ConstantChecker constantChecker = ConstantCheckerFactory.createWithForwardChaining(model);
		// constantChecker.

		PropNet propNet = OptimizingPropNetFactory.create(rules);
//		// propNet.renderToFile("tictactoe.dot");
		Map<Role, Set<Proposition>> goalMap = propNet.getGoalPropositions();
		Proposition prop = goalMap.get(goalMap.keySet().iterator().next()).iterator().next();
		NeuralNetwork net = NeuralNetwork.createFromPropNet(prop, prover);
		System.out.print(net.toString());
//		//printComp("", prop);
//		Node root = new Node();
//		root.setName(prop.getName().toString());
//		createNeuralNetwork(prop, root);
//		printNode("", root);
		System.out.println("Done");

	}

	private static void printComp(String indent, Component... comps) {
		for (Component comp : comps) {
			if (comp instanceof Proposition) {
				Proposition prop = (Proposition) comp;
				System.out.println(indent + prop.getName());
				printComp("    " + indent, comp.getInputs().toArray(new Component[0]));
			} else if (comp instanceof And) {
				System.out.println(indent + "AND");
				printComp("    " + indent, comp.getInputs().toArray(new Component[0]));
			} else if (comp instanceof Or) {
				System.out.println(indent + "OR");
				printComp("    " + indent, comp.getInputs().toArray(new Component[0]));
			} else if (comp instanceof Not) {
				System.out.println(indent + "NOT");
				printComp("    " + indent, comp.getInputs().toArray(new Component[0]));
			} else if (comp instanceof Constant) {
				System.out.println(indent + "CONSTANT" + comp.getValue());
				// TODO how to handle
			} else if (comp instanceof Transition) {
				// done
			}
		}
	}

	private static void printProp(Gdl prop, KnowledgeBase knowledge, String indent) {
		System.out.println(indent + prop);
		if (prop instanceof GdlDistinct) {
			GdlDistinct distinct = (GdlDistinct) prop;
		} else if (prop instanceof GdlNot) {
			GdlNot not = (GdlNot) prop;
		} else if (prop instanceof GdlOr) {
			GdlOr or = (GdlOr) prop;
		} else if (prop instanceof GdlRule) {
			GdlRule rule = (GdlRule) prop;
			for (GdlLiteral lit : rule.getBody()) {
				printProp(lit, knowledge, indent + "    ");
			}
		} else {
			GdlSentence sentence = (GdlSentence) prop;
			List<GdlRule> rules = knowledge.fetch(sentence);
			for (GdlRule rule : rules) {
				printProp(rule, knowledge, indent + "    ");
			}
		}
	}

	@Test
	public void testProverOnTicTacToe() throws Exception {
		List<Gdl> ticTacToeDesc = new TestGameRepository().getGame("ticTacToe").getRules();

		NeuralNetworkStateEvaluator neuralNetworkStateEvaluator = new NeuralNetworkStateEvaluator(ticTacToeDesc);

		sm.initialize(ticTacToeDesc);
		MachineState state = sm.getInitialState();
		assertFalse(sm.isTerminal(state));
		GdlConstant X_PLAYER = GdlPool.getConstant("xplayer");
		GdlConstant O_PLAYER = GdlPool.getConstant("oplayer");
		Role xRole = new Role(X_PLAYER);
		Role oRole = new Role(O_PLAYER);
		List<Role> roles = Arrays.asList(xRole, oRole);
		assertEquals(roles, sm.getRoles());
		assertEquals(9, sm.getLegalJointMoves(state).size());
		assertEquals(9, sm.getLegalMoves(state, xRole).size());
		assertEquals(1, sm.getLegalMoves(state, oRole).size());
		Move noop = new Move(GdlPool.getConstant("noop"));
		assertEquals(noop, sm.getLegalMoves(state, oRole).get(0));
		System.out.println("Testing Tic-tac-toe\nInit:");
		System.out.print(neuralNetworkStateEvaluator.printEvaluations(state));

		Move m11 = move("mark 1 1");
		assertTrue(sm.getLegalMoves(state, xRole).contains(m11));
		state = sm.getNextState(state, Arrays.asList(new Move[] { m11, noop }));
		assertFalse(sm.isTerminal(state));
		System.out.println("X " + m11);
		System.out.print(neuralNetworkStateEvaluator.printEvaluations(state));

		Move m13 = move("mark 1 3");
		assertTrue(sm.getLegalMoves(state, oRole).contains(m13));
		state = sm.getNextState(state, Arrays.asList(new Move[] { noop, m13 }));
		assertFalse(sm.isTerminal(state));
		System.out.println("O " + m13);
		System.out.print(neuralNetworkStateEvaluator.printEvaluations(state));

		Move m31 = move("mark 3 1");
		assertTrue(sm.getLegalMoves(state, xRole).contains(m31));
		state = sm.getNextState(state, Arrays.asList(new Move[] { m31, noop }));
		assertFalse(sm.isTerminal(state));
		System.out.println("X " + m31);
		System.out.print(neuralNetworkStateEvaluator.printEvaluations(state));

		Move m22 = move("mark 2 2");
		assertTrue(sm.getLegalMoves(state, oRole).contains(m22));
		state = sm.getNextState(state, Arrays.asList(new Move[] { noop, m22 }));
		assertFalse(sm.isTerminal(state));
		System.out.println("O " + m22);
		System.out.print(neuralNetworkStateEvaluator.printEvaluations(state));

		Move m21 = move("mark 2 1");
		assertTrue(sm.getLegalMoves(state, xRole).contains(m21));
		state = sm.getNextState(state, Arrays.asList(new Move[] { m21, noop }));
		assertTrue(sm.isTerminal(state));
		assertEquals(100, sm.getGoal(state, xRole));
		assertEquals(0, sm.getGoal(state, oRole));
		assertEquals(Arrays.asList(new Integer[] { 100, 0 }), sm.getGoals(state));
		System.out.println("X wins " + m21);
		System.out.print(neuralNetworkStateEvaluator.printEvaluations(state));

		// My expectations for the behavior, but there's no consensus...
		/*
		 * Move m23 = new Move(GdlPool.getRelation(PLAY, new GdlTerm[] {C2, C3, O})); try { sm.getNextState(state, Arrays.asList(new Move[] {noop, m23})); fail("Should throw an exception when trying to transition from a terminal state"); } catch(TransitionDefinitionException e) { //Expected }
		 */
	}


	@Test
	public void testCase1A() throws Exception {
		List<Gdl> desc = new TestGameRepository().getGame("test_case_1a").getRules();
		sm.initialize(desc);
		NeuralNetworkStateEvaluator neuralNetworkStateEvaluator = new NeuralNetworkStateEvaluator(desc);
		MachineState state = sm.getInitialState();
		System.out.println("\n\nTest case 1a\nInit:");
		System.out.print(neuralNetworkStateEvaluator.printEvaluations(state));
		Role you = new Role(GdlPool.getConstant("you"));
		assertFalse(sm.isTerminal(state));
		assertEquals(100, sm.getGoal(state, you));
		assertEquals(Collections.singletonList(100), sm.getGoals(state));
		state = sm.getNextState(state, Collections.singletonList(move("proceed")));
		assertTrue(sm.isTerminal(state));
		assertEquals(100, sm.getGoal(state, you));
		assertEquals(Collections.singletonList(100), sm.getGoals(state));
		System.out.println("Achieved goal:");
		System.out.print(neuralNetworkStateEvaluator.printEvaluations(state));
	}

	@Test
	public void testCase3C() throws Exception {
		List<Gdl> desc = new TestGameRepository().getGame("test_case_3c").getRules();
		sm.initialize(desc);
		MachineState state = sm.getInitialState();
		Role xplayer = new Role(GdlPool.getConstant("xplayer"));
		assertFalse(sm.isTerminal(state));
		assertEquals(1, sm.getLegalMoves(state, xplayer).size());
		assertEquals(move("win"), sm.getLegalMoves(state, xplayer).get(0));
		state = sm.getNextState(state, Collections.singletonList(move("win")));
		assertTrue(sm.isTerminal(state));
		assertEquals(100, sm.getGoal(state, xplayer));
		assertEquals(Collections.singletonList(100), sm.getGoals(state));
	}

	@Test
	public void testCase5A() throws Exception {
		List<Gdl> desc = new TestGameRepository().getGame("test_case_5a").getRules();
		sm.initialize(desc);
		MachineState state = sm.getInitialState();
		Role you = new Role(GdlPool.getConstant("you"));
		assertFalse(sm.isTerminal(state));
		assertEquals(1, sm.getLegalMoves(state, you).size());
		assertEquals(move("proceed"), sm.getLegalMoves(state, you).get(0));
		state = sm.getNextState(state, Collections.singletonList(move("proceed")));
		assertTrue(sm.isTerminal(state));
		assertEquals(100, sm.getGoal(state, you));
		assertEquals(Collections.singletonList(100), sm.getGoals(state));
	}

	@Test
	public void testCase5B() throws Exception {
		List<Gdl> desc = new TestGameRepository().getGame("test_case_5b").getRules();
		sm.initialize(desc);
		MachineState state = sm.getInitialState();
		Role you = new Role(GdlPool.getConstant("you"));
		assertFalse(sm.isTerminal(state));
		assertEquals(1, sm.getLegalMoves(state, you).size());
		assertEquals(move("draw 1 1 1 2"), sm.getLegalMoves(state, you).get(0));
		state = sm.getNextState(state, Collections.singletonList(move("draw 1 1 1 2")));
		assertTrue(sm.isTerminal(state));
	}

	@Test
	public void testCase5C() throws Exception {
		List<Gdl> desc = new TestGameRepository().getGame("test_case_5c").getRules();
		sm.initialize(desc);
		MachineState state = sm.getInitialState();
		Role you = new Role(GdlPool.getConstant("you"));
		assertFalse(sm.isTerminal(state));
		assertEquals(1, sm.getLegalMoves(state, you).size());
		assertEquals(move("proceed"), sm.getLegalMoves(state, you).get(0));
		state = sm.getNextState(state, Collections.singletonList(move("proceed")));
		assertTrue(sm.isTerminal(state));
		assertEquals(100, sm.getGoal(state, you));
		assertEquals(Collections.singletonList(100), sm.getGoals(state));
	}

	@Test
	public void testCase5D() throws Exception {
		List<Gdl> desc = new TestGameRepository().getGame("test_case_5d").getRules();
		sm.initialize(desc);
		MachineState state = sm.getInitialState();
		Role you = new Role(GdlPool.getConstant("you"));
		assertFalse(sm.isTerminal(state));
		assertEquals(1, sm.getLegalMoves(state, you).size());
		assertEquals(move("proceed"), sm.getLegalMoves(state, you).get(0));
		state = sm.getNextState(state, Collections.singletonList(move("proceed")));
		assertTrue(sm.isTerminal(state));
		assertEquals(100, sm.getGoal(state, you));
		assertEquals(Collections.singletonList(100), sm.getGoals(state));
	}

	@Test
	public void testCase5E() throws Exception {
		List<Gdl> desc = new TestGameRepository().getGame("test_case_5e").getRules();
		sm.initialize(desc);
		MachineState state = sm.getInitialState();
		Role robot = new Role(GdlPool.getConstant("robot"));
		assertFalse(sm.isTerminal(state));
		System.out.println(sm.getLegalMoves(state, robot));
		assertEquals(7, sm.getLegalMoves(state, robot).size());
		assertEquals(ImmutableSet.of(
				move("reduce a 0"),
				move("reduce a 1"),
				move("reduce c 0"),
				move("reduce c 1"),
				move("reduce c 2"),
				move("reduce c 3"),
				move("reduce c 4")),
				ImmutableSet.copyOf(sm.getLegalMoves(state, robot)));
	}

	@Test
	public void testDistinctAtBeginningOfRule() throws Exception {
		List<Gdl> desc = new TestGameRepository().getGame("test_distinct_beginning_rule").getRules();
		sm.initialize(desc);
		MachineState state = sm.getInitialState();
		Role you = new Role(GdlPool.getConstant("you"));
		assertFalse(sm.isTerminal(state));
		assertEquals(2, sm.getLegalMoves(state, you).size());
		state = sm.getNextState(state, Collections.singletonList(move("do a b")));
		assertTrue(sm.isTerminal(state));
		assertEquals(100, sm.getGoal(state, you));
		assertEquals(Collections.singletonList(100), sm.getGoals(state));
	}

	protected Move move(String description) {
		String[] parts = description.split(" ");
		GdlConstant head = GdlPool.getConstant(parts[0]);
		if (parts.length == 1)
			return new Move(head);
		List<GdlTerm> body = new ArrayList<GdlTerm>();
		for (int i = 1; i < parts.length; i++) {
			body.add(GdlPool.getConstant(parts[i]));
		}
		return new Move(GdlPool.getFunction(head, body));
	}
}
