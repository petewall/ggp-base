package edu.umn.kylepete.neuralnetworks;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicTacToeNeuralNetwork2 {

	public static final File TTT_DATA_SET = new File("../ggp-base/archives/tictactoeDataSet2.csv");

	private static Logger logger = LoggerFactory.getLogger(TicTacToeNeuralNetwork2.class);

	// Random number generator seed, for reproducibility
	private static final int seed = 12345;
	// Number of iterations per minibatch
	private static final int iterations = 1;
	// Number of epochs (full passes of the data)
	private static final int nEpochs = 1000;// 2000;
	// Batch size: i.e., each epoch has nSamples/batchSize parameter updates
	private static final int batchSize = 5000;
	// Network learning rate
	private static final double learningRate = 0.1;
	private static final Random rng = new Random(seed);
	private static int listenerFreq = 10;// iterations / 5;

	public static MultiLayerNetwork trainNetwork() throws IOException, InterruptedException {
		return trainNetwork(getTicTacToeDataSet());
	}

	private static MultiLayerNetwork trainNetwork(DataSet data) {
		// Create the network
		MultiLayerNetwork net = new MultiLayerNetwork(getNetworkConfiguration(true));
		net.init();
		net.setListeners(new ScoreIterationListener(listenerFreq));

		logger.info("Train model....");
		for (int i = 0; i < nEpochs; i++) {
			logger.info("Epoch " + i + " of " + nEpochs + " ....");
			// iterator.reset();
			// net.fit(iterator);
			net.fit(data);
		}
		return net;
	}

	private static DataSet getTicTacToeDataSet() throws IOException, InterruptedException {
		DataSetIterator iterator = new TicTacToeDataSetIterator(TTT_DATA_SET, batchSize);
		DataSet next = iterator.next();
		boolean normalize = false;
		if (normalize) {
			// Normalize the full data set. Our DataSet 'next' contains the full 150 examples
			next.normalizeZeroMeanZeroUnitVariance();

			INDArray columnMeans = next.getLabels().mean(0);
			INDArray columnStds = next.getLabels().std(0);
			next.setLabels(next.getLabels().subiRowVector(columnMeans));
			columnStds.addi(Nd4j.scalar(Nd4j.EPS_THRESHOLD));
			next.setLabels(next.getLabels().diviRowVector(columnStds));
			next.shuffle();
		}
		return next;
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		DataSet fullDataSet = getTicTacToeDataSet();

		// split test and train
		// SplitTestAndTrain testAndTrain = fullDataSet.splitTestAndTrain(0.999999);// 0.65); // Use 65% of data for training
		// DataSet trainingData = testAndTrain.getTrain();
		// DataSet testData = testAndTrain.getTest();

		MultiLayerNetwork net = trainNetwork(fullDataSet);

		logger.info("Evaluate model....");
		Evaluation eval = new Evaluation();
		INDArray output = net.output(fullDataSet.getFeatureMatrix());
		INDArray testLabels = fullDataSet.getLabels();
		eval.eval(testLabels, output);
		logger.info(eval.stats());
		int correct = 0;
		for (int i = 0; i < testLabels.rows(); i++) {
			int prediction = getMaxIndex(output.getRow(i));
			int known = getMaxIndex(testLabels.getRow(i));
			if (prediction == known) {
				correct++;
			}
		}
		System.out.println("Percent correct: " + ((double) correct) / testLabels.rows() * 100.0);
		// eval.eval(testData.getLabels(), output);
	}

	private static int getMaxIndex(INDArray array) {
		double max = Double.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int i = 0; i < array.length(); i++) {
			double value = array.getDouble(i);
			if (value > max) {
				max = value;
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	private static MultiLayerConfiguration getNetworkConfiguration(boolean simple) {
		int numInputs = 9;
		int numOutputs = 9;

		int numHiddenNodes = 27;
		return new NeuralNetConfiguration.Builder()
				.seed(seed)
				.iterations(iterations)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.learningRate(learningRate)
				.updater(Updater.NESTEROVS)
				.momentum(0.9)
				.regularization(true)
				.l2(1e-4)
				.list(4)
				.layer(0, new DenseLayer.Builder()
						.nIn(numInputs)
						.nOut(numHiddenNodes)
						.activation("relu")
						.weightInit(WeightInit.XAVIER)
						.build())
				.layer(1, new DenseLayer.Builder()
						.nIn(numHiddenNodes)
						.nOut(numHiddenNodes)
						.activation("relu")
						.weightInit(WeightInit.XAVIER)
						.build())
				.layer(2, new DenseLayer.Builder()
						.nIn(numHiddenNodes)
						.nOut(numHiddenNodes)
						.activation("relu")
						.weightInit(WeightInit.XAVIER)
						.build())
				.layer(3, new OutputLayer.Builder(LossFunction.MCXENT)
						.nIn(numHiddenNodes)
						.nOut(numOutputs)
						.activation("softmax")
						.weightInit(WeightInit.XAVIER)
						.build())
				.pretrain(false)
				.backprop(true)
				.build();

		// return new NeuralNetConfiguration.Builder()
		// .seed(seed)
		// .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
		// .gradientNormalizationThreshold(1.0)
		// .iterations(iterations)
		// .momentum(0.5)
		// .momentumAfter(Collections.singletonMap(3, 0.9))
		// .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT)
		// .list(4)
		// .layer(0, new RBM.Builder().nIn(numInputs).nOut(500)
		// .weightInit(WeightInit.XAVIER).lossFunction(LossFunction.RMSE_XENT)
		// .visibleUnit(RBM.VisibleUnit.BINARY)
		// .hiddenUnit(RBM.HiddenUnit.BINARY)
		// .build())
		// .layer(1, new RBM.Builder().nIn(500).nOut(250)
		// .weightInit(WeightInit.XAVIER).lossFunction(LossFunction.RMSE_XENT)
		// .visibleUnit(RBM.VisibleUnit.BINARY)
		// .hiddenUnit(RBM.HiddenUnit.BINARY)
		// .build())
		// .layer(2, new RBM.Builder().nIn(250).nOut(200)
		// .weightInit(WeightInit.XAVIER).lossFunction(LossFunction.RMSE_XENT)
		// .visibleUnit(RBM.VisibleUnit.BINARY)
		// .hiddenUnit(RBM.HiddenUnit.BINARY)
		// .build())
		// .layer(3, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD).activation("softmax")
		// .nIn(200).nOut(numOutputs).build())
		// .pretrain(true).backprop(false)
		// .build();
	}

}
