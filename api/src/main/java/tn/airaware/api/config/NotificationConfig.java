package tn.airaware.api.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Configuration class for notification settings
 * Reads configuration from environment variables or system properties
 * 
 * Environment Variables:
 * - EMAIL_NOTIFICATIONS_ENABLED: true/false
 * - SMTP_HOST: SMTP server hostname
 * - SMTP_PORT: SMTP server port
 * - SMTP_USERNAME: SMTP username (email)
 * - SMTP_PASSWORD: SMTP password (app password for Gmail)
 * - ALERT_EMAIL_RECIPIENTS: comma-separated list of emails
 * - SMS_NOTIFICATIONS_ENABLED: true/false
 * - TWILIO_ACCOUNT_SID: Twilio account SID
 * - TWILIO_AUTH_TOKEN: Twilio auth token
 * - TWILIO_FROM_NUMBER: Twilio phone number
 * - SMS_RECIPIENTS: comma-separated list of phone numbers
 * - SLACK_WEBHOOK_URL: Slack incoming webhook URL
 * - DISCORD_WEBHOOK_URL: Discord webhook URL
 */
@ApplicationScoped
@Named("notificationConfig")
public class NotificationConfig {

    private static final Logger LOGGER = Logger.getLogger(NotificationConfig.class.getName());

    // ==================== EMAIL CONFIGURATION ====================
    
    private final boolean emailEnabled;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final List<String> emailRecipients;
    private final boolean smtpStartTlsEnabled;
    private final boolean smtpAuthEnabled;

    // ==================== SMS CONFIGURATION (Twilio) ====================
    
    private final boolean smsEnabled;
    private final String twilioAccountSid;
    private final String twilioAuthToken;
    private final String twilioFromNumber;
    private final List<String> smsRecipients;

    // ==================== WEBHOOK CONFIGURATION ====================
    
    private final String slackWebhookUrl;
    private final String discordWebhookUrl;
    private final boolean slackEnabled;
    private final boolean discordEnabled;

    // ==================== GENERAL SETTINGS ====================
    
    private final String dashboardUrl;
    private final boolean consoleLoggingEnabled;

    // ==================== CONSTRUCTOR ====================

    public NotificationConfig() {
        LOGGER.info("Loading notification configuration...");

        // Email configuration
        this.emailEnabled = getBooleanEnv("EMAIL_NOTIFICATIONS_ENABLED", false);
        this.smtpHost = getEnv("SMTP_HOST", "smtp.gmail.com");
        this.smtpPort = getIntEnv("SMTP_PORT", 587);
        this.smtpUsername = getEnv("SMTP_USERNAME", "");
        this.smtpPassword = getEnv("SMTP_PASSWORD", "");
        this.emailRecipients = getListEnv("ALERT_EMAIL_RECIPIENTS");
        this.smtpStartTlsEnabled = getBooleanEnv("SMTP_STARTTLS_ENABLED", true);
        this.smtpAuthEnabled = getBooleanEnv("SMTP_AUTH_ENABLED", true);

        // SMS configuration
        this.smsEnabled = getBooleanEnv("SMS_NOTIFICATIONS_ENABLED", false);
        this.twilioAccountSid = getEnv("TWILIO_ACCOUNT_SID", "");
        this.twilioAuthToken = getEnv("TWILIO_AUTH_TOKEN", "");
        this.twilioFromNumber = getEnv("TWILIO_FROM_NUMBER", "");
        this.smsRecipients = getListEnv("SMS_RECIPIENTS");

        // Webhook configuration
        this.slackWebhookUrl = getEnv("SLACK_WEBHOOK_URL", "");
        this.discordWebhookUrl = getEnv("DISCORD_WEBHOOK_URL", "");
        this.slackEnabled = !slackWebhookUrl.isEmpty();
        this.discordEnabled = !discordWebhookUrl.isEmpty();

        // General settings
        this.dashboardUrl = getEnv("DASHBOARD_URL", "https://airaware.tn");
        this.consoleLoggingEnabled = getBooleanEnv("CONSOLE_LOGGING_ENABLED", true);

        logConfiguration();
    }

    // ==================== GETTERS ====================

