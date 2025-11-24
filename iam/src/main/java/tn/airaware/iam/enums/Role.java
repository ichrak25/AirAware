package tn.airaware.iam.enums;

import org.eclipse.microprofile.config.ConfigProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Role enumeration for AirAware IAM
 * Adapted for air quality monitoring system with tenant-based access control
 */
public enum Role {
    GUEST(0L),

    // AirAware Specific Roles
    SYSTEM_ADMIN(1L),              // Full system access
    TENANT_ADMIN(1L << 1L),        // Manage organization sensors and users
    SENSOR_OPERATOR(1L << 2L),     // Deploy and configure sensors
    DATA_ANALYST(1L << 3L),        // Access to data analysis and reports
    ALERT_MANAGER(1L << 4L),       // Manage alerts and thresholds
    VIEWER(1L << 5L),              // Read-only access to dashboards
    API_CLIENT(1L << 6L),          // Programmatic API access

    // Extended permission slots (56 more slots for future use)
    R_P07(1L << 7L), R_P08(1L << 8L), R_P09(1L << 9L), R_P10(1L << 10L),
    R_P11(1L << 11L), R_P12(1L << 12L), R_P13(1L << 13L), R_P14(1L << 14L), R_P15(1L << 15L),
    R_P16(1L << 16L), R_P17(1L << 17L), R_P18(1L << 18L), R_P19(1L << 19L), R_P20(1L << 20L),
    R_P21(1L << 21L), R_P22(1L << 22L), R_P23(1L << 23L), R_P24(1L << 24L), R_P25(1L << 25L),
    R_P26(1L << 26L), R_P27(1L << 27L), R_P28(1L << 28L), R_P29(1L << 29L), R_P30(1L << 30L),
    R_P31(1L << 31L), R_P32(1L << 32L), R_P33(1L << 33L), R_P34(1L << 34L), R_P35(1L << 35L),
    R_P36(1L << 36L), R_P37(1L << 37L), R_P38(1L << 38L), R_P39(1L << 39L), R_P40(1L << 40L),
    R_P41(1L << 41L), R_P42(1L << 42L), R_P43(1L << 43L), R_P44(1L << 44L), R_P45(1L << 45L),
    R_P46(1L << 46L), R_P47(1L << 47L), R_P48(1L << 48L), R_P49(1L << 49L), R_P50(1L << 50L),
    R_P51(1L << 51L), R_P52(1L << 52L), R_P53(1L << 53L), R_P54(1L << 54L), R_P55(1L << 55L),
    R_P56(1L << 56L), R_P57(1L << 57L), R_P58(1L << 58L), R_P59(1L << 59L), R_P60(1L << 60L),
    R_P61(1L << 61L), R_P62(1L << 62L),

    ROOT(Long.MAX_VALUE);  // Super admin

    private final long value;

