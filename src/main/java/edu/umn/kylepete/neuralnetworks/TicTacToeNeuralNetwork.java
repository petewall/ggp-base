package edu.umn.kylepete.neuralnetworks;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.canova.api.records.reader.RecordReader;
import org.canova.api.records.reader.impl.CSVRecordReader;
import org.canova.api.split.FileSplit;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
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

public class TicTacToeNeuralNetwork {

	public static final File TTT_DATA_SET = new File("../ggp-base/archives/tictactoeDataSet.csv");

	private static Logger logger = LoggerFactory.getLogger(TicTacToeNeuralNetwork.class);

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
		// First: get the dataset using the record reader. CSVRecordReader handles loading/parsing
		int numLinesToSkip = 0;
		String delimiter = ",";
		RecordReader recordReader = new CSVRecordReader(numLinesToSkip, delimiter);
		recordReader.initialize(new FileSplit(TTT_DATA_SET));

		// Second: the RecordReaderDataSetIterator handles conversion to DataSet objects, ready for use in neural network
		int labelIndex = 9;
		int numClasses = 1;
		DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader, null, batchSize, labelIndex, numClasses, true);
		DataSet next = iterator.next();
		boolean normalize = true;
		if (normalize) {
			// Normalize the full data set. Our DataSet 'next' contains the full 150 examples
			// next.normalizeZeroMeanZeroUnitVariance();

			INDArray columnMeans = next.getLabels().mean(0);
			INDArray columnStds = next.getLabels().std(0);
			next.setLabels(next.getLabels().subiRowVector(columnMeans));
			columnStds.addi(Nd4j.scalar(Nd4j.EPS_THRESHOLD));
			next.setLabels(next.getLabels().diviRowVector(columnStds));
			// next.shuffle();
		}
		return next;
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		DataSet fullDataSet = getTicTacToeDataSet();

		// split test and train
		// SplitTestAndTrain testAndTrain = fullDataSet.splitTestAndTrain(0.999999);// 0.65); // Use 65% of data for training
		// DataSet trainingData = testAndTrain.getTrain();
		// DataSet testData = testAndTrain.getTest();
		DataSet trainingData = fullDataSet;

		MultiLayerNetwork net = trainNetwork(trainingData);

		logger.info("Evaluate model....");
		// Evaluation eval = new Evaluation(1);
		INDArray output = net.output(trainingData.getFeatureMatrix());
		INDArray testLabels = trainingData.getLabels();
		for (int i = 0; i < 20; i++) {
			System.out.println(testLabels.getDouble(i) + " -- " + output.getDouble(i));
		}
		// eval.eval(testData.getLabels(), output);
		// logger.info(eval.stats());
	}

	private static MultiLayerConfiguration getNetworkConfiguration(boolean simple) {
		int numInputs = 9;
		int numOutputs = 1;

		int numHiddenNodes = 200;
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
//				.layer(3, new DenseLayer.Builder()
//						.nIn(numHiddenNodes)
//						.nOut(numHiddenNodes)
//						.activation("relu")
//						.weightInit(WeightInit.XAVIER)
//						.build())
//				.layer(4, new DenseLayer.Builder()
//						.nIn(numHiddenNodes)
//						.nOut(numHiddenNodes)
//						.activation("relu")
//						.weightInit(WeightInit.XAVIER)
//						.build())
				.layer(3, new OutputLayer.Builder(LossFunction.MSE)
						.nIn(numHiddenNodes)
						.nOut(numOutputs)
						.activation("identity")
						.weightInit(WeightInit.XAVIER)
						.build())
				.pretrain(false)
				.backprop(true)
				.build();
	}

}
