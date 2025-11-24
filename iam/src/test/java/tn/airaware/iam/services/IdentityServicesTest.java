package tn.airaware.iam.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.airaware.core.entities.Identity;
import tn.airaware.iam.repositories.IdentityRepository;
import tn.airaware.core.security.Argon2Utils;

import jakarta.ejb.EJBException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IdentityServicesTest {

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private Argon2Utils argon2Utils;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private IdentityServices identityServices;

    @Test
    void testRegisterIdentity_Success() {
        // Given
        String username = "testuser";
        String password = "Password123!";
        String email = "test@example.com";

        when(identityRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(identityRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(argon2Utils.hash(any(char[].class))).thenReturn("hashedPassword");

        // When
        identityServices.registerIdentity(username, password, email);

        // Then
        ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
        verify(identityRepository, times(1)).save(identityCaptor.capture());
        Identity savedIdentity = identityCaptor.getValue();
        assertEquals(username, savedIdentity.getUsername());
        assertEquals(email, savedIdentity.getEmail());

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendActivationEmail(emailCaptor.capture(), codeCaptor.capture());
        assertEquals(email, emailCaptor.getValue());
        assertNotNull(codeCaptor.getValue());
        assertEquals(6, codeCaptor.getValue().length());
    }

    @Test
    void testRegisterIdentity_UsernameAlreadyExists() {
        // Given
        String username = "testuser";
        String password = "Password123!";
        String email = "test@example.com";
        Identity existingIdentity = new Identity();
        existingIdentity.setUsername(username);

        when(identityRepository.findByUsername(username)).thenReturn(Optional.of(existingIdentity));

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.registerIdentity(username, password, email);
        });
        assertEquals("Username '" + username + "' already exists.", exception.getMessage());
    }

    @Test
    void testRegisterIdentity_EmailAlreadyExists() {
        // Given
        String username = "testuser";
        String password = "Password123!";
        String email = "test@example.com";
        Identity existingIdentity = new Identity();
        existingIdentity.setEmail(email);

        when(identityRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(identityRepository.findByEmail(email)).thenReturn(Optional.of(existingIdentity));

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.registerIdentity(username, password, email);
        });
        assertEquals("Email '" + email + "' already exists.", exception.getMessage());
    }

    @Test
    void testRegisterIdentity_PasswordTooShort() {
        // Given
        String username = "testuser";
        String password = "Pwd1!";
        String email = "test@example.com";

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.registerIdentity(username, password, email);
        });
        assertEquals("Password must be at least 8 characters long.", exception.getMessage());
    }

    @Test
    void testRegisterIdentity_PasswordMissingNumber() {
        // Given
        String username = "testuser";
        String password = "Password!";
        String email = "test@example.com";

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.registerIdentity(username, password, email);
        });
        assertEquals("Password must contain at least one number.", exception.getMessage());
    }

    @Test
    void testRegisterIdentity_PasswordMissingSpecialChar() {
        // Given
        String username = "testuser";
        String password = "Password123";
        String email = "test@example.com";

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.registerIdentity(username, password, email);
        });
        assertEquals("Password must contain at least one special character.", exception.getMessage());
    }

    @Test
    void testActivateIdentity_Success() throws Exception {
        // Given
        String email = "test@example.com";
        String activationCode = "123456";
        Identity identity = new Identity();
        identity.setEmail(email);
        identity.setAccountActivated(false);

        // Use reflection to add an activation code to the map
        java.lang.reflect.Field activationCodesField = IdentityServices.class.getDeclaredField("activationCodes");
        activationCodesField.setAccessible(true);
        java.util.Map<String, org.apache.commons.lang3.tuple.Pair<String, java.time.LocalDateTime>> activationCodes =
                (java.util.Map<String, org.apache.commons.lang3.tuple.Pair<String, java.time.LocalDateTime>>) activationCodesField.get(identityServices);
        activationCodes.put(activationCode, org.apache.commons.lang3.tuple.Pair.of(email, java.time.LocalDateTime.now().plusMinutes(5)));

        when(identityRepository.findByEmail(email)).thenReturn(Optional.of(identity));

        // When
        identityServices.activateIdentity(activationCode);

        // Then
        assertTrue(identity.isAccountActivated());
        verify(identityRepository, times(1)).save(identity);
    }

    @Test
    void testActivateIdentity_InvalidCode() {
        // Given
        String activationCode = "invalidCode";

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.activateIdentity(activationCode);
        });
        assertEquals("Invalid activation code.", exception.getMessage());
    }

    @Test
    void testActivateIdentity_ExpiredCode() throws Exception {
        // Given
        String email = "test@example.com";
        String activationCode = "123456";

        // Use reflection to add an expired activation code to the map
        java.lang.reflect.Field activationCodesField = IdentityServices.class.getDeclaredField("activationCodes");
        activationCodesField.setAccessible(true);
        java.util.Map<String, org.apache.commons.lang3.tuple.Pair<String, java.time.LocalDateTime>> activationCodes =
                (java.util.Map<String, org.apache.commons.lang3.tuple.Pair<String, java.time.LocalDateTime>>) activationCodesField.get(identityServices);
        activationCodes.put(activationCode, org.apache.commons.lang3.tuple.Pair.of(email, java.time.LocalDateTime.now().minusMinutes(10)));

        when(identityRepository.findByEmail(email)).thenReturn(Optional.of(new Identity()));

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.activateIdentity(activationCode);
        });
        assertEquals("Activation code expired. Please register again.", exception.getMessage());
        verify(identityRepository, times(1)).delete(any(Identity.class));
    }

    @Test
    void testUpdateIdentity_Success() {
        // Given
        String id = "123";
        String username = "updateduser";
        String email = "updated@example.com";
        String newPassword = "newPassword123!";
        String currentPassword = "currentPassword";
        Identity identity = new Identity();
        identity.setId(id);
        identity.setPassword("hashedCurrentPassword");

        when(identityRepository.findById(id)).thenReturn(Optional.of(identity));
        when(argon2Utils.check("hashedCurrentPassword", currentPassword.toCharArray())).thenReturn(true);
        when(argon2Utils.hash(newPassword.toCharArray())).thenReturn("hashedNewPassword");

        // When
        Identity updatedIdentity = identityServices.updateIdentity(id, username, email, newPassword, currentPassword);

        // Then
        assertEquals(username, updatedIdentity.getUsername());
        assertEquals(email, updatedIdentity.getEmail());
        assertEquals("hashedNewPassword", updatedIdentity.getPassword());
        verify(identityRepository, times(1)).save(identity);
    }

    @Test
    void testUpdateIdentity_IncorrectCurrentPassword() {
        // Given
        String id = "123";
        String username = "updateduser";
        String email = "updated@example.com";
        String newPassword = "newPassword123!";
        String currentPassword = "incorrectPassword";
        Identity identity = new Identity();
        identity.setId(id);
        identity.setPassword("hashedCurrentPassword");

        when(identityRepository.findById(id)).thenReturn(Optional.of(identity));
        when(argon2Utils.check("hashedCurrentPassword", currentPassword.toCharArray())).thenReturn(false);

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.updateIdentity(id, username, email, newPassword, currentPassword);
        });
        assertEquals("Current password is incorrect.", exception.getMessage());
    }

    @Test
    void testDeleteIdentityById_Success() {
        // Given
        String id = "123";
        Identity identity = new Identity();
        identity.setId(id);

        when(identityRepository.findById(id)).thenReturn(Optional.of(identity));

        // When
        identityServices.deleteIdentityById(id);

        // Then
        verify(identityRepository, times(1)).delete(identity);
    }

    @Test
    void testDeleteIdentityById_NotFound() {
        // Given
        String id = "123";

        when(identityRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        EJBException exception = assertThrows(EJBException.class, () -> {
            identityServices.deleteIdentityById(id);
        });
        assertEquals("Identity not found with ID: " + id, exception.getMessage());
    }

    @Test
    void testAssignRole_Success() {
        // Given
        String username = "testuser";
        Identity identity = new Identity();
        identity.setUsername(username);
        identity.setRoles(tn.airaware.iam.enums.Role.VIEWER.getValue());

        when(identityRepository.findByUsername(username)).thenReturn(Optional.of(identity));

        // When
        identityServices.assignRole(username, tn.airaware.iam.enums.Role.SYSTEM_ADMIN);

        // Then
        long expectedRoles = tn.airaware.iam.enums.Role.VIEWER.getValue() | tn.airaware.iam.enums.Role.SYSTEM_ADMIN.getValue();
        assertEquals(expectedRoles, identity.getRoles());
        verify(identityRepository, times(1)).save(identity);
    }

    @Test
    void testRemoveRole_Success() {
        // Given
        String username = "testuser";
        Identity identity = new Identity();
        identity.setUsername(username);
        identity.setRoles(tn.airaware.iam.enums.Role.VIEWER.getValue() | tn.airaware.iam.enums.Role.SYSTEM_ADMIN.getValue());

        when(identityRepository.findByUsername(username)).thenReturn(Optional.of(identity));

        // When
        identityServices.removeRole(username, tn.airaware.iam.enums.Role.SYSTEM_ADMIN);

        // Then
        assertEquals(tn.airaware.iam.enums.Role.VIEWER.getValue(), identity.getRoles());
        verify(identityRepository, times(1)).save(identity);
    }
}
