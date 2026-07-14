/**
 * Input Validation Utilities for the Face Recognition Attendance System.
 * Provides comprehensive validation for user input data including user IDs,
 * names, emails, passwords, dates, and other application-specific fields.
 * Validates input format and ensures data integrity.
 * NOTE: SQL injection prevention relies on PreparedStatement, not validation.
 * 
 * @author Face Recognition Team
 * @version 2.0
 */

public class ValidationUtils {

    private static final java.time.format.DateTimeFormatter ISO_DATE = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
    private static final java.time.format.DateTimeFormatter ISO_TIME = java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

    /**
     * Normalizes user ID to uppercase for consistency.
     * 
     * @param userId The user ID to normalize
     * @return Uppercase user ID, or null if input is null
     */
    public static String normalizeUserId(String userId) {
        return userId == null ? null : userId.trim().toUpperCase();
    }

    /**
     * Normalizes email to lowercase for consistency.
     * 
     * @param email The email to normalize
     * @return Lowercase email, or null if input is null
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /**
     * Validates user ID format.
     * Must be 4-20 alphanumeric characters (case-insensitive).
     * Whitespace is trimmed before validation.
     * 
     * @param userId The user ID to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidUserId(String userId) {
        return userId != null && userId.trim().matches("^[A-Za-z0-9]{4,20}$");
    }

    /**
     * Validates user ID format based on role.
     * - STUDENT: format YYITMNNN (example: 23ITM001)
     * - TEACHER: format YYITMNN (example: 23ITM01)
     * - ADMIN: format ADMINNN where NN are digits (example: ADMIN01)
     * - EMPLOYEE: format EMPNNN (example: EMP001)
     *
     * @param userId The user ID to validate
     * @param role   The selected user role
     * @return true if valid for the role, false otherwise
     */
    public static boolean isValidUserIdForRole(String userId, String role) {
        if (userId == null || role == null) {
            return false;
        }

        String trimmedUserId = userId.trim();
        String trimmedRole = role.trim().toUpperCase();

        if ("STUDENT".equals(trimmedRole)) {
            return trimmedUserId.matches("(?i)^\\d{2}ITM\\d{3}$");
        }
        if ("TEACHER".equals(trimmedRole)) {
            return trimmedUserId.matches("(?i)^\\d{2}ITM\\d{2}$");
        }
        if ("ADMIN".equals(trimmedRole)) {
            return trimmedUserId.matches("(?i)^ADMIN\\d{2}$");
        }
        if ("EMPLOYEE".equals(trimmedRole)) {
            return trimmedUserId.matches("(?i)^EMP\\d{3}$");
        }

        return false;
    }

    /**
     * Returns the role-specific label used for user identifiers in forms.
     *
     * @param role Selected role
     * @return Human-readable label for the role's identifier
     */
    public static String getUserIdLabelForRole(String role) {
        if (role == null) {
            return "User ID";
        }
        String normalized = role.trim().toUpperCase();
        if ("STUDENT".equals(normalized)) {
            return "Roll No.";
        }
        if ("TEACHER".equals(normalized)) {
            return "Teacher ID";
        }
        if ("ADMIN".equals(normalized)) {
            return "Admin ID";
        }
        if ("EMPLOYEE".equals(normalized)) {
            return "Employee ID";
        }
        return "User ID";
    }

    /**
     * Validates name format.
     * Must be 2-100 characters containing letters, spaces, dots, hyphens, and
     * apostrophes.
     * Examples: "John Smith", "Mary-Jane", "Dr. Johnson", "Raj O'Brien", "M. A.
     * Gupta"
     * Whitespace is trimmed before validation.
     * 
     * @param name The name to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidName(String name) {
        return name != null && name.trim().matches("^[a-zA-Z\\s.\\'-]{2,100}$");
    }

    /**
     * Validates email address format.
     * Whitespace is trimmed before validation.
     * 
     * @param email The email address to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidEmail(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null) {
            return false;
        }
        return normalized.length() <= 254
                && normalized.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
    }

    /**
     * Validates password strength requirements.
     * Password must be at least 8 characters with:
     * - At least one uppercase letter (A-Z)
     * - At least one lowercase letter (a-z)
     * - At least one digit (0-9)
     * - At least one special character (!@#$%^&*...)
     * 
     * @param password The password to validate
     * @return true if password meets all requirements, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < AppConfig.PASSWORD_MIN_LENGTH) {
            return false;
        }
        return password.matches(".*[A-Z].*") && // Has uppercase
                password.matches(".*[a-z].*") && // Has lowercase
                password.matches(".*\\d.*") && // Has digit
                password.matches(".*[^A-Za-z0-9].*"); // Has special character
    }

    /**
     * Validates date format (YYYY-MM-DD).
     * 
     * @param date The date string to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidDateFormat(String date) {
        if (date == null) {
            return false;
        }
        try {
            java.time.LocalDate.parse(date, ISO_DATE);
            return true;
        } catch (java.time.format.DateTimeParseException ex) {
            return false;
        }
    }

    /**
     * Validates time format (HH:MM:SS).
     * Validates that the time represents a valid 24-hour time, not just regex
     * format.
     * 
     * @param time The time string to validate
     * @return true if valid time, false otherwise
     */
    public static boolean isValidTimeFormat(String time) {
        if (time == null) {
            return false;
        }
        try {
            java.time.LocalTime.parse(time, ISO_TIME);
            return true;
        } catch (java.time.format.DateTimeParseException ex) {
            return false;
        }
    }

