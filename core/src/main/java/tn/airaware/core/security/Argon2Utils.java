package tn.airaware.core.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

/**
 * Unified Argon2 Password Hashing Utility for AirAware
 * Used across the entire platform (API + IAM modules)
 */
@ApplicationScoped
public class Argon2Utils {
    
    private final Config config;
    private final int saltLength;
    private final int hashLength;
    private final int iterations;
    private final int memory;
    private final int threadNumber;
    private final Argon2 argon2;

    @Inject
    public Argon2Utils(Config config) {
        this.config = config;
        this.saltLength = config.getValue("argon2.saltLength", Integer.class);
        this.hashLength = config.getValue("argon2.hashLength", Integer.class);
        this.iterations = config.getValue("argon2.iterations", Integer.class);
        this.memory = config.getValue("argon2.memory", Integer.class);
        this.threadNumber = config.getValue("argon2.threadNumber", Integer.class);
        this.argon2 = Argon2Factory.create(
                Argon2Factory.Argon2Types.ARGON2id, 
                saltLength, 
                hashLength
        );
    }

    /**
     * Verify password against stored hash
     */
    public boolean check(String dbHash, char[] clientHash) {
        try {
            return argon2.verify(dbHash, clientHash);
        } finally {
            argon2.wipeArray(clientHash);
        }
    }

    /**
     * Hash a plain text password
     */
    public String hash(char[] clientHash) {
        try {
            return argon2.hash(iterations, memory, threadNumber, clientHash);
        } finally {
            argon2.wipeArray(clientHash);
        }
    }
}