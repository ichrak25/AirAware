package tn.airaware.iam.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * Authorization Code with PKCE (Proof Key for Code Exchange)
 * Adapted for AirAware OAuth 2.0 flow
 */
public class AuthorizationCode {
    private static final SecretKey key;
    private static final String codePrefix = "urn:airaware:code:";

    static {
        try {
            key = KeyGenerator.getInstance("CHACHA20").generateKey();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private final String tenantName;
    private final String identityUsername;
    private final String approvedScopes;
    private final Long expirationDate;
    private final String redirectUri;

    public AuthorizationCode(String tenantName, String identityUsername,
                             String approvedScopes, Long expirationDate,
                             String redirectUri) {
        this.tenantName = tenantName;
        this.identityUsername = identityUsername;
        this.approvedScopes = approvedScopes;
        this.expirationDate = expirationDate;
        this.redirectUri = redirectUri;
    }

    public String getCode(String codeChallenge) throws Exception {
        String code = UUID.randomUUID().toString();
        String payload = Base64.getEncoder().withoutPadding().encodeToString(
                (tenantName + ":" + identityUsername + ":" + approvedScopes + ":" + 
                 expirationDate + ":" + redirectUri).getBytes(StandardCharsets.UTF_8));
        String associatedData = codePrefix + code;
        code = codePrefix + code + ":" + payload;
        return code + ":" + Base64.getEncoder().withoutPadding().encodeToString(
                ChaCha20Poly1305.encrypt(codeChallenge.getBytes(), key));
    }

    public static AuthorizationCode decode(String authorizationCode, String codeVerifier) throws Exception {
        int pos = authorizationCode.lastIndexOf(':');
        String code = authorizationCode.substring(0, pos);
        String cipherCodeChallenge = authorizationCode.substring(pos + 1);
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String expected = Base64.getEncoder().withoutPadding().encodeToString(digest.digest());
        
        String decrypted = new String(
                ChaCha20Poly1305.decrypt(Base64.getDecoder().decode(cipherCodeChallenge), key), 
                StandardCharsets.UTF_8).replace('_', '/').replace('-', '+');
        
        if (!expected.equals(decrypted)) {
            return null;
        }
        
        code = code.substring(codePrefix.length());
        pos = code.lastIndexOf(':');
        code = new String(Base64.getDecoder().decode(code.substring(pos + 1)), StandardCharsets.UTF_8);
        String[] attributes = code.split(":");
        
        return new AuthorizationCode(attributes[0], attributes[1], attributes[2],
                Long.parseLong(attributes[3]), attributes[4] + ":" + attributes[5]);
    }

    public String tenantName() { return tenantName; }
    public String identityUsername() { return identityUsername; }
    public String approvedScopes() { return approvedScopes; }
    public Long expirationDate() { return expirationDate; }

    private static class ChaCha20Poly1305 {
        private static final String ENCRYPT_ALGO = "ChaCha20-Poly1305";
        private static final int NONCE_LEN = 12;

        public static byte[] encrypt(byte[] pText, SecretKey key) throws Exception {
            return encrypt(pText, key, getNonce());
        }

        public static byte[] encrypt(byte[] pText, SecretKey key, byte[] nonce) throws Exception {
            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            IvParameterSpec iv = new IvParameterSpec(nonce);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encryptedText = cipher.doFinal(pText);

            return ByteBuffer.allocate(encryptedText.length + NONCE_LEN)
                    .put(encryptedText)
                    .put(nonce)
                    .array();
        }

        public static byte[] decrypt(byte[] cText, SecretKey key) throws Exception {
            ByteBuffer bb = ByteBuffer.wrap(cText);
            byte[] encryptedText = new byte[cText.length - NONCE_LEN];
            byte[] nonce = new byte[NONCE_LEN];
            bb.get(encryptedText);
            bb.get(nonce);

            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            IvParameterSpec iv = new IvParameterSpec(nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            return cipher.doFinal(encryptedText);
        }

        private static byte[] getNonce() {
            byte[] newNonce = new byte[12];
            new SecureRandom().nextBytes(newNonce);
            return newNonce;
        }
    }
}