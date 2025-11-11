package com.therighthandapp.autobads.financial.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * LSTM Model for Financial Forecasting
 * 
 * Architecture:
 * - Input layer: 6 features (revenue, costs, profit, market growth, competitors, CAC)
 * - LSTM layer 1: 128 units
 * - LSTM layer 2: 64 units
 * - Dense output: 1 unit (predicted value)
 * 
 * Training:
 * - Optimizer: Adam (lr=0.001)
 * - Loss: MSE
 * - Lookback window: 12 months
 * - Forecast horizon: 60 months (5 years)
 */
@Slf4j
@Component
@Data
public class LstmFinancialModel {

    private static final int INPUT_SIZE = 6;  // 6 features
    private static final int LSTM_LAYER_1_SIZE = 128;
    private static final int LSTM_LAYER_2_SIZE = 64;
    private static final int OUTPUT_SIZE = 1;
    private static final int LOOKBACK_WINDOW = 12; // 12 months
    private static final double LEARNING_RATE = 0.001;
    
    private MultiLayerNetwork model;
    private boolean isTrained = false;
    private double lastTrainingLoss = 0.0;
    
    /**
     * Build LSTM architecture
     */
    public void buildModel() {
        log.info("Building LSTM model architecture");
        
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(42)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(LEARNING_RATE))
            .list()
            // Layer 0: First LSTM layer
            .layer(0, new LSTM.Builder()
                .nIn(INPUT_SIZE)
                .nOut(LSTM_LAYER_1_SIZE)
                .activation(Activation.TANH)
                .build())
            // Layer 1: Second LSTM layer
            .layer(1, new LSTM.Builder()
                .nIn(LSTM_LAYER_1_SIZE)
                .nOut(LSTM_LAYER_2_SIZE)
                .activation(Activation.TANH)
                .build())
            // Layer 2: Output layer
            .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(LSTM_LAYER_2_SIZE)
                .nOut(OUTPUT_SIZE)
                .activation(Activation.IDENTITY)
                .build())
            .build();
        
        model = new MultiLayerNetwork(conf);
        model.init();
        
        log.info("LSTM model built successfully. Total params: {}", model.numParams());
    }
    
    /**
     * Train model on historical financial data
     */
    public void train(List<FinancialTimeSeriesData> trainingData, int epochs) {
        if (model == null) {
            buildModel();
        }
        
        log.info("Training LSTM model on {} samples for {} epochs", trainingData.size(), epochs);
        
        // Prepare training datasets
        List<DataSet> datasets = prepareTrainingData(trainingData);
        
        if (datasets.isEmpty()) {
            log.warn("No valid training data available");
            return;
        }
        
        // Training loop
        for (int epoch = 0; epoch < epochs; epoch++) {
            double epochLoss = 0.0;
            
            for (DataSet dataset : datasets) {
                model.fit(dataset);
                epochLoss += model.score();
            }
            
            lastTrainingLoss = epochLoss / datasets.size();
            
            if (epoch % 10 == 0) {
                log.info("Epoch {}/{} - Loss: {}", epoch, epochs, lastTrainingLoss);
            }
        }
        
        isTrained = true;
        log.info("Training completed. Final loss: {}", lastTrainingLoss);
    }
    
    /**
     * Prepare training data from historical records
     */
    private List<DataSet> prepareTrainingData(List<FinancialTimeSeriesData> trainingData) {
        List<DataSet> datasets = new ArrayList<>();
        
        for (FinancialTimeSeriesData data : trainingData) {
            if (!data.isValid() || data.getSequenceLength() < LOOKBACK_WINDOW + 1) {
                log.warn("Skipping invalid/short data sequence");
                continue;
            }
            
            // Create sliding windows
            int sequenceLength = data.getSequenceLength();
            for (int i = 0; i < sequenceLength - LOOKBACK_WINDOW; i++) {
                INDArray input = createInputSequence(data, i, LOOKBACK_WINDOW);
                INDArray output = createOutputSequence(data, i + LOOKBACK_WINDOW);
                
                datasets.add(new DataSet(input, output));
            }
        }
        
        return datasets;
    }
    
    /**
     * Create input sequence [lookback, features]
     */
    private INDArray createInputSequence(FinancialTimeSeriesData data, int startIdx, int length) {
        // Shape: [1, INPUT_SIZE, length]
        INDArray input = Nd4j.create(1, INPUT_SIZE, length);
        
        for (int t = 0; t < length; t++) {
            int idx = startIdx + t;
            
            // Normalize features
            input.putScalar(new int[]{0, 0, t}, normalize(data.getRevenue().get(idx)));
            input.putScalar(new int[]{0, 1, t}, normalize(data.getCosts().get(idx)));
            input.putScalar(new int[]{0, 2, t}, normalize(data.getProfit().get(idx)));
            input.putScalar(new int[]{0, 3, t}, normalize(data.getMarketSizeGrowth().get(idx)));
            input.putScalar(new int[]{0, 4, t}, data.getCompetitorCount().get(idx) / 100.0);
            input.putScalar(new int[]{0, 5, t}, data.getCustomerAcquisitionCost().get(idx) / 10000.0);
        }
        
        return input;
    }
    
    /**
     * Create output (target) value
     */
    private INDArray createOutputSequence(FinancialTimeSeriesData data, int idx) {
        // Shape: [1, OUTPUT_SIZE, 1]
        INDArray output = Nd4j.create(1, OUTPUT_SIZE, 1);
        output.putScalar(new int[]{0, 0, 0}, normalize(data.getRevenue().get(idx)));
        return output;
    }
    
    /**
     * Predict future revenue trajectory
     */
    public List<BigDecimal> predict(
            List<BigDecimal> historicalRevenue,
            List<BigDecimal> historicalCosts,
            List<BigDecimal> historicalProfit,
            int forecastMonths) {
        
        if (!isTrained && model == null) {
            log.warn("Model not trained, building with random weights");
            buildModel();
        }
        
        log.info("Predicting {} months of revenue", forecastMonths);
        
        List<BigDecimal> predictions = new ArrayList<>();
        
        // Use last LOOKBACK_WINDOW months as initial input
        int historySize = Math.min(historicalRevenue.size(), LOOKBACK_WINDOW);
        List<BigDecimal> recentRevenue = new ArrayList<>(
            historicalRevenue.subList(historicalRevenue.size() - historySize, historicalRevenue.size())
        );
        List<BigDecimal> recentCosts = new ArrayList<>(
            historicalCosts.subList(historicalCosts.size() - historySize, historicalCosts.size())
        );
        List<BigDecimal> recentProfit = new ArrayList<>(
            historicalProfit.subList(historicalProfit.size() - historySize, historicalProfit.size())
        );
        
        // Iteratively predict
        for (int month = 0; month < forecastMonths; month++) {
            INDArray input = createPredictionInput(recentRevenue, recentCosts, recentProfit);
            INDArray output = model.output(input);
            
            // Denormalize prediction
            double predictedValue = denormalize(output.getDouble(0, 0, 0));
            BigDecimal prediction = BigDecimal.valueOf(Math.max(0, predictedValue));
            predictions.add(prediction);
            
            // Update sliding window (append prediction, remove oldest)
            recentRevenue.add(prediction);
            if (recentRevenue.size() > LOOKBACK_WINDOW) {
                recentRevenue.remove(0);
            }
            
            // Estimate future costs and profit
            BigDecimal estimatedCost = prediction.multiply(BigDecimal.valueOf(0.6)); // 60% cost ratio
            BigDecimal estimatedProfit = prediction.subtract(estimatedCost);
            
            recentCosts.add(estimatedCost);
            recentProfit.add(estimatedProfit);
            if (recentCosts.size() > LOOKBACK_WINDOW) {
                recentCosts.remove(0);
                recentProfit.remove(0);
            }
        }
        
        return predictions;
    }
    
    /**
     * Create input for prediction
     */
    private INDArray createPredictionInput(
            List<BigDecimal> revenue,
            List<BigDecimal> costs,
            List<BigDecimal> profit) {
        
        int length = Math.min(revenue.size(), LOOKBACK_WINDOW);
        INDArray input = Nd4j.create(1, INPUT_SIZE, length);
        
        for (int t = 0; t < length; t++) {
            input.putScalar(new int[]{0, 0, t}, normalize(revenue.get(t)));
            input.putScalar(new int[]{0, 1, t}, normalize(costs.get(t)));
            input.putScalar(new int[]{0, 2, t}, normalize(profit.get(t)));
            input.putScalar(new int[]{0, 3, t}, 0.05); // Default market growth
            input.putScalar(new int[]{0, 4, t}, 0.1);  // Default competitor count
            input.putScalar(new int[]{0, 5, t}, 0.05); // Default CAC
        }
        
        return input;
    }
    
    /**
     * Normalize BigDecimal to [0, 1] range
     */
    private double normalize(BigDecimal value) {
        // Log normalization for financial data (handles wide range)
        double v = value.doubleValue();
        return Math.log1p(Math.max(0, v)) / 20.0; // Scale factor 20
    }
    
    /**
     * Denormalize prediction back to original scale
     */
    private double denormalize(double normalized) {
        return Math.expm1(normalized * 20.0);
    }
    
    /**
     * Save model to disk
     */
    public void saveModel(String modelPath) throws IOException {
        if (model == null) {
            throw new IllegalStateException("No model to save");
        }
        
        File modelFile = new File(modelPath);
        modelFile.getParentFile().mkdirs();
        model.save(modelFile, true);
        log.info("Model saved to {}", modelPath);
    }
    
    /**
     * Load model from disk
     */
    public void loadModel(String modelPath) throws IOException {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new IllegalArgumentException("Model file not found: " + modelPath);
        }
        
        model = MultiLayerNetwork.load(modelFile, true);
        isTrained = true;
        log.info("Model loaded from {}", modelPath);
    }
    
    /**
     * Get model evaluation metrics
     */
    public ModelMetrics evaluate(List<FinancialTimeSeriesData> testData) {
        log.info("Evaluating model on {} test samples", testData.size());
        
        List<DataSet> datasets = prepareTrainingData(testData);
        
        if (datasets.isEmpty()) {
            return ModelMetrics.builder()
                .mse(Double.NaN)
                .rmse(Double.NaN)
                .mae(Double.NaN)
                .build();
        }
        
        double totalMse = 0.0;
        double totalMae = 0.0;
        int count = 0;
        
        for (DataSet dataset : datasets) {
            INDArray predicted = model.output(dataset.getFeatures());
            INDArray actual = dataset.getLabels();
            
            // Calculate MSE and MAE
            INDArray diff = predicted.sub(actual);
            double mse = diff.mul(diff).meanNumber().doubleValue();
            INDArray absDiff = Nd4j.getExecutioner().exec(
                new org.nd4j.linalg.api.ops.impl.transforms.same.Abs(diff.dup())
            );
            double mae = absDiff.meanNumber().doubleValue();
            
            totalMse += mse;
            totalMae += mae;
            count++;
        }
        
        double avgMse = totalMse / count;
        double avgRmse = Math.sqrt(avgMse);
        double avgMae = totalMae / count;
        
        return ModelMetrics.builder()
            .mse(avgMse)
            .rmse(avgRmse)
            .mae(avgMae)
            .sampleCount(count)
            .build();
    }
    
    @Data
    @lombok.Builder
    public static class ModelMetrics {
        private double mse;   // Mean Squared Error
        private double rmse;  // Root Mean Squared Error
        private double mae;   // Mean Absolute Error
        private int sampleCount;
    }
}