    /**
     * Validates OTP format (6 digits).
     * 
     * @param otp The OTP to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidOTP(String otp) {
        return otp != null && otp.matches("^\\d{6}$");
    }

    /**
     * Validates user role.
     * Must be one of the values defined in UserRole enum: STUDENT, EMPLOYEE,
     * TEACHER, or ADMIN.
     * Role check is case-insensitive.
     * 
     * @param role The role to validate
     * @return true if valid role, false otherwise
     */
    public static boolean isValidRole(String role) {
        return role != null && UserRole.isValidRole(role.trim());
    }

    /**
     * @deprecated Do not use for SQL injection prevention. Use PreparedStatement
     *             instead.
     *             This method is retained only for backward compatibility and does
     *             NOT modify input.
     * 
     * @param input The input string
     * @return Original input (or empty string if null)
     */
    @Deprecated
    public static String sanitizeInput(String input) {
        return input == null ? "" : input;
    }

    /**
     * Validates string length is within specified range.
     * Whitespace is trimmed before length check.
     * 
     * @param input     The string to validate
     * @param minLength Minimum allowed length
     * @param maxLength Maximum allowed length
     * @return true if length is within range, false otherwise
     */
    public static boolean isValidLength(String input, int minLength, int maxLength) {
        return input != null && input.trim().length() >= minLength && input.trim().length() <= maxLength;
    }

    /**
     * Validates confidence score is within valid range (0.0 to 1.0).
     * 
     * @param score The confidence score to validate
     * @return true if score is between 0.0 and 1.0 inclusive, false otherwise
     */
    public static boolean isValidConfidenceScore(float score) {
        return score >= 0.0f && score <= 1.0f;
    }

    /**
     * Performs comprehensive validation of user registration data.
     * Checks all fields for format correctness and returns detailed error messages.
     * 
     * @param userId   User's unique identifier
     * @param name     User's full name
     * @param email    User's email address
     * @param password User's password
     * @param role     User's role
     * @return ValidationResult containing validation status and any error messages
     */
    public static ValidationResult validateUserData(String userId, String name, String email, String password,
            String role) {
        ValidationResult result = new ValidationResult();

        if (!isValidUserIdForRole(userId != null ? userId.trim() : null, role != null ? role.trim() : null)) {
            String normalizedRole = role == null ? "" : role.trim().toUpperCase();
            if ("STUDENT".equals(normalizedRole)) {
                result.addError("Student Roll No. must be in format YYITMNNN (e.g., 23ITM001)");
            } else if ("TEACHER".equals(normalizedRole)) {
                result.addError("Teacher ID must be in format YYITMNN (e.g., 23ITM01)");
            } else if ("ADMIN".equals(normalizedRole)) {
                result.addError("Admin ID must be in format ADMIN01");
            } else if ("EMPLOYEE".equals(normalizedRole)) {
                result.addError("Employee ID must be in format EMP001");
            } else {
                result.addError("Invalid role-based ID format");
            }
        }

        if (!isValidName(name != null ? name.trim() : null)) {
            result.addError("Name must be 2-100 characters (letters and spaces only)");
        }

        if (!isValidEmail(normalizeEmail(email))) {
            result.addError("Invalid email format");
        }

        if (!isStrongPassword(password)) {
            result.addError(
                    "Password must be at least 8 characters with uppercase, lowercase, digit, and special character");
        }

        if (!isValidRole(role != null ? role.trim() : null)) {
            result.addError("Invalid role. Must be STUDENT, EMPLOYEE, TEACHER, or ADMIN");
        }

        return result;
    }

    /**
     * Container class for validation results.
     * Holds validation status and accumulated error messages.
     */
    public static class ValidationResult {
        private java.util.List<String> errors = new java.util.ArrayList<>();

        /**
         * Adds an error message to the validation result.
         * 
         * @param error The error message to add
         */
        public void addError(String error) {
            errors.add(error);
        }

        /**
         * Checks if validation passed with no errors.
         * 
         * @return true if valid (no errors), false otherwise
         */
        public boolean isValid() {
            return errors.isEmpty();
        }

        /**
         * Gets an unmodifiable copy of all error messages.
         * Prevents external code from modifying the internal error list.
         * 
         * @return Unmodifiable list of error messages
         */
        public java.util.List<String> getErrors() {
            return java.util.Collections.unmodifiableList(errors);
        }

        /**
         * Gets all error messages as a single formatted string.
         * 
         * @return Error messages joined by newlines
         */
        public String getErrorMessage() {
            return String.join("\n", errors);
        }
    }
}