    // Email
    public boolean isEmailEnabled() {
        return emailEnabled && !smtpUsername.isEmpty() && !smtpPassword.isEmpty();
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public List<String> getEmailRecipients() {
        return emailRecipients;
    }

    public boolean isSmtpStartTlsEnabled() {
        return smtpStartTlsEnabled;
    }

    public boolean isSmtpAuthEnabled() {
        return smtpAuthEnabled;
    }

    // SMS
    public boolean isSmsEnabled() {
        return smsEnabled && !twilioAccountSid.isEmpty() && !twilioAuthToken.isEmpty();
    }

    public String getTwilioAccountSid() {
        return twilioAccountSid;
    }

    public String getTwilioAuthToken() {
        return twilioAuthToken;
    }

    public String getTwilioFromNumber() {
        return twilioFromNumber;
    }

    public List<String> getSmsRecipients() {
        return smsRecipients;
    }

    // Webhooks
    public boolean isSlackEnabled() {
        return slackEnabled;
    }

    public String getSlackWebhookUrl() {
        return slackWebhookUrl;
    }

    public boolean isDiscordEnabled() {
        return discordEnabled;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    // General
    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public boolean isConsoleLoggingEnabled() {
        return consoleLoggingEnabled;
    }

    // ==================== HELPER METHODS ====================

    private String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    private boolean getBooleanEnv(String key, boolean defaultValue) {
        String value = getEnv(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private int getIntEnv(String key, int defaultValue) {
        try {
            return Integer.parseInt(getEnv(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private List<String> getListEnv(String key) {
        String value = getEnv(key, "");
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void logConfiguration() {
        LOGGER.info("═══════════════════════════════════════════════════════════");
        LOGGER.info("📧 Notification Configuration Loaded");
        LOGGER.info("═══════════════════════════════════════════════════════════");
        LOGGER.info("Email Notifications: " + (isEmailEnabled() ? "✅ ENABLED" : "❌ DISABLED"));
        if (isEmailEnabled()) {
            LOGGER.info("  SMTP Host: " + smtpHost + ":" + smtpPort);
            LOGGER.info("  Recipients: " + emailRecipients.size() + " configured");
        }
        LOGGER.info("SMS Notifications: " + (isSmsEnabled() ? "✅ ENABLED" : "❌ DISABLED"));
        if (isSmsEnabled()) {
            LOGGER.info("  Recipients: " + smsRecipients.size() + " configured");
        }
        LOGGER.info("Slack Notifications: " + (isSlackEnabled() ? "✅ ENABLED" : "❌ DISABLED"));
        LOGGER.info("Discord Notifications: " + (isDiscordEnabled() ? "✅ ENABLED" : "❌ DISABLED"));
        LOGGER.info("Console Logging: " + (isConsoleLoggingEnabled() ? "✅ ENABLED" : "❌ DISABLED"));
        LOGGER.info("═══════════════════════════════════════════════════════════");
    }

    // ==================== VALIDATION ====================

    /**
     * Validate the configuration and return any errors
     */
    public List<String> validate() {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();

        if (emailEnabled) {
            if (smtpUsername.isEmpty()) {
                errors.add("EMAIL_NOTIFICATIONS_ENABLED is true but SMTP_USERNAME is not set");
            }
            if (smtpPassword.isEmpty()) {
                errors.add("EMAIL_NOTIFICATIONS_ENABLED is true but SMTP_PASSWORD is not set");
            }
            if (emailRecipients.isEmpty()) {
                errors.add("EMAIL_NOTIFICATIONS_ENABLED is true but ALERT_EMAIL_RECIPIENTS is not set");
            }
        }

        if (smsEnabled) {
            if (twilioAccountSid.isEmpty()) {
                errors.add("SMS_NOTIFICATIONS_ENABLED is true but TWILIO_ACCOUNT_SID is not set");
            }
            if (twilioAuthToken.isEmpty()) {
                errors.add("SMS_NOTIFICATIONS_ENABLED is true but TWILIO_AUTH_TOKEN is not set");
            }
            if (twilioFromNumber.isEmpty()) {
                errors.add("SMS_NOTIFICATIONS_ENABLED is true but TWILIO_FROM_NUMBER is not set");
            }
            if (smsRecipients.isEmpty()) {
                errors.add("SMS_NOTIFICATIONS_ENABLED is true but SMS_RECIPIENTS is not set");
            }
        }

        return errors;
    }

    /**
     * Check if any notification channel is enabled
     */
    public boolean isAnyChannelEnabled() {
        return isEmailEnabled() || isSmsEnabled() || isSlackEnabled() || isDiscordEnabled();
    }

    @Override
    public String toString() {
        return "NotificationConfig{" +
                "emailEnabled=" + isEmailEnabled() +
                ", smsEnabled=" + isSmsEnabled() +
                ", slackEnabled=" + isSlackEnabled() +
                ", discordEnabled=" + isDiscordEnabled() +
                '}';
    }
}