    Role(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    private static final Map<Long, String> ids = new LinkedHashMap<>();
    private static final Map<String, Role> byIds = new LinkedHashMap<>();

    static {
        final AtomicLong id = new AtomicLong(1L);
        List<String> customRoles = ConfigProvider.getConfig().getOptionalValues("airaware.roles", String.class)
                .orElse(List.of());

        if (customRoles.stream().anyMatch(r -> r.equalsIgnoreCase(GUEST.name()) || r.equalsIgnoreCase(ROOT.name()))
                || customRoles.size() > 62) {
            throw new IllegalArgumentException("Illegal config value for roles");
        }

        ids.putAll(customRoles.stream().collect(Collectors.toMap(
                x -> id.getAndUpdate(y -> 2L * y), Function.identity())));
        ids.put(GUEST.value, GUEST.name().toLowerCase());
        ids.put(ROOT.value, ROOT.name().toLowerCase());

        final AtomicInteger ordinal = new AtomicInteger(1);
        final Role[] values = Role.values();
        byIds.put(GUEST.name().toLowerCase(), GUEST);
        byIds.put(ROOT.name().toLowerCase(), ROOT);
        byIds.putAll(customRoles.stream().collect(Collectors.toMap(
                Function.identity(), x -> values[ordinal.getAndIncrement()])));
    }

    public final String id() {
        return ids.get(value);
    }

    public static String byValue(Long value) {
        return ids.get(value);
    }

    public static Role byId(String id) {
        return byIds.get(id);
    }

    // ==================== Utility Methods for Role Operations ====================

    /**
     * Combine multiple roles using bitwise OR operation
     * @param roles Variable number of roles to combine
     * @return The combined bitwise value of all roles
     *
     * Example:
     * long combinedRoles = Role.combine(Role.VIEWER, Role.DATA_ANALYST, Role.SENSOR_OPERATOR);
     */
    public static long combine(Role... roles) {
        long combined = 0L;
        for (Role role : roles) {
            combined |= role.getValue();
        }
        return combined;
    }

    /**
     * Check if a combined role value contains a specific role
     * @param combinedRoles The combined bitwise value to check
     * @param role The role to check for
     * @return true if the role is present in the combined value
     *
     * Example:
     * long userRoles = Role.combine(Role.VIEWER, Role.DATA_ANALYST);
     * boolean isViewer = Role.hasRole(userRoles, Role.VIEWER); // true
     */
    public static boolean hasRole(long combinedRoles, Role role) {
        // Special case for ROOT - it has all permissions
        if (combinedRoles == ROOT.getValue()) {
            return true;
        }
        // GUEST has no permissions
        if (role == GUEST) {
            return combinedRoles == GUEST.getValue();
        }
        return (combinedRoles & role.getValue()) == role.getValue();
    }

    /**
     * Check if a combined role value contains ALL of the specified roles
     * @param combinedRoles The combined bitwise value to check
     * @param requiredRoles The roles that must all be present
     * @return true if ALL required roles are present
     */
    public static boolean hasAllRoles(long combinedRoles, Role... requiredRoles) {
        // Special case for ROOT - it has all permissions
        if (combinedRoles == ROOT.getValue()) {
            return true;
        }

        for (Role role : requiredRoles) {
            if (!hasRole(combinedRoles, role)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a combined role value contains ANY of the specified roles
     * @param combinedRoles The combined bitwise value to check
     * @param allowedRoles The roles to check for (any match is sufficient)
     * @return true if ANY of the allowed roles are present
     */
    public static boolean hasAnyRole(long combinedRoles, Role... allowedRoles) {
        // Special case for ROOT - it has all permissions
        if (combinedRoles == ROOT.getValue()) {
            return true;
        }

        for (Role role : allowedRoles) {
            if (hasRole(combinedRoles, role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a role to an existing combined roles value
     * @param combinedRoles The existing combined roles
     * @param roleToAdd The role to add
     * @return The new combined value with the role added
     */
    public static long addRole(long combinedRoles, Role roleToAdd) {
        return combinedRoles | roleToAdd.getValue();
    }

    /**
     * Remove a role from an existing combined roles value
     * @param combinedRoles The existing combined roles
     * @param roleToRemove The role to remove
     * @return The new combined value with the role removed
     */
    public static long removeRole(long combinedRoles, Role roleToRemove) {
        return combinedRoles & ~roleToRemove.getValue();
    }

    /**
     * Get a human-readable string representation of combined roles
     * @param combinedRoles The combined bitwise value
     * @return Comma-separated list of role names
     */
    public static String toString(long combinedRoles) {
        if (combinedRoles == ROOT.getValue()) {
            return "ROOT";
        }
        if (combinedRoles == GUEST.getValue()) {
            return "GUEST";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Role role : Role.values()) {
            if (role != ROOT && role != GUEST && hasRole(combinedRoles, role)) {
                if (!first) {
                    sb.append(", ");
                }
                String roleName = ids.get(role.getValue());
                sb.append(roleName != null ? roleName.toUpperCase() : role.name());
                first = false;
            }
        }

        return sb.length() > 0 ? sb.toString() : "NONE";
    }

    /**
     * Parse a role name string to a Role enum
     * @param roleName The name of the role (case-insensitive)
     * @return The corresponding Role enum, or null if not found
     */
    public static Role fromString(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return null;
        }

        // Try direct enum lookup
        try {
            return Role.valueOf(roleName.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // Try custom role lookup
            return byId(roleName.toLowerCase().trim());
        }
    }

    /**
     * Get all standard AirAware roles (excluding GUEST, ROOT, and placeholder roles)
     * @return Array of standard roles
     */
    public static Role[] getStandardRoles() {
        return new Role[]{
                SYSTEM_ADMIN,
                TENANT_ADMIN,
                SENSOR_OPERATOR,
                DATA_ANALYST,
                ALERT_MANAGER,
                VIEWER,
                API_CLIENT
        };
    }

    @Override
    public String toString() {
        String roleName = ids.get(value);
        return (roleName != null ? roleName.toUpperCase() : name()) + " (" + value + ")";
    }
}