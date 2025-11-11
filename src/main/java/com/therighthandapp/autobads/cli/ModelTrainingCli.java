package com.therighthandapp.autobads.cli;

import com.therighthandapp.autobads.financial.model.FinancialTimeSeriesData;
import com.therighthandapp.autobads.financial.model.LstmFinancialModel;
import com.therighthandapp.autobads.financial.model.SyntheticDataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CLI tool for training LSTM model manually
 * 
 * Usage:
 * mvn spring-boot:run -Dspring-boot.run.arguments="--train-model=true --samples=500 --epochs=100"
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "train-model", havingValue = "true")
public class ModelTrainingCli implements CommandLineRunner {

    private final LstmFinancialModel lstmModel;
    private final SyntheticDataGenerator dataGenerator;
    
    public ModelTrainingCli(LstmFinancialModel lstmModel, SyntheticDataGenerator dataGenerator) {
        this.lstmModel = lstmModel;
        this.dataGenerator = dataGenerator;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=== LSTM Model Training CLI ===");
        
        // Parse arguments
        int samples = getArgument(args, "--samples", 500);
        int epochs = getArgument(args, "--epochs", 100);
        String modelPath = getArgument(args, "--output", "models/lstm-financial-forecasting.zip");
        
        log.info("Training configuration:");
        log.info("  Samples: {}", samples);
        log.info("  Epochs: {}", epochs);
        log.info("  Output: {}", modelPath);
        
        // Generate data
        log.info("Generating {} training samples...", samples);
        List<FinancialTimeSeriesData> allData = dataGenerator.generateTrainingData(samples);
        
        int trainSize = (int) (allData.size() * 0.8);
        int valSize = allData.size() - trainSize;
        
        List<FinancialTimeSeriesData> trainData = allData.subList(0, trainSize);
        List<FinancialTimeSeriesData> valData = allData.subList(trainSize, allData.size());
        
        log.info("Split: {} training, {} validation", trainSize, valSize);
        
        // Train
        log.info("Training LSTM model...");
        long startTime = System.currentTimeMillis();
        
        lstmModel.train(trainData, epochs);
        
        long trainTime = System.currentTimeMillis() - startTime;
        log.info("Training completed in {} seconds", trainTime / 1000.0);
        
        // Evaluate
        log.info("Evaluating on validation set...");
        LstmFinancialModel.ModelMetrics metrics = lstmModel.evaluate(valData);
        
        log.info("Validation Metrics:");
        log.info("  MSE:  {}", metrics.getMse());
        log.info("  RMSE: {}", metrics.getRmse());
        log.info("  MAE:  {}", metrics.getMae());
        log.info("  Samples: {}", metrics.getSampleCount());
        
        // Save
        log.info("Saving model to {}...", modelPath);
        lstmModel.saveModel(modelPath);
        
        log.info("=== Training Complete ===");
        log.info("Model ready for deployment");
        
        // Exit application
        System.exit(0);
    }
    
    private int getArgument(String[] args, String name, int defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(name + "=")) {
                return Integer.parseInt(arg.substring(name.length() + 1));
            }
        }
        return defaultValue;
    }
    
    private String getArgument(String[] args, String name, String defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(name + "=")) {
                return arg.substring(name.length() + 1);
            }
        }
        return defaultValue;
    }
}
