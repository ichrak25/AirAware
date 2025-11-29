package tn.airaware.iam.services;

import jakarta.ejb.EJBException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.microprofile.config.Config;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Email Service for AirAware
 * Handles account activation, alerts, and notifications
 */
@ApplicationScoped
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private static final String SMTP_HOST_KEY = "smtp.host";
    private static final String SMTP_PORT_KEY = "smtp.port";
    private static final String SMTP_USERNAME_KEY = "smtp.username";
    private static final String SMTP_PASSWORD_KEY = "smtp.password";
    private static final String SMTP_STARTTLS_KEY = "smtp.starttls.enable";

    @Inject
    private Config config;

    // Ces champs ne peuvent plus être final car initialisés dans @PostConstruct
    private String smtpHost;
    private int smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private boolean startTlsEnabled;

    // Constructeur sans argument REQUIS pour CDI proxy
    public EmailService() {
    }

    @PostConstruct
    public void init() {
        this.smtpHost = getConfigValue(SMTP_HOST_KEY, String.class);
        this.smtpPort = getConfigValue(SMTP_PORT_KEY, Integer.class);
        this.smtpUser = getConfigValue(SMTP_USERNAME_KEY, String.class);
        this.smtpPassword = getConfigValue(SMTP_PASSWORD_KEY, String.class);
        this.startTlsEnabled = getConfigValue(SMTP_STARTTLS_KEY, Boolean.class);
    }

    /**
     * Send email with custom content
     */
    public void sendEmail(String from, String to, String subject, String content) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        });

        try {
            Message message = createEmailMessage(session, from, to, subject, content);
            Transport.send(message);
            LOGGER.info("Email sent successfully to: " + to);
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email to: " + to, e);
            throw new EJBException("Failed to send email. Please check the configuration and recipient details.", e);
        }
    }

    /**
     * Send activation email for new accounts
     */
    public void sendActivationEmail(String to, String activationCode) {
        String subject = "AirAware - Activate Your Account";
        String content = String.format(
                "Dear User,\n\n" +
                        "Welcome to AirAware - Air Quality Monitoring System!\n\n" +
                        "To complete your account setup, please use the activation code below:\n\n" +
                        "Activation Code: %s\n\n" +
                        "⚠️ Please note: This code is valid for the next 5 minutes.\n\n" +
                        "If you did not request this activation, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "The AirAware Team\n" +
                        "Air Quality Monitoring for a Healthier Tomorrow",
                activationCode
        );

        sendEmail(smtpUser, to, subject, content);
    }

    /**
     * Send alert notification email
     */
    public void sendAlertNotification(String to, String alertType, String sensorId, String message) {
        String subject = "AirAware Alert - " + alertType;
        String content = String.format(
                "Dear User,\n\n" +
                        "An air quality alert has been triggered:\n\n" +
                        "Alert Type: %s\n" +
                        "Sensor ID: %s\n" +
                        "Message: %s\n\n" +
                        "Please check your AirAware dashboard for more details.\n\n" +
                        "Best regards,\n" +
                        "The AirAware Team",
                alertType, sensorId, message
        );

        sendEmail(smtpUser, to, subject, content);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String to, String resetCode) {
        String subject = "AirAware - Password Reset Request";
        String content = String.format(
                "Dear User,\n\n" +
                        "You requested to reset your password for AirAware.\n\n" +
                        "Reset Code: %s\n\n" +
                        "⚠️ This code is valid for 15 minutes.\n\n" +
                        "If you did not request this reset, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "The AirAware Team",
                resetCode
        );

        sendEmail(smtpUser, to, subject, content);
    }

    private Message createEmailMessage(Session session, String from, String to, String subject, String content)
            throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(content);
        return message;
    }

    private <T> T getConfigValue(String propertyName, Class<T> propertyType) {
        return config.getOptionalValue(propertyName, propertyType).orElseThrow(() ->
                new IllegalArgumentException("Missing required configuration: " + propertyName));
    }
}