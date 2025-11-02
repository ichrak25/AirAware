package tn.cot.airaware.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MLPrediction implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private ObjectId id;
    private String deviceId;
    private LocalDateTime predictionTimestamp;
    private LocalDateTime targetTimestamp; // When the prediction is for
    
    // Model information
    private String modelName;
    private String modelVersion;
    private String algorithm; // ARIMA, LSTM, RandomForest
    
    // Prediction results
    private Map<String, PredictedValue> predictions;
    
    // Overall air quality prediction
    private Integer predictedAQI;
    private String predictedAQICategory;
    private Double overallConfidence;
    
    // Risk assessment
    private RiskLevel riskLevel;
    private String riskDescription;
    
    // Model performance metrics
    private ModelMetrics metrics;
    
    // Actual vs Predicted (for validation)
    private Boolean validated;
    private Map<String, Double> actualValues;
    private Double predictionError;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictedValue implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Double value;
        private Double confidenceScore;
        private Double lowerBound;
        private Double upperBound;
        private String unit;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelMetrics implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Double accuracy;
        private Double precision;
        private Double recall;
        private Double f1Score;
        private Double mse; // Mean Squared Error
        private Double mae; // Mean Absolute Error
    }
    
    public enum RiskLevel {
        NO_RISK,
        LOW,
        MODERATE,
        HIGH,
        SEVERE
    }
}