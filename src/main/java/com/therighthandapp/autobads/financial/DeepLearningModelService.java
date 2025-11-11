package com.therighthandapp.autobads.financial;

import com.therighthandapp.autobads.financial.model.FinancialTimeSeriesData;
import com.therighthandapp.autobads.financial.model.LstmFinancialModel;
import com.therighthandapp.autobads.financial.model.SyntheticDataGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Deep Learning Model Service - LSTM-based financial forecasting
 * 
 * Features:
 * - Automatic model training on startup (if model not found)
 * - Pretrained model loading
 * - Revenue and profit trajectory prediction
 * - Model evaluation metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeepLearningModelService {

    private final LstmFinancialModel lstmModel;
    private final SyntheticDataGenerator dataGenerator;
    
    @Value("${auto-bads.ml.model-path:models/lstm-financial-forecasting.zip}")
    private String modelPath;
    
    @Value("${auto-bads.ml.auto-train:true}")
    private boolean autoTrain;
    
    @Value("${auto-bads.ml.training-samples:200}")
    private int trainingSamples;
    
    @Value("${auto-bads.ml.training-epochs:50}")
    private int trainingEpochs;
    
    /**
     * Initialize model on startup
     */
    @PostConstruct
    public void initialize() {
        File modelFile = new File(modelPath);
        
        if (modelFile.exists()) {
            log.info("Loading pretrained LSTM model from {}", modelPath);
            try {
                lstmModel.loadModel(modelPath);
                log.info("Model loaded successfully");
            } catch (Exception e) {
                log.error("Failed to load model, will train new one", e);
                trainModel();
            }
        } else if (autoTrain) {
            log.info("No pretrained model found, training new model");
            trainModel();
        } else {
            log.warn("No model available and auto-train disabled, predictions will use untrained model");
            lstmModel.buildModel();
        }
    }
    
    /**
     * Train LSTM model on synthetic data
     */
    public void trainModel() {
        log.info("Training LSTM model with {} samples, {} epochs", trainingSamples, trainingEpochs);
        
        // Generate training and validation data
        List<FinancialTimeSeriesData> allData = dataGenerator.generateTrainingData(trainingSamples);
        
        int trainSize = (int) (allData.size() * 0.8);
        List<FinancialTimeSeriesData> trainData = allData.subList(0, trainSize);
        List<FinancialTimeSeriesData> valData = allData.subList(trainSize, allData.size());
        
        // Train model
        lstmModel.train(trainData, trainingEpochs);
        
        // Evaluate on validation set
        LstmFinancialModel.ModelMetrics metrics = lstmModel.evaluate(valData);
        log.info("Validation metrics - MSE: {}, RMSE: {}, MAE: {}", 
            metrics.getMse(), metrics.getRmse(), metrics.getMae());
        
        // Save trained model
        try {
            lstmModel.saveModel(modelPath);
            log.info("Trained model saved to {}", modelPath);
        } catch (Exception e) {
            log.error("Failed to save model", e);
        }
    }
    
    /**
     * Predict revenue trajectory using LSTM
     */
    public List<BigDecimal> predictRevenue(AnalysisContext context) {
        log.info("DL Model predicting 5-year revenue trajectory");
        
        // Generate baseline historical data based on context
        List<BigDecimal> historicalRevenue = generateHistoricalRevenue(context);
        List<BigDecimal> historicalCosts = generateHistoricalCosts(historicalRevenue);
        List<BigDecimal> historicalProfit = calculateProfit(historicalRevenue, historicalCosts);
        
        // Predict next 60 months (5 years)
        List<BigDecimal> monthlyPredictions = lstmModel.predict(
            historicalRevenue, 
            historicalCosts, 
            historicalProfit, 
            60
        );
        
        // Aggregate to yearly predictions
        return aggregateToYearly(monthlyPredictions);
    }

    /**
     * Predict profit based on revenue and TCO
     */
    public List<BigDecimal> predictProfit(
            List<BigDecimal> revenue,
            com.therighthandapp.autobads.core.domain.FinancialAnalysisResult.TotalCostOfOwnership tco) {

        log.info("DL Model predicting profit margins");

        List<BigDecimal> profits = new ArrayList<>();
        BigDecimal annualCost = tco.getYearlyOperationalCost()
                .add(tco.getMaintenanceCost())
                .add(tco.getThirdPartyLicensing())
                .add(tco.getInternalResourceCost());

        for (BigDecimal yearRevenue : revenue) {
            // Profit = Revenue - Costs, with improving margins over time
            BigDecimal profit = yearRevenue.subtract(annualCost);
            profits.add(profit);
            // Costs decrease slightly each year due to efficiency (5% annual reduction)
            annualCost = annualCost.multiply(BigDecimal.valueOf(0.95));
        }

        return profits;
    }
    
    /**
     * Generate baseline historical revenue (12 months)
     */
    private List<BigDecimal> generateHistoricalRevenue(AnalysisContext context) {
        List<BigDecimal> history = new ArrayList<>();
        
        // Estimate initial monthly revenue based on problem domain
        double baseMonthlyRevenue = estimateInitialRevenue(context);
        double growthRate = 0.10; // 10% monthly growth assumption
        
        for (int month = 0; month < 12; month++) {
            double revenue = baseMonthlyRevenue * Math.pow(1 + growthRate, month);
            history.add(BigDecimal.valueOf(revenue));
        }
        
        return history;
    }
    
    /**
     * Estimate initial revenue based on context
     */
    private double estimateInitialRevenue(AnalysisContext context) {
        // Simple heuristic based on industry patterns
        // In real scenario, this could use more context or user input
        return 20000.0; // $20K/month baseline
    }
    
    /**
     * Generate historical costs (typically 60-70% of revenue)
     */
    private List<BigDecimal> generateHistoricalCosts(List<BigDecimal> revenue) {
        return revenue.stream()
            .map(r -> r.multiply(BigDecimal.valueOf(0.65))) // 65% cost ratio
            .toList();
    }
    
    /**
     * Calculate profit
     */
    private List<BigDecimal> calculateProfit(List<BigDecimal> revenue, List<BigDecimal> costs) {
        List<BigDecimal> profit = new ArrayList<>();
        for (int i = 0; i < revenue.size(); i++) {
            profit.add(revenue.get(i).subtract(costs.get(i)));
        }
        return profit;
    }
    
    /**
     * Aggregate monthly predictions to yearly
     */
    private List<BigDecimal> aggregateToYearly(List<BigDecimal> monthlyData) {
        List<BigDecimal> yearly = new ArrayList<>();
        
        for (int year = 0; year < 5; year++) {
            BigDecimal yearTotal = BigDecimal.ZERO;
            for (int month = 0; month < 12; month++) {
                int idx = year * 12 + month;
                if (idx < monthlyData.size()) {
                    yearTotal = yearTotal.add(monthlyData.get(idx));
                }
            }
            yearly.add(yearTotal);
        }
        
        return yearly;
    }
    
    /**
     * Get model training status
     */
    public boolean isModelTrained() {
        return lstmModel.isTrained();
    }
    
    /**
     * Get last training loss
     */
    public double getLastTrainingLoss() {
        return lstmModel.getLastTrainingLoss();
    }
}
