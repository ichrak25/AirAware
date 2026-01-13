package tn.airaware.api.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.entities.Reading;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for automatic alert generation based on air quality thresholds
 *
 * Thresholds based on:
 * - EPA Air Quality Index (AQI) standards
 * - WHO Air Quality Guidelines
 * - OSHA workplace safety limits
 */
@ApplicationScoped
public class AlertThresholdService {

    private static final Logger LOGGER = Logger.getLogger(AlertThresholdService.class.getName());

    @Inject
    private AlertService alertService;

    @Inject
    private NotificationService notificationService;

    // ==================== NOTIFICATION COOLDOWN SETTINGS ====================
    
    // ⚠️ TEMPORARILY DISABLE EMAIL NOTIFICATIONS
    // Set to true to completely disable email notifications (useful when Gmail is blocked)
    // Change to false once Gmail unblocks the account (usually after 1-24 hours)
    private static final boolean DISABLE_EMAIL_NOTIFICATIONS = true;
    
    // Cooldown period in milliseconds (2 hours for CRITICAL alerts)
    // This prevents email spam even when enabled
    private static final long CRITICAL_ALERT_COOLDOWN_MS = 2 * 60 * 60 * 1000; // 2 hours
    
    // Track last notification time per sensor+alert type to prevent spam
    // Key: "sensorId:alertType", Value: timestamp of last email sent
    private static final java.util.Map<String, Long> lastNotificationTimes = 
            new java.util.concurrent.ConcurrentHashMap<>();

    // ==================== THRESHOLD DEFINITIONS ====================

    // PM2.5 thresholds (µg/m³) - EPA AQI breakpoints
    private static final double PM25_GOOD = 12.0;
    private static final double PM25_MODERATE = 35.4;
    private static final double PM25_UNHEALTHY_SENSITIVE = 55.4;
    private static final double PM25_UNHEALTHY = 150.4;
    private static final double PM25_VERY_UNHEALTHY = 250.4;
    private static final double PM25_HAZARDOUS = 500.0;

    // PM10 thresholds (µg/m³)
    private static final double PM10_MODERATE = 54.0;
    private static final double PM10_UNHEALTHY_SENSITIVE = 154.0;
    private static final double PM10_UNHEALTHY = 254.0;

    // CO2 thresholds (ppm) - OSHA/ASHRAE standards
    private static final double CO2_NORMAL = 800.0;
    private static final double CO2_ELEVATED = 1000.0;
    private static final double CO2_HIGH = 2000.0;
    private static final double CO2_DANGEROUS = 5000.0;

    // VOC thresholds (mg/m³)
    private static final double VOC_MODERATE = 0.5;
    private static final double VOC_HIGH = 1.0;
    private static final double VOC_DANGEROUS = 3.0;

    // Temperature thresholds (°C)
    private static final double TEMP_LOW = 10.0;
    private static final double TEMP_HIGH = 35.0;
    private static final double TEMP_EXTREME = 40.0;

    // Humidity thresholds (%)
    private static final double HUMIDITY_LOW = 30.0;
    private static final double HUMIDITY_HIGH = 70.0;

    // ==================== MAIN CHECK METHOD ====================

    /**
     * Check a reading against all thresholds and generate alerts if needed
     * Called automatically when a new reading is received via MQTT
     *
     * @param reading The sensor reading to check
     * @return List of generated alerts
     */
    public List<Alert> checkThresholdsAndGenerateAlerts(Reading reading) {
        List<Alert> generatedAlerts = new ArrayList<>();

        if (reading == null) {
            return generatedAlerts;
        }

        String sensorId = reading.getSensorId();
        LOGGER.fine("Checking thresholds for sensor: " + sensorId);

        // Check PM2.5 (skip if value is 0 which indicates not set)
        Alert pm25Alert = checkPM25(reading);
        if (pm25Alert != null) {
            generatedAlerts.add(pm25Alert);
        }

        // Check PM10
        Alert pm10Alert = checkPM10(reading);
        if (pm10Alert != null) {
            generatedAlerts.add(pm10Alert);
        }

        // Check CO2
        Alert co2Alert = checkCO2(reading);
        if (co2Alert != null) {
            generatedAlerts.add(co2Alert);
        }

        // Check VOC
        Alert vocAlert = checkVOC(reading);
        if (vocAlert != null) {
            generatedAlerts.add(vocAlert);
        }

        // Check Temperature
        Alert tempAlert = checkTemperature(reading);
        if (tempAlert != null) {
            generatedAlerts.add(tempAlert);
        }

        // Check Humidity
        Alert humidityAlert = checkHumidity(reading);
        if (humidityAlert != null) {
            generatedAlerts.add(humidityAlert);
        }

        // Save all generated alerts and send notifications
        for (Alert alert : generatedAlerts) {
            try {
                alertService.saveAlert(alert);
                LOGGER.info("🚨 Alert generated: " + alert.getType() + " - " + alert.getSeverity());

                // Only send email notifications for CRITICAL alerts with cooldown
                if ("CRITICAL".equals(alert.getSeverity())) {
                    if (DISABLE_EMAIL_NOTIFICATIONS) {
                        LOGGER.info("📧 Email notifications DISABLED - CRITICAL alert logged only: " + alert.getType());
                    } else if (shouldSendNotification(alert)) {
                        notificationService.sendAlertNotification(alert);
                        recordNotificationSent(alert);
                        LOGGER.info("📧 Email notification sent for CRITICAL alert: " + alert.getType());
                    } else {
                        LOGGER.fine("⏳ Skipping notification (cooldown active): " + alert.getType());
                    }
                }
                // WARNING and INFO alerts are logged to console but no email
                // Users can see them in the PWA Alerts page
            } catch (Exception e) {
                LOGGER.severe("Failed to save alert: " + e.getMessage());
            }
        }

        return generatedAlerts;
    }

