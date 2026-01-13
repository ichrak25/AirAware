#!/bin/bash
# ============================================================================
# AirAware - WildFly Startup Script with Notification Configuration
# ============================================================================
# Usage: ./start-wildfly.sh
#
# Edit the values below with your actual credentials before running!
# ============================================================================

echo "═══════════════════════════════════════════════════════════"
echo "🚀 Starting AirAware Backend (WildFly)"
echo "═══════════════════════════════════════════════════════════"

# ==================== EMAIL CONFIGURATION ====================
# Set to "true" to enable email notifications
export EMAIL_NOTIFICATIONS_ENABLED=true

# Gmail SMTP settings (use App Password, not regular password)
# Get App Password: https://myaccount.google.com/apppasswords
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=aouadniichrak@gmail.com
export SMTP_PASSWORD="lwan jzam objc qwnl"

# Comma-separated list of email recipients
export ALERT_EMAIL_RECIPIENTS=ichrak.aouadni@supcom.tn

# ==================== SMS CONFIGURATION (Twilio) ====================
# Set to "true" to enable SMS notifications (CRITICAL alerts only)
export SMS_NOTIFICATIONS_ENABLED=false

# Twilio credentials - Get from https://console.twilio.com
export TWILIO_ACCOUNT_SID=your_account_sid
export TWILIO_AUTH_TOKEN=your_auth_token
export TWILIO_FROM_NUMBER=+1234567890

# Comma-separated phone numbers (with country code)
export SMS_RECIPIENTS=+21612345678,+21698765432

# ==================== SLACK CONFIGURATION ====================
# Create webhook: https://api.slack.com/messaging/webhooks
# Leave empty to disable
export SLACK_WEBHOOK_URL=

# ==================== DISCORD CONFIGURATION ====================
# Get webhook: Right-click channel -> Edit -> Integrations -> Webhooks
# Leave empty to disable
export DISCORD_WEBHOOK_URL=

# ==================== GENERAL SETTINGS ====================
# Your frontend dashboard URL (for links in notifications)
export DASHBOARD_URL=http://localhost:5173

# Enable console logging of alerts
export CONSOLE_LOGGING_ENABLED=true

# ==================== MQTT CONFIGURATION ====================
export MQTT_BROKER_URL=tcp://localhost:1883

# ==================== DATABASE CONFIGURATION ====================
export MONGODB_URI=mongodb://localhost:27017
export MONGODB_DATABASE=AirAwareDB

# ============================================================================
# Print Configuration Summary
# ============================================================================
echo ""
echo "📧 Email Notifications: $EMAIL_NOTIFICATIONS_ENABLED"
echo "📱 SMS Notifications:   $SMS_NOTIFICATIONS_ENABLED"
echo "💬 Slack Enabled:       $([ -n \"$SLACK_WEBHOOK_URL\" ] && echo 'true' || echo 'false')"
echo "🎮 Discord Enabled:     $([ -n \"$DISCORD_WEBHOOK_URL\" ] && echo 'true' || echo 'false')"
echo ""
echo "═══════════════════════════════════════════════════════════"

# ==================== START WILDFLY ====================
# Adjust the path to your WildFly installation
JBOSS_HOME=${JBOSS_HOME:-/c/wildfly-38.0.0.Final}

if [ ! -d "$JBOSS_HOME" ]; then
    echo "❌ ERROR: WildFly not found at $JBOSS_HOME"
    echo "   Set JBOSS_HOME environment variable to your WildFly installation"
    exit 1
fi

echo "Starting WildFly from: $JBOSS_HOME"
echo ""

# Start WildFly
$JBOSS_HOME/bin/standalone.sh