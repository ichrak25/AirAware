package tn.airaware.api.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.config.NotificationConfig;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import jakarta.mail.*;
import jakarta.mail.internet.*;

/**
 * Service for sending notifications (Email, SMS, Webhook, Web Push)
 * Supports multiple notification channels for critical alerts
 * 
 * Web Push notifications work even when the app is closed on both
 * desktop (PC) and mobile devices.
 *
 * Configuration is loaded from NotificationConfig which reads environment variables.
 */
@ApplicationScoped
public class NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());

    @Inject
    private NotificationConfig config;

    @Inject
    private UserEmailService userEmailService;

    @Inject
    private WebPushService webPushService;

    // ==================== MAIN NOTIFICATION METHOD ====================

    /**
     * Send notifications for an alert through all configured channels
     * Runs asynchronously to not block the main thread
     */
    public void sendAlertNotification(Alert alert) {
        LOGGER.info("📤 Sending notifications for alert: " + alert.getType() + " [" + alert.getSeverity() + "]");

        // Send notifications asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // 🔔 Web Push notifications (works even when app is closed!)
                // This is sent to all subscribed browsers/devices
                try {
                    LOGGER.info("📱 Sending Web Push notifications...");
                    webPushService.sendPushToAll(alert);
                } catch (Exception e) {
                    LOGGER.warning("Web Push notification failed: " + e.getMessage());
                }

                // Email notification
                if (config.isEmailEnabled()) {
                    sendEmailNotification(alert);
                }

                // SMS notification (only for CRITICAL alerts)
                if (config.isSmsEnabled() && "CRITICAL".equals(alert.getSeverity())) {
                    sendSMSNotification(alert);
                }

                // Slack notification
                if (config.isSlackEnabled()) {
                    sendSlackNotification(alert);
                }

                // Discord notification
                if (config.isDiscordEnabled()) {
                    sendDiscordNotification(alert);
                }

                // Always log to console
                if (config.isConsoleLoggingEnabled()) {
                    logAlertToConsole(alert);
                }

            } catch (Exception e) {
                LOGGER.severe("Failed to send notifications: " + e.getMessage());
            }
        });
    }

    // ==================== EMAIL NOTIFICATION ====================
    
    // Reusable SMTP session (created once, reused for all emails)
    private Session smtpSession = null;
    private Transport smtpTransport = null;
    private long lastSmtpConnectionTime = 0;
    private static final long SMTP_CONNECTION_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    private void sendEmailNotification(Alert alert) {
        try {
            // Collect all recipients: configured recipients + all users from database
            List<String> allRecipients = new java.util.ArrayList<>();
            
            // Add configured recipients (admin emails from env vars)
            List<String> configuredRecipients = config.getEmailRecipients();
            if (configuredRecipients != null && !configuredRecipients.isEmpty()) {
                allRecipients.addAll(configuredRecipients);
            }
            
            // Add all registered users from IAM database
            if (userEmailService != null && userEmailService.isConnected()) {
                List<String> userEmails = userEmailService.getAllUserEmails();
                for (String email : userEmails) {
                    if (!allRecipients.contains(email)) {
                        allRecipients.add(email);
                    }
                }
                LOGGER.info("📧 Added " + userEmails.size() + " user emails from database");
            }

            if (allRecipients.isEmpty()) {
                LOGGER.warning("⚠️ No email recipients configured or found in database");
                return;
            }

            // Get or create SMTP session
            Session session = getOrCreateSmtpSession();
            
            // Create email with BCC to all recipients (single email, single login)
            String subject = getEmailSubject(alert);
            String htmlBody = createEmailBody(alert);
            
            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(config.getSmtpUsername(), "AirAware Alerts"));
                
                // Use BCC for all recipients (privacy + single email)
                InternetAddress[] bccAddresses = allRecipients.stream()
                        .map(email -> {
                            try {
                                return new InternetAddress(email.trim());
                            } catch (Exception e) {
                                LOGGER.warning("Invalid email address: " + email);
                                return null;
                            }
                        })
                        .filter(addr -> addr != null)
                        .toArray(InternetAddress[]::new);
                
                if (bccAddresses.length == 0) {
                    LOGGER.warning("⚠️ No valid email addresses found");
                    return;
                }
                
                // Set first recipient as TO (required), rest as BCC
                message.addRecipient(Message.RecipientType.TO, bccAddresses[0]);
                if (bccAddresses.length > 1) {
                    InternetAddress[] bccOnly = java.util.Arrays.copyOfRange(bccAddresses, 1, bccAddresses.length);
                    message.addRecipients(Message.RecipientType.BCC, bccOnly);
                }
                
                message.setSubject(subject);
                message.setContent(htmlBody, "text/html; charset=utf-8");
                
                // Send using reusable transport
                sendWithReusableTransport(session, message);
                
                LOGGER.info("📨 Email notification sent to " + allRecipients.size() + " recipients (single email with BCC)");
                
            } catch (Exception e) {
                LOGGER.warning("❌ Failed to send email: " + e.getMessage());
                // Reset connection on failure so next attempt creates fresh connection
                closeSmtpConnection();
            }

        } catch (Exception e) {
            LOGGER.warning("❌ Failed to send email notifications: " + e.getMessage());
        }
    }
    
    /**
     * Get or create a reusable SMTP session (avoids multiple login attempts)
     */
    private synchronized Session getOrCreateSmtpSession() {
        if (smtpSession == null) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", String.valueOf(config.isSmtpAuthEnabled()));
            props.put("mail.smtp.starttls.enable", String.valueOf(config.isSmtpStartTlsEnabled()));
            props.put("mail.smtp.host", config.getSmtpHost());
            props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
            // Connection pooling settings
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            smtpSession = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
                }
            });
            
            LOGGER.info("📧 Created new SMTP session");
        }
        return smtpSession;
    }
    
    /**
     * Send message using reusable transport connection
     */
    private synchronized void sendWithReusableTransport(Session session, Message message) throws MessagingException {
        long now = System.currentTimeMillis();
        
        // Check if we need to create or reconnect transport
        if (smtpTransport == null || !smtpTransport.isConnected() || 
            (now - lastSmtpConnectionTime) > SMTP_CONNECTION_TIMEOUT_MS) {
            
            // Close old connection if exists
            if (smtpTransport != null) {
                try {
                    smtpTransport.close();
                } catch (Exception ignored) {}
            }
            
            // Create new connection
            smtpTransport = session.getTransport("smtp");
            smtpTransport.connect(config.getSmtpHost(), config.getSmtpUsername(), config.getSmtpPassword());
            lastSmtpConnectionTime = now;
            LOGGER.info("📧 Connected to SMTP server");
        }
        
        // Send using existing connection
        smtpTransport.sendMessage(message, message.getAllRecipients());
    }
    
    /**
     * Close SMTP connection (called on errors or shutdown)
     */
    private synchronized void closeSmtpConnection() {
        if (smtpTransport != null) {
            try {
                smtpTransport.close();
            } catch (Exception ignored) {}
            smtpTransport = null;
        }
        smtpSession = null;
        LOGGER.info("📧 SMTP connection closed");
    }

    private String getEmailSubject(Alert alert) {
        String emoji = switch (alert.getSeverity()) {
            case "CRITICAL" -> "🚨";
            case "WARNING" -> "⚠️";
            default -> "ℹ️";
        };
        return emoji + " AirAware Alert: " + alert.getType() + " [" + alert.getSeverity() + "]";
    }

    private String createEmailBody(Alert alert) {
        String severityColor = switch (alert.getSeverity()) {
            case "CRITICAL" -> "#dc2626";
            case "WARNING" -> "#f59e0b";
            default -> "#3b82f6";
        };

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                    .header { background: %s; color: white; padding: 24px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 24px; }
                    .alert-box { background: #f8fafc; border-left: 4px solid %s; padding: 16px; margin: 16px 0; border-radius: 0 8px 8px 0; }
                    .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #e2e8f0; }
                    .info-label { color: #64748b; font-weight: 500; }
                    .info-value { color: #1e293b; font-weight: 600; }
                    .footer { background: #f8fafc; padding: 16px 24px; text-align: center; color: #64748b; font-size: 12px; }
                    .button { display: inline-block; background: #0ea5e9; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; margin-top: 16px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌍 AirAware Alert</h1>
                    </div>
                    <div class="content">
                        <div class="alert-box">
                            <strong style="font-size: 18px;">%s</strong>
                            <p style="margin: 8px 0 0 0; color: #475569;">%s</p>
                        </div>
                        
                        <div class="info-row">
                            <span class="info-label">Alert Type</span>
                            <span class="info-value">%s</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Severity</span>
                            <span class="info-value" style="color: %s;">%s</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Sensor ID</span>
                            <span class="info-value">%s</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Triggered At</span>
                            <span class="info-value">%s</span>
                        </div>
                        
                        <a href="%s/alerts" class="button">View Dashboard →</a>
                    </div>
                    <div class="footer">
                        <p>AirAware - IoT Air Quality Monitoring System</p>
                        <p>Tunisia | Powered by Raspberry Pi & Machine Learning</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                severityColor,
                severityColor,
                alert.getSeverity(),
                alert.getMessage(),
                alert.getType(),
                severityColor,
                alert.getSeverity(),
                alert.getSensorId(),
                alert.getTriggeredAt() != null ? alert.getTriggeredAt().toString() : "N/A",
                config.getDashboardUrl()
        );
    }

    // ==================== SMS NOTIFICATION (Twilio) ====================

    private void sendSMSNotification(Alert alert) {
        if (!config.isSmsEnabled()) {
            LOGGER.fine("SMS notifications not configured - skipping");
            return;
        }

        try {
            String messageBody = createSMSBody(alert);
            List<String> recipients = config.getSmsRecipients();

            for (String recipient : recipients) {
                sendTwilioSMS(recipient.trim(), messageBody);
            }

            LOGGER.info("✅ SMS notifications sent successfully");

        } catch (Exception e) {
            LOGGER.warning("❌ Failed to send SMS notification: " + e.getMessage());
        }
    }

    private String createSMSBody(Alert alert) {
        return String.format(
                "🚨 AirAware CRITICAL ALERT\n%s\nSensor: %s\n%s\nView: airaware.tn/alerts",
                alert.getType(),
                alert.getSensorId(),
                alert.getMessage()
        );
    }

    private void sendTwilioSMS(String to, String body) throws Exception {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + config.getTwilioAccountSid() + "/Messages.json";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // Basic Auth
        String auth = config.getTwilioAccountSid() + ":" + config.getTwilioAuthToken();
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String data = "To=" + java.net.URLEncoder.encode(to, "UTF-8") +
                "&From=" + java.net.URLEncoder.encode(config.getTwilioFromNumber(), "UTF-8") +
                "&Body=" + java.net.URLEncoder.encode(body, "UTF-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 201) {
            LOGGER.warning("Twilio SMS failed with code: " + responseCode);
        }
    }

    // ==================== SLACK NOTIFICATION ====================

    private void sendSlackNotification(Alert alert) {
        try {
            String payload = createSlackPayload(alert);
            sendWebhook(config.getSlackWebhookUrl(), payload);
            LOGGER.info("✅ Slack notification sent successfully");

        } catch (Exception e) {
            LOGGER.warning("❌ Failed to send Slack notification: " + e.getMessage());
        }
    }

    private String createSlackPayload(Alert alert) {
        String emoji = switch (alert.getSeverity()) {
            case "CRITICAL" -> ":rotating_light:";
            case "WARNING" -> ":warning:";
            default -> ":information_source:";
        };

        String color = switch (alert.getSeverity()) {
            case "CRITICAL" -> "#dc2626";
            case "WARNING" -> "#f59e0b";
            default -> "#3b82f6";
        };

        return """
            {
                "attachments": [
                    {
                        "color": "%s",
                        "blocks": [
                            {
                                "type": "header",
                                "text": {
                                    "type": "plain_text",
                                    "text": "%s AirAware Alert: %s",
                                    "emoji": true
                                }
                            },
                            {
                                "type": "section",
                                "fields": [
                                    {"type": "mrkdwn", "text": "*Type:*\\n%s"},
                                    {"type": "mrkdwn", "text": "*Severity:*\\n%s"},
                                    {"type": "mrkdwn", "text": "*Sensor:*\\n%s"},
                                    {"type": "mrkdwn", "text": "*Time:*\\n%s"}
                                ]
                            },
                            {
                                "type": "section",
                                "text": {
                                    "type": "mrkdwn",
                                    "text": "*Message:* %s"
                                }
                            },
                            {
                                "type": "actions",
                                "elements": [
                                    {
                                        "type": "button",
                                        "text": {"type": "plain_text", "text": "View Dashboard"},
                                        "url": "%s/alerts"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
            """.formatted(
                color, emoji, alert.getType(),
                alert.getType(), alert.getSeverity(),
                alert.getSensorId(),
                alert.getTriggeredAt() != null ? alert.getTriggeredAt().toString() : "N/A",
                alert.getMessage(),
                config.getDashboardUrl()
        );
    }

    // ==================== DISCORD NOTIFICATION ====================

    private void sendDiscordNotification(Alert alert) {
        try {
            String payload = createDiscordPayload(alert);
            sendWebhook(config.getDiscordWebhookUrl(), payload);
            LOGGER.info("✅ Discord notification sent successfully");

        } catch (Exception e) {
            LOGGER.warning("❌ Failed to send Discord notification: " + e.getMessage());
        }
    }

    private String createDiscordPayload(Alert alert) {
        int color = switch (alert.getSeverity()) {
            case "CRITICAL" -> 14370832; // Red
            case "WARNING" -> 16098851;  // Orange
            default -> 3901635;          // Blue
        };

        String emoji = switch (alert.getSeverity()) {
            case "CRITICAL" -> "🚨";
            case "WARNING" -> "⚠️";
            default -> "ℹ️";
        };

        return """
            {
                "embeds": [
                    {
                        "title": "%s AirAware Alert",
                        "description": "%s",
                        "color": %d,
                        "fields": [
                            {"name": "Type", "value": "%s", "inline": true},
                            {"name": "Severity", "value": "%s", "inline": true},
                            {"name": "Sensor", "value": "%s", "inline": true}
                        ],
                        "footer": {"text": "AirAware - Air Quality Monitoring"},
                        "timestamp": "%s"
                    }
                ]
            }
            """.formatted(
                emoji,
                alert.getMessage(),
                color,
                alert.getType(),
                alert.getSeverity(),
                alert.getSensorId(),
                alert.getTriggeredAt() != null ? alert.getTriggeredAt().toString() : java.time.Instant.now().toString()
        );
    }

    // ==================== HELPER METHODS ====================

    private void sendWebhook(String webhookUrl, String payload) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            LOGGER.warning("Webhook failed with code: " + responseCode);
        }
    }

    private void logAlertToConsole(Alert alert) {
        String border = "═".repeat(60);
        String emoji = switch (alert.getSeverity()) {
            case "CRITICAL" -> "🚨";
            case "WARNING" -> "⚠️";
            default -> "ℹ️";
        };

        LOGGER.info("\n" + border);
        LOGGER.info(emoji + " ALERT NOTIFICATION");
        LOGGER.info(border);
        LOGGER.info("Type:     " + alert.getType());
        LOGGER.info("Severity: " + alert.getSeverity());
        LOGGER.info("Sensor:   " + alert.getSensorId());
        LOGGER.info("Message:  " + alert.getMessage());
        LOGGER.info("Time:     " + (alert.getTriggeredAt() != null ? alert.getTriggeredAt().toString() : "N/A"));
        LOGGER.info(border + "\n");
    }
}