    // ==================== NOTIFICATION COOLDOWN LOGIC ====================

    /**
     * Check if we should send a notification for this alert
     * Returns true if no notification was sent recently for this sensor+alert type
     */
    private boolean shouldSendNotification(Alert alert) {
        String key = getNotificationKey(alert);
        Long lastSent = lastNotificationTimes.get(key);
        
        if (lastSent == null) {
            return true; // Never sent before
        }
        
        long elapsed = System.currentTimeMillis() - lastSent;
        return elapsed >= CRITICAL_ALERT_COOLDOWN_MS;
    }

    /**
     * Record that a notification was sent for this alert
     */
    private void recordNotificationSent(Alert alert) {
        String key = getNotificationKey(alert);
        lastNotificationTimes.put(key, System.currentTimeMillis());
    }

    /**
     * Generate a unique key for tracking notifications per sensor+alert type
     */
    private String getNotificationKey(Alert alert) {
        return alert.getSensorId() + ":" + alert.getType();
    }

    // ==================== INDIVIDUAL THRESHOLD CHECKS ====================

    private Alert checkPM25(Reading reading) {
        double value = reading.getPm25();
        String sensorId = reading.getSensorId();

        // Skip if value is 0 or negative (likely not set for primitive double)
        if (value <= 0) {
            return null;
        }

        if (value >= PM25_HAZARDOUS) {
            return createAlert("PM25_HAZARDOUS", "CRITICAL",
                    String.format("🚨 HAZARDOUS: PM2.5 at %.1f µg/m³ - Serious health risk for everyone!", value),
                    sensorId, reading);
        } else if (value >= PM25_VERY_UNHEALTHY) {
            return createAlert("PM25_VERY_UNHEALTHY", "CRITICAL",
                    String.format("⚠️ VERY UNHEALTHY: PM2.5 at %.1f µg/m³ - Health alert for all groups", value),
                    sensorId, reading);
        } else if (value >= PM25_UNHEALTHY) {
            return createAlert("PM25_UNHEALTHY", "WARNING",
                    String.format("⚠️ UNHEALTHY: PM2.5 at %.1f µg/m³ - Everyone may experience health effects", value),
                    sensorId, reading);
        } else if (value >= PM25_UNHEALTHY_SENSITIVE) {
            return createAlert("PM25_UNHEALTHY_SENSITIVE", "WARNING",
                    String.format("PM2.5 at %.1f µg/m³ - Unhealthy for sensitive groups", value),
                    sensorId, reading);
        } else if (value >= PM25_MODERATE) {
            return createAlert("PM25_MODERATE", "INFO",
                    String.format("PM2.5 at %.1f µg/m³ - Moderate air quality", value),
                    sensorId, reading);
        }

        return null;
    }

    private Alert checkPM10(Reading reading) {
        double value = reading.getPm10();
        String sensorId = reading.getSensorId();

        // Skip if value is 0 or negative (likely not set)
        if (value <= 0) {
            return null;
        }

        if (value >= PM10_UNHEALTHY) {
            return createAlert("PM10_UNHEALTHY", "WARNING",
                    String.format("⚠️ PM10 at %.1f µg/m³ - Unhealthy levels", value),
                    sensorId, reading);
        } else if (value >= PM10_UNHEALTHY_SENSITIVE) {
            return createAlert("PM10_ELEVATED", "INFO",
                    String.format("PM10 at %.1f µg/m³ - Elevated for sensitive groups", value),
                    sensorId, reading);
        }

        return null;
    }

