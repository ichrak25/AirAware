package tn.airaware.iam.services;

import jakarta.ejb.EJBException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import tn.airaware.core.entities.Identity;
import tn.airaware.iam.enums.Role;
import tn.airaware.iam.repositories.IdentityRepository;
import tn.airaware.core.security.Argon2Utils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Identity Services for AirAware
 * Handles user registration, activation, and management
 *
 * UPDATED: Added resendActivationCode() method for PWA integration
 */
@ApplicationScoped
public class IdentityServices {

    private static final Logger LOGGER = Logger.getLogger(IdentityServices.class.getName());

    @Inject
    private IdentityRepository identityRepository;

    @Inject
    private Argon2Utils argon2Utils;

    @Inject
    private EmailService emailService;

    private static final int ACTIVATION_CODE_LENGTH = 6;
    private static final int ACTIVATION_CODE_EXPIRATION_MINUTES = 15; // Increased from 5 to 15 minutes
    private static final int MIN_PASSWORD_LENGTH = 8;

    // Store activation codes temporarily (in production, use Redis or database)
    // Key: activation code, Value: Pair<email, expiration time>
    private final Map<String, Pair<String, LocalDateTime>> activationCodes = new HashMap<>();

    // Reverse lookup: email -> activation code (for resending)
    private final Map<String, String> emailToCode = new HashMap<>();

    /**
     * Register a new identity/user for AirAware
     */
    public void registerIdentity(String username, String password, String email) {
        validateIdentity(username, email);
        validatePassword(password);

        LOGGER.info("Registering new identity: " + username);
        Identity identity = createNewIdentity(username, password, email);
        identityRepository.save(identity);

        // Generate and send activation code
        sendActivationCode(email);
    }

    /**
     * Send or resend activation code to email
     */
    public void sendActivationCode(String email) {
        // Remove old code if exists
        String oldCode = emailToCode.get(email);
        if (oldCode != null) {
            activationCodes.remove(oldCode);
        }

        // Generate new activation code
        String activationCode = generateActivationCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(ACTIVATION_CODE_EXPIRATION_MINUTES);

        // Store the code
        activationCodes.put(activationCode, Pair.of(email, expirationTime));
        emailToCode.put(email, activationCode);

        // Send email
        emailService.sendActivationEmail(email, activationCode);
        LOGGER.info("Activation email sent to: " + email + " (code expires in " + ACTIVATION_CODE_EXPIRATION_MINUTES + " minutes)");
    }

    /**
     * Resend activation code to user
     * @param email The email address to send the code to
     * @return true if code was sent successfully
     */
    public boolean resendActivationCode(String email) {
        // Find identity by email
        Optional<Identity> identityOpt = identityRepository.findByEmail(email);
        if (identityOpt.isEmpty()) {
            throw new EJBException("No account found with email: " + email);
        }

        Identity identity = identityOpt.get();

        // Check if already activated
        if (identity.isAccountActivated()) {
            throw new EJBException("Account is already activated. Please sign in.");
        }

        // Send new activation code
        sendActivationCode(email);
        return true;
    }

    /**
     * Activate identity using activation code
     */
    public void activateIdentity(String code) {
        Pair<String, LocalDateTime> codeDetails = activationCodes.get(code);

        if (codeDetails == null) {
            throw new EJBException("Invalid activation code.");
        }

        String email = codeDetails.getLeft();
        LocalDateTime expirationTime = codeDetails.getRight();

        if (LocalDateTime.now().isAfter(expirationTime)) {
            // Clean up expired code
            activationCodes.remove(code);
            emailToCode.remove(email);
            throw new EJBException("Activation code expired. Please request a new code.");
        }

        Identity identity = identityRepository.findByEmail(email)
                .orElseThrow(() -> new EJBException("Identity associated with the activation code not found."));

        identity.setAccountActivated(true);
        identityRepository.save(identity);

        // Clean up used code
        activationCodes.remove(code);
        emailToCode.remove(email);

        LOGGER.info("Identity activated: " + email);
    }

