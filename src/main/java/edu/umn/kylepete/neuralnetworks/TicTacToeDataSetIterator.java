/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package edu.umn.kylepete.neuralnetworks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.canova.api.records.reader.RecordReader;
import org.canova.api.records.reader.SequenceRecordReader;
import org.canova.api.records.reader.impl.CSVRecordReader;
import org.canova.api.split.FileSplit;
import org.canova.api.writable.Writable;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Record reader dataset iterator
 *
 * @author Adam Gibson
 */
public class TicTacToeDataSetIterator implements DataSetIterator {

	private static final long serialVersionUID = -7391574322022204456L;
	private RecordReader recordReader;
	private int batchSize = 10;
	private int labelIndex = -1;
	private int numPossibleLabels = -1;
	private boolean overshot = false;
	private Iterator<Collection<Writable>> sequenceIter;
	private DataSet last;
	private boolean useCurrent = false;
	private DataSetPreProcessor preProcessor;

	public TicTacToeDataSetIterator(File tttCSVdata, int batchSize) throws IOException, InterruptedException {
		this(new CSVRecordReader(0, ","), batchSize, 9, 9);
		recordReader.initialize(new FileSplit(tttCSVdata));
	}

	/**
	 * Use the record reader and batch size; no labels
	 * 
	 * @param recordReader
	 *            the record reader to use
	 * @param batchSize
	 *            the batch size of the data
	 */
	public TicTacToeDataSetIterator(RecordReader recordReader, int batchSize) {
		this(recordReader, batchSize, -1, -1);
	}

	/**
	 * Invoke the recordreaderdatasetiterator with a batch size of 10
	 * 
	 * @param recordReader
	 *            the recordreader to use
	 * @param labelIndex
	 *            the index of the label
	 * @param numPossibleLabels
	 *            the number of possible labels for classification
	 *
	 */
	public TicTacToeDataSetIterator(RecordReader recordReader, int labelIndex, int numPossibleLabels) {
		this(recordReader, 10, labelIndex, numPossibleLabels);
	}

	/**
	 * Main constructor
	 * 
	 * @param recordReader
	 *            the recorder to use for the dataset
	 * @param batchSize
	 *            the batch size
	 * @param labelIndex
	 *            the index of the label to use
	 * @param numPossibleLabels
	 *            the number of posible
	 */
	public TicTacToeDataSetIterator(RecordReader recordReader, int batchSize, int labelIndex, int numPossibleLabels) {
		this.recordReader = recordReader;
		this.batchSize = batchSize;
		this.labelIndex = labelIndex;
		this.numPossibleLabels = numPossibleLabels;
	}

	public TicTacToeDataSetIterator(RecordReader recordReader) {
		this(recordReader, 10, -1, -1);
	}

	@Override
	public DataSet next(int num) {
		if (useCurrent) {
			useCurrent = false;
			if (preProcessor != null)
				preProcessor.preProcess(last);
			return last;
		}

		List<DataSet> dataSets = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			if (!hasNext())
				break;
			if (recordReader instanceof SequenceRecordReader) {
				if (sequenceIter == null || !sequenceIter.hasNext()) {
					Collection<Collection<Writable>> sequenceRecord = ((SequenceRecordReader) recordReader).sequenceRecord();
					sequenceIter = sequenceRecord.iterator();
				}
				Collection<Writable> record = sequenceIter.next();
				dataSets.add(getDataSet(record));
			}

			else {
				Collection<Writable> record = recordReader.next();
				dataSets.add(getDataSet(record));
			}
		}
		List<INDArray> inputs = new ArrayList<>();
		List<INDArray> labels = new ArrayList<>();

		for (DataSet data : dataSets) {
			inputs.add(data.getFeatureMatrix());
			labels.add(data.getLabels());
		}

		if (inputs.isEmpty()) {
			overshot = true;
			return last;
		}

		DataSet ret = new DataSet(Nd4j.vstack(inputs.toArray(new INDArray[0])), Nd4j.vstack(labels.toArray(new INDArray[0])));
		last = ret;
		if (preProcessor != null)
			preProcessor.preProcess(ret);
		return ret;
	}

	private DataSet getDataSet(Collection<Writable> record) {
		List<Writable> currList;
		if (record instanceof List)
			currList = (List<Writable>) record;
		else
			currList = new ArrayList<>(record);

		// allow people to specify label index as -1 and infer the last possible label
		if (numPossibleLabels >= 1 && labelIndex < 0) {
			labelIndex = record.size() - 1;
		}

		INDArray labelVector = Nd4j.create(numPossibleLabels);
		INDArray featureVector = Nd4j.create(labelIndex >= 0 ? currList.size() - numPossibleLabels : currList.size());
		int featureCount = 0;
		int labelCount = 0;
		for (int j = 0; j < currList.size(); j++) {
			Writable current = currList.get(j);
			if (current.toString().isEmpty())
				continue;
			if (labelIndex >= 0 && j >= labelIndex) {
				if (numPossibleLabels < 1)
					throw new IllegalStateException("Number of possible labels invalid, must be >= 1");
				labelVector.putScalar(labelCount++, current.toDouble());
			} else {
				featureVector.putScalar(featureCount++, current.toDouble());
			}
		}

		double max = Double.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int i = 0; i < labelVector.length(); i++) {
			double value = labelVector.getDouble(i);
			if (value > max) {
				max = value;
				maxIndex = i;
			}
			labelVector.putScalar(i, 0);
		}
		labelVector.putScalar(maxIndex, 1);
		return new DataSet(featureVector, labelIndex >= 0 ? labelVector : featureVector);
	}

	@Override
	public int totalExamples() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int inputColumns() {
		if (last == null) {
			DataSet next = next();
			last = next;
			useCurrent = true;
			return next.numInputs();
		} else
			return last.numInputs();

	}

	@Override
	public int totalOutcomes() {
		if (last == null) {
			DataSet next = next();
			last = next;
			useCurrent = true;
			return next.numOutcomes();
		} else
			return last.numOutcomes();

	}

	@Override
	public void reset() {
		if (recordReader instanceof RecordReader)
			recordReader.reset();
		else if (recordReader instanceof SequenceRecordReader)
			throw new UnsupportedOperationException("Reset not supported for SequenceRecordReader type.");
	}

	@Override
	public int batch() {
		return batchSize;
	}

	@Override
	public int cursor() {
		throw new UnsupportedOperationException();

	}

	@Override
	public int numExamples() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void setPreProcessor(org.nd4j.linalg.dataset.api.DataSetPreProcessor preProcessor) {
		this.preProcessor = preProcessor;
	}

	@Override
	public boolean hasNext() {
		return recordReader.hasNext() || overshot;
	}

	@Override
	public DataSet next() {
		return next(batchSize);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getLabels() {
		return recordReader.getLabels();
	}
}