    private Alert checkCO2(Reading reading) {
        double value = reading.getCo2();
        String sensorId = reading.getSensorId();

        // Skip if value is 0 or negative (likely not set)
        if (value <= 0) {
            return null;
        }

        if (value >= CO2_DANGEROUS) {
            return createAlert("CO2_DANGEROUS", "CRITICAL",
                    String.format("🚨 DANGER: CO2 at %.0f ppm - Immediately dangerous! Evacuate area!", value),
                    sensorId, reading);
        } else if (value >= CO2_HIGH) {
            return createAlert("CO2_HIGH", "WARNING",
                    String.format("⚠️ HIGH CO2: %.0f ppm - Poor ventilation, may cause drowsiness", value),
                    sensorId, reading);
        } else if (value >= CO2_ELEVATED) {
            return createAlert("CO2_ELEVATED", "INFO",
                    String.format("CO2 at %.0f ppm - Consider improving ventilation", value),
                    sensorId, reading);
        }

        return null;
    }

    private Alert checkVOC(Reading reading) {
        double value = reading.getVoc();
        String sensorId = reading.getSensorId();

        // Skip if value is 0 or negative (likely not set)
        if (value <= 0) {
            return null;
        }

        if (value >= VOC_DANGEROUS) {
            return createAlert("VOC_DANGEROUS", "CRITICAL",
                    String.format("🚨 DANGER: VOC at %.2f mg/m³ - Toxic levels detected!", value),
                    sensorId, reading);
        } else if (value >= VOC_HIGH) {
            return createAlert("VOC_HIGH", "WARNING",
                    String.format("⚠️ HIGH VOC: %.2f mg/m³ - May cause health effects", value),
                    sensorId, reading);
        } else if (value >= VOC_MODERATE) {
            return createAlert("VOC_ELEVATED", "INFO",
                    String.format("VOC at %.2f mg/m³ - Slightly elevated", value),
                    sensorId, reading);
        }

        return null;
    }

    private Alert checkTemperature(Reading reading) {
        double value = reading.getTemperature();
        String sensorId = reading.getSensorId();

        // For temperature, 0 could be valid, so we check for extreme invalid values
        if (value < -100 || value > 100) {
            return null; // Invalid reading
        }

        if (value >= TEMP_EXTREME) {
            return createAlert("TEMPERATURE_EXTREME", "CRITICAL",
                    String.format("🚨 EXTREME HEAT: %.1f°C - Heat stroke risk!", value),
                    sensorId, reading);
        } else if (value >= TEMP_HIGH) {
            return createAlert("TEMPERATURE_HIGH", "WARNING",
                    String.format("⚠️ High temperature: %.1f°C", value),
                    sensorId, reading);
        } else if (value <= TEMP_LOW) {
            return createAlert("TEMPERATURE_LOW", "INFO",
                    String.format("Low temperature: %.1f°C", value),
                    sensorId, reading);
        }

        return null;
    }

    private Alert checkHumidity(Reading reading) {
        double value = reading.getHumidity();
        String sensorId = reading.getSensorId();

        // Skip if value is out of valid range (0-100%)
        if (value <= 0 || value > 100) {
            return null;
        }

        if (value >= HUMIDITY_HIGH) {
            return createAlert("HUMIDITY_HIGH", "INFO",
                    String.format("High humidity: %.1f%% - May feel uncomfortable", value),
                    sensorId, reading);
        } else if (value <= HUMIDITY_LOW) {
            return createAlert("HUMIDITY_LOW", "INFO",
                    String.format("Low humidity: %.1f%% - Air is dry", value),
                    sensorId, reading);
        }

        return null;
    }

    // ==================== HELPER METHODS ====================

    private Alert createAlert(String type, String severity, String message,
                              String sensorId, Reading reading) {
        Alert alert = new Alert(type, severity, message, sensorId, reading);
        return alert;
    }

    /**
     * Calculate AQI from PM2.5 value using EPA breakpoints
     */
    public int calculateAQI(double pm25) {
        if (pm25 <= 12.0) {
            return (int) linearScale(pm25, 0, 12.0, 0, 50);
        } else if (pm25 <= 35.4) {
            return (int) linearScale(pm25, 12.1, 35.4, 51, 100);
        } else if (pm25 <= 55.4) {
            return (int) linearScale(pm25, 35.5, 55.4, 101, 150);
        } else if (pm25 <= 150.4) {
            return (int) linearScale(pm25, 55.5, 150.4, 151, 200);
        } else if (pm25 <= 250.4) {
            return (int) linearScale(pm25, 150.5, 250.4, 201, 300);
        } else if (pm25 <= 500.4) {
            return (int) linearScale(pm25, 250.5, 500.4, 301, 500);
        } else {
            return 500; // Max AQI
        }
    }

    private double linearScale(double value, double inMin, double inMax, double outMin, double outMax) {
        return ((value - inMin) / (inMax - inMin)) * (outMax - outMin) + outMin;
    }
}