    /**
     * Get identity by email (useful for auto-login after activation)
     */
    public Optional<Identity> getIdentityByEmail(String email) {
        return identityRepository.findByEmail(email);
    }

    /**
     * Get identity by username
     */
    public Optional<Identity> getIdentityByUsername(String username) {
        return identityRepository.findByUsername(username);
    }

    /**
     * Get identity by ID
     */
    public Identity getIdentityById(String id) {
        return identityRepository.findById(id)
                .orElseThrow(() -> new EJBException("Identity not found with ID: " + id));
    }

    /**
     * Update identity information
     */
    public Identity updateIdentity(String id, String username, String email,
                                   String newPassword, String currentPassword) {
        Identity identity = identityRepository.findById(id)
                .orElseThrow(() -> new EJBException("Identity not found with ID: " + id));

        // Verify current password
        if (!argon2Utils.check(identity.getPassword(), currentPassword.toCharArray())) {
            throw new EJBException("Current password is incorrect.");
        }

        // Update fields
        identity.setUsername(username);
        identity.setEmail(email);

        // Update password if provided
        if (newPassword != null && !newPassword.isEmpty()) {
            validatePassword(newPassword);
            identity.hashPassword(newPassword, argon2Utils);
        }

        identityRepository.save(identity);
        LOGGER.info("Identity updated: " + id);

        return identity;
    }

    /**
     * Delete identity by ID
     */
    public void deleteIdentityById(String id) {
        Identity identity = identityRepository.findById(id)
                .orElseThrow(() -> new EJBException("Identity not found with ID: " + id));
        identityRepository.delete(identity);
        LOGGER.info("Identity deleted: " + id);
    }

    /**
     * Assign role to identity
     */
    public void assignRole(String username, Role role) {
        Identity identity = identityRepository.findByUsername(username)
                .orElseThrow(() -> new EJBException("Identity not found: " + username));

        Long currentRoles = identity.getRoles();
        identity.setRoles(currentRoles | role.getValue());
        identityRepository.save(identity);

        LOGGER.info("Role " + role.name() + " assigned to: " + username);
    }

    /**
     * Remove role from identity
     */
    public void removeRole(String username, Role role) {
        Identity identity = identityRepository.findByUsername(username)
                .orElseThrow(() -> new EJBException("Identity not found: " + username));

        Long currentRoles = identity.getRoles();
        identity.setRoles(currentRoles & ~role.getValue());
        identityRepository.save(identity);

        LOGGER.info("Role " + role.name() + " removed from: " + username);
    }

    // ==================== Private Helper Methods ====================

    private void validateIdentity(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new EJBException("Username is required.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new EJBException("Email is required.");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new EJBException("Invalid email format.");
        }
        if (identityRepository.findByUsername(username).isPresent()) {
            throw new EJBException("Username '" + username + "' already exists.");
        }
        if (identityRepository.findByEmail(email).isPresent()) {
            throw new EJBException("Email '" + email + "' already exists.");
        }
    }

    private Identity createNewIdentity(String username, String password, String email) {
        Identity identity = new Identity();
        identity.setUsername(username);
        identity.setEmail(email);
        identity.setCreationDate(Instant.now());
        identity.setRoles(Role.VIEWER.getValue()); // Default role for new users
        identity.setScopes("sensor:read alert:read dashboard:view");
        identity.hashPassword(password, argon2Utils);
        return identity;
    }

    private void deleteIdentityByEmail(String email) {
        identityRepository.findByEmail(email).ifPresent(identityRepository::delete);
    }

    private String generateActivationCode() {
        String characters = "0123456789";
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder(ACTIVATION_CODE_LENGTH);

        for (int i = 0; i < ACTIVATION_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new EJBException("Password is required.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new EJBException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        // Optional: Add more password requirements
        // if (!password.matches(".*\\d.*")) {
        //     throw new EJBException("Password must contain at least one number.");
        // }
        // if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
        //     throw new EJBException("Password must contain at least one special character.");
        // }
    }
}