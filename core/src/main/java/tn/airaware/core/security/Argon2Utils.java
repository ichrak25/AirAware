package tn.airaware.core.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class Argon2Utils {

    @Inject
    private Config config;

    private int saltLength;
    private int hashLength;
    private int iterations;
    private int memory;
    private int threadNumber;
    private Argon2 argon2;

    // Constructeur sans argument REQUIS pour CDI proxy
    public Argon2Utils() {
    }

    @PostConstruct
    public void init() {
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

    public boolean check(String dbHash, char[] clientHash) {
        try {
            return argon2.verify(dbHash, clientHash);
        } finally {
            argon2.wipeArray(clientHash);
        }
    }

    public String hash(char[] clientHash) {
        try {
            return argon2.hash(iterations, memory, threadNumber, clientHash);
        } finally {
            argon2.wipeArray(clientHash);
        }
    }
}