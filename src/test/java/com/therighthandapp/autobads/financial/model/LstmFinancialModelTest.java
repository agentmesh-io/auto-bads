package com.therighthandapp.autobads.financial.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LstmFinancialModel.
 * Tests LSTM model building and basic functionality.
 * Note: Full training tests are skipped due to complexity - tested via integration tests.
 */
class LstmFinancialModelTest {

    private LstmFinancialModel model;

    @BeforeEach
    void setUp() {
        model = new LstmFinancialModel();
    }

    @Test
    void testBuildModel() {
        // When: Build model architecture
        model.buildModel();

        // Then: Model should be initialized
        assertThat(model.getModel()).isNotNull();
        assertThat(model.isTrained()).isFalse();
    }

    @Test
    void testModelArchitecture() {
        // When: Build model
        model.buildModel();

        // Then: Model should have correct number of layers (2 LSTM + 1 output)
        assertThat(model.getModel().getLayers()).hasSize(3);
    }

    @Test
    void testModelNotTrainedInitially() {
        // When: Create new model
        model.buildModel();

        // Then: Should not be trained and loss should be 0
        assertThat(model.isTrained()).isFalse();
        assertThat(model.getLastTrainingLoss()).isEqualTo(0.0);
    }

    @Test
    void testSaveAndLoadModel() throws Exception {
        // Given: Built model
        model.buildModel();
        String tempModelPath = "/tmp/test-lstm-model.zip";

        // When: Save model
        model.saveModel(tempModelPath);

        // Then: Model file should exist
        assertThat(new java.io.File(tempModelPath)).exists();

        // When: Load model in new instance
        LstmFinancialModel loadedModel = new LstmFinancialModel();
        loadedModel.loadModel(tempModelPath);

        // Then: Loaded model should have model network
        assertThat(loadedModel.getModel()).isNotNull();
        assertThat(loadedModel.getModel().getLayers()).hasSize(3);

        // Cleanup
        new java.io.File(tempModelPath).delete();
    }

    @Test
    void testPredictWithUntrainedModel() {
        // Given: Untrained model
        model.buildModel();
        List<BigDecimal> revenue = generateSampleData(12);
        List<BigDecimal> costs = generateSampleData(12);
        List<BigDecimal> profit = generateSampleData(12);

        // When: Make predictions (should work but with random weights)
        List<BigDecimal> predictions = model.predict(revenue, costs, profit, 12);

        // Then: Should return predictions
        assertThat(predictions).hasSize(12);
        // All predictions should be non-null
        predictions.forEach(prediction -> assertThat(prediction).isNotNull());
    }

    @Test
    void testPredictDifferentHorizons() {
        // Given: Built model
        model.buildModel();
        List<BigDecimal> revenue = generateSampleData(12);
        List<BigDecimal> costs = generateSampleData(12);
        List<BigDecimal> profit = generateSampleData(12);

        // When: Predict different horizons
        List<BigDecimal> predictions6 = model.predict(revenue, costs, profit, 6);
        List<BigDecimal> predictions12 = model.predict(revenue, costs, profit, 12);
        List<BigDecimal> predictions24 = model.predict(revenue, costs, profit, 24);

        // Then: Should return correct sizes
        assertThat(predictions6).hasSize(6);
        assertThat(predictions12).hasSize(12);
        assertThat(predictions24).hasSize(24);
    }

    @Test
    void testPredictionConsistency() {
        // Given: Built model
        model.buildModel();
        List<BigDecimal> revenue = generateSampleData(12);
        List<BigDecimal> costs = generateSampleData(12);
        List<BigDecimal> profit = generateSampleData(12);

        // When: Make same prediction twice
        List<BigDecimal> predictions1 = model.predict(revenue, costs, profit, 12);
        List<BigDecimal> predictions2 = model.predict(revenue, costs, profit, 12);

        // Then: Predictions should be identical (deterministic)
        assertThat(predictions1).hasSize(predictions2.size());
        for (int i = 0; i < predictions1.size(); i++) {
            assertThat(predictions1.get(i)).isEqualByComparingTo(predictions2.get(i));
        }
    }

    // Helper method to generate sample data
    private List<BigDecimal> generateSampleData(int size) {
        List<BigDecimal> data = new ArrayList<>();
        double baseValue = 10000.0;
        for (int i = 0; i < size; i++) {
            double value = baseValue * (1.0 + i * 0.05); // 5% growth per month
            data.add(BigDecimal.valueOf(value));
        }
        return data;
    }
}
