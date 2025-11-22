package tn.airaware.iam.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.airaware.iam.enums.Role;

import java.util.HashSet;

/**
 * IAM Repository for AirAware
 * Handles role extraction and authorization checks
 */
@ApplicationScoped
public class IamRepository {

    @Inject
    IdentityRepository identityRepository;

    /**
     * Get all role names for a user
     */
    public String[] getRoles(String username) {
        Long roles = identityRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Identity not found: " + username))
                .getRoles();
        
        HashSet<String> ret = new HashSet<>();
        for (Role role : Role.values()) {
            if ((roles & role.getValue()) != 0L) {
                String value = Role.byValue(role.getValue());
                if (value != null) {
                    ret.add(value);
                }
            }
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String username, Role role) {
        Long userRoles = identityRepository.findByUsername(username)
                .map(identity -> identity.getRoles())
                .orElse(0L);
        return (userRoles & role.getValue()) != 0L;
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String username, Role... roles) {
        Long userRoles = identityRepository.findByUsername(username)
                .map(identity -> identity.getRoles())
                .orElse(0L);
        
        for (Role role : roles) {
            if ((userRoles & role.getValue()) != 0L) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add role to user
     */
    public void addRole(String username, Role role) {
        identityRepository.findByUsername(username).ifPresent(identity -> {
            Long currentRoles = identity.getRoles();
            identity.setRoles(currentRoles | role.getValue());
            identityRepository.save(identity);
        });
    }

    /**
     * Remove role from user
     */
    public void removeRole(String username, Role role) {
        identityRepository.findByUsername(username).ifPresent(identity -> {
            Long currentRoles = identity.getRoles();
            identity.setRoles(currentRoles & ~role.getValue());
            identityRepository.save(identity);
        });
    }
}