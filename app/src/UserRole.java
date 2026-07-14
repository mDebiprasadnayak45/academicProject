/**
 * User role enumeration for the Face Recognition Attendance System.
 * Defines the available user roles and replaces hardcoded string checks.
 * 
 * @author Face Recognition Team
 * @version 2.0
 */
public enum UserRole {
    /**
     * Student role - can mark attendance and view own records
     */
    STUDENT("STUDENT"),

    /**
     * Employee role - can mark attendance and view own records
     */
    EMPLOYEE("EMPLOYEE"),

    /**
     * Teacher role - can manage students and view records
     */
    TEACHER("TEACHER"),

    /**
     * Administrator role - full system access
     */
    ADMIN("ADMIN");

    private final String displayName;

    /**
     * Constructs a UserRole with a display name.
     * 
     * @param displayName String representation of the role
     */
    UserRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the string representation of this role.
     * 
     * @return The display name of this role
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string to find the corresponding UserRole.
     * 
     * @param value The string to parse (case-insensitive)
     * @return The corresponding UserRole, or null if not found
     */
    public static UserRole fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UserRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Checks if a string is a valid role.
     * 
     * @param value The string to check
     * @return true if the string represents a valid role, false otherwise
     */
    public static boolean isValidRole(String value) {
        return fromString(value) != null;
    }
}
