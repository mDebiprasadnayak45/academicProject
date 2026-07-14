import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Management System for the Face Recognition Attendance Application.
 * Handles user registration, authentication, profile management, and face data
 * storage.
 * All passwords are securely hashed using SHA-256 with salt before storage.
 * 
 * @author Face Recognition Team
 * @version 2.0
 */
public class UserManager {

    private static final int MIN_BATCH_YEAR = 2023;
    private static final int MAX_BATCH_YEAR = 2029;

    private static String normalizeEmail(String email) {
        return ValidationUtils.normalizeEmail(email);
    }

    private static String normalizeUserId(String userId) {
        return ValidationUtils.normalizeUserId(userId);
    }

    private static String normalizeRole(String role) {
        return role == null ? null : role.trim().toUpperCase();
    }

    private static boolean isBatchYearValid(Integer batchYear) {
        return batchYear != null && batchYear >= MIN_BATCH_YEAR && batchYear <= MAX_BATCH_YEAR;
    }

    private static String normalizeSubject(String subject) {
        return subject == null ? null : subject.trim();
    }

    /**
     * Registers a new user in the system with secure password hashing.
     * Validates user data before registration and checks for duplicate users.
     * Passwords are hashed using SHA-256 with salt via SecurityUtils.
     * 
     * @param id       Unique user identifier (4-20 alphanumeric characters)
     * @param name     User's full name
     * @param email    User's email address (must be unique)
     * @param password Plain text password (will be hashed before storage)
     * @param role     User role (STUDENT, EMPLOYEE, or ADMIN)
     * @return true if registration successful, false otherwise
     */
    public static boolean registerUser(String id, String name, String email, String password, String role) {
        return registerUser(id, name, email, password, role, null, null, null, null);
    }

    /**
     * Registers a new user with optional security question/answer for password
     * recovery.
     */
    public static boolean registerUser(String id, String name, String email, String password, String role,
            String securityQuestion, String securityAnswer) {
        return registerUser(id, name, email, password, role, securityQuestion, securityAnswer, null, null);
    }

    /**
     * Registers a new user with optional security question and batch/subject
     * metadata.
     */
    public static boolean registerUser(String id, String name, String email, String password, String role,
            String securityQuestion, String securityAnswer, Integer batchYear, String subject) {
        id = normalizeUserId(id);
        email = normalizeEmail(email);
        role = normalizeRole(role);
        name = name == null ? null : name.trim();
        subject = normalizeSubject(subject);

        boolean requiresBatch = "STUDENT".equals(role) || "TEACHER".equals(role);
        Integer normalizedBatchYear = isBatchYearValid(batchYear) ? batchYear : null;

        if ("STUDENT".equals(role)) {
            System.out.println("Validation failed: Student registration requires face data");
            return false;
        }

        if (requiresBatch && normalizedBatchYear == null) {
            System.out.println("Validation failed: Invalid or missing batch year");
            return false;
        }

        if ("TEACHER".equals(role) && (subject == null || subject.isEmpty())) {
            System.out.println("Validation failed: Teacher registration requires subject");
            return false;
        }

        boolean teacherHasBatchMapping = "TEACHER".equals(role) && normalizedBatchYear != null;

        // Validate input data using ValidationUtils
        ValidationUtils.ValidationResult validation = ValidationUtils.validateUserData(id, name, email, password, role);
        if (!validation.isValid()) {
            System.out.println("Validation failed: " + validation.getErrorMessage());
            return false;
        }

        // Enforce role+batch ID code uniqueness on backend as well.
        if (("STUDENT".equals(role) || "TEACHER".equals(role))
                && normalizedBatchYear != null
                && !isRoleCodeAvailableInBatch(id, role, normalizedBatchYear)) {
            System.out.println("Validation failed: user ID code already taken for this role and batch");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE id = ? OR email = ?")) {

                // Check if user already exists
                checkStmt.setString(1, id);
                checkStmt.setString(2, email);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("User ID or email already exists!");
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Hash password securely before storing
            String passwordHash = SecurityUtils.hashPassword(password);
            String securityAnswerHash = (securityAnswer == null || securityAnswer.trim().isEmpty())
                    ? null
                    : SecurityUtils.hashPassword(securityAnswer.trim().toLowerCase());

            // Insert new user with hashed password
            String sql = "INSERT INTO users (id, name, email, password_hash, security_question, security_answer_hash, batch_year, subject, role) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.setString(2, name);
                stmt.setString(3, email);
                stmt.setString(4, passwordHash);
                stmt.setString(5,
                        securityQuestion == null || securityQuestion.trim().isEmpty() ? null : securityQuestion.trim());
                stmt.setString(6, securityAnswerHash);
                if (requiresBatch && normalizedBatchYear != null) {
                    stmt.setInt(7, normalizedBatchYear);
                } else {
                    stmt.setNull(7, java.sql.Types.INTEGER);
                }
                stmt.setString(8, "TEACHER".equals(role) ? subject : null);
                stmt.setString(9, role);

                int result = stmt.executeUpdate();

                if (result > 0) {
                    if (teacherHasBatchMapping
                            && !upsertTeacherBatchSubject(conn, id, normalizedBatchYear, subject)) {
                        conn.rollback();
                        return false;
                    }
                    conn.commit();
                    System.out.println("User registered successfully: " + name);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error restoring autocommit: " + e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * SECURE AUTHENTICATION: Authenticates user login using hashed password
     * verification.
     * CRITICAL: Passwords are NEVER compared in plain text.
     * Stored password_hash is extracted and compared using
     * SecurityUtils.verifyPassword() only.
     * 
     * @param email    User's email address
     * @param password Plain text password provided at login (will be hashed for
     *                 comparison)
     * @return Map with success status, userId, name, role if authentication
     *         succeeds
     */
    public static Map<String, Object> authenticateUser(String email, String password) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);

        email = normalizeEmail(email);

        // Validate email format
        if (!ValidationUtils.isValidEmail(email)) {
            System.out.println("[ERROR] Invalid email format");
            return result;
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT id, name, role, password_hash FROM users WHERE email = ? AND is_active = 1")) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPasswordHash = rs.getString("password_hash");
                    String userId = rs.getString("id");

                    // SECURITY CHECK: Use SecurityUtils to verify password against hash
                    // This ensures passwords are NEVER compared as plain text
                    // SecurityUtils.verifyPassword() safely extracts salt and recomputes hash
                    if (SecurityUtils.verifyPassword(password, storedPasswordHash)) {
                        System.out.println("[OK] Authentication successful for user: " + userId);
                        result.put("success", true);
                        result.put("userId", userId);
                        result.put("name", rs.getString("name"));
                        result.put("role", rs.getString("role"));

                        // Log successful login
                        logLoginAttempt(userId, true, "127.0.0.1");
                    } else {
                        System.out.println("[ERROR] Authentication failed: Password mismatch for " + email);
                        logLoginAttempt(userId, false, "127.0.0.1");
                    }
                } else {
                    System.out.println("[ERROR] Authentication failed: User not found - " + email);
                    logLoginAttempt(null, false, "127.0.0.1");
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Database error during authentication: " + e.getMessage());
        }
        return result;
    }

    /**
     * Registers a user and stores their face data in an atomic transaction.
     * Both operations must succeed together, or the entire transaction is rolled
     * back.
     * This prevents orphaned user records without face data.
     * 
     * @param id       Unique user identifier
     * @param name     User's full name
     * @param email    User's email address
     * @param password User's password (will be hashed)
     * @param role     User's role
     * @param faceData Binary face data from OpenCV
     * @return true if registration and face storage both succeed, false otherwise
     */
    public static boolean registerUserWithFaceData(String id, String name, String email, String password, String role,
            byte[] faceData) {
        return registerUserWithFaceData(id, name, email, password, role, faceData, null, null, null, null);
    }

    /**
     * Registers user with mandatory face data and optional password recovery
     * question.
     */
    public static boolean registerUserWithFaceData(String id, String name, String email, String password, String role,
            byte[] faceData, String securityQuestion, String securityAnswer) {
        return registerUserWithFaceData(id, name, email, password, role, faceData,
                securityQuestion, securityAnswer, null, null);
    }

    /**
     * Registers user with face data, optional password recovery details, and
     * batch/subject metadata.
     */
    public static boolean registerUserWithFaceData(String id, String name, String email, String password, String role,
            byte[] faceData, String securityQuestion, String securityAnswer, Integer batchYear, String subject) {
        id = normalizeUserId(id);
        email = normalizeEmail(email);
        role = normalizeRole(role);
        name = name == null ? null : name.trim();
        subject = normalizeSubject(subject);

        boolean requiresBatch = "STUDENT".equals(role) || "TEACHER".equals(role);
        Integer normalizedBatchYear = isBatchYearValid(batchYear) ? batchYear : null;

        if ("STUDENT".equals(role) && (faceData == null || faceData.length == 0)) {
            System.out.println("Validation failed: Student registration requires face data");
            return false;
        }

        if (requiresBatch && normalizedBatchYear == null) {
            System.out.println("Validation failed: Invalid or missing batch year");
            return false;
        }

        if ("TEACHER".equals(role) && (subject == null || subject.isEmpty())) {
            System.out.println("Validation failed: Teacher registration requires subject");
            return false;
        }

        boolean teacherHasBatchMapping = "TEACHER".equals(role) && normalizedBatchYear != null;

        // Validate input data first
        ValidationUtils.ValidationResult validation = ValidationUtils.validateUserData(id, name, email, password, role);
        if (!validation.isValid()) {
            System.out.println("Validation failed: " + validation.getErrorMessage());
            return false;
        }

        // Enforce role+batch ID code uniqueness on backend as well.
        if (("STUDENT".equals(role) || "TEACHER".equals(role))
                && normalizedBatchYear != null
                && !isRoleCodeAvailableInBatch(id, role, normalizedBatchYear)) {
            System.out.println("Validation failed: user ID code already taken for this role and batch");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Disable autocommit to create explicit transaction
            conn.setAutoCommit(false);

            // Check if user already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE id = ? OR email = ?")) {
                checkStmt.setString(1, id);
                checkStmt.setString(2, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("User ID or email already exists!");
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Hash password securely
            String passwordHash = SecurityUtils.hashPassword(password);
            String securityAnswerHash = (securityAnswer == null || securityAnswer.trim().isEmpty())
                    ? null
                    : SecurityUtils.hashPassword(securityAnswer.trim().toLowerCase());

            // Insert user with face data
            try (PreparedStatement insertUserStmt = conn.prepareStatement(
                    "INSERT INTO users (id, name, email, password_hash, security_question, security_answer_hash, batch_year, subject, role, face_data) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insertUserStmt.setString(1, id);
                insertUserStmt.setString(2, name);
                insertUserStmt.setString(3, email);
                insertUserStmt.setString(4, passwordHash);
                insertUserStmt.setString(5,
                        securityQuestion == null || securityQuestion.trim().isEmpty() ? null : securityQuestion.trim());
                insertUserStmt.setString(6, securityAnswerHash);
                if (requiresBatch && normalizedBatchYear != null) {
                    insertUserStmt.setInt(7, normalizedBatchYear);
                } else {
                    insertUserStmt.setNull(7, java.sql.Types.INTEGER);
                }
                insertUserStmt.setString(8, "TEACHER".equals(role) ? subject : null);
                insertUserStmt.setString(9, role);
                insertUserStmt.setBytes(10, faceData);

                int result = insertUserStmt.executeUpdate();
                if (result == 0) {
                    conn.rollback();
                    return false;
                }
            }

            if (teacherHasBatchMapping
                    && !upsertTeacherBatchSubject(conn, id, normalizedBatchYear, subject)) {
                conn.rollback();
                return false;
            }

            // Commit transaction if all operations succeeded
            conn.commit();
            System.out.println("User registered successfully with face data: " + name);
            return true;
        } catch (SQLException e) {
            System.err.println("Error in user registration transaction: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            // Restore autocommit to default state
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error restoring autocommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Stores face data for a user in the database.
     * Face data is stored as binary data for later recognition.
     * 
     * @param userId   The user's unique identifier
     * @param faceData Binary face data from OpenCV Mat
     * @return true if storage successful, false otherwise
     */
    public static boolean storeFaceData(String userId, byte[] faceData) {
        userId = normalizeUserId(userId);
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET face_data = ? WHERE id = ?")) {

            stmt.setBytes(1, faceData);
            stmt.setString(2, userId);

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error storing face data: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves a user's information by their ID.
     * 
     * @param userId The user's unique identifier
     * @return Map containing user details (id, name, email, role, isActive)
     */
    public static Map<String, Object> getUserById(String userId) {
        Map<String, Object> user = new HashMap<>();
        userId = normalizeUserId(userId);

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT id, name, email, role, batch_year, subject, is_active, security_question, " +
                                "CASE WHEN face_data IS NULL THEN 0 ELSE 1 END AS has_face_data " +
                                "FROM users WHERE id = ?")) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user.put("id", rs.getString("id"));
                    user.put("name", rs.getString("name"));
                    user.put("email", rs.getString("email"));
                    user.put("role", rs.getString("role"));
                    user.put("batchYear", rs.getObject("batch_year") == null ? null : rs.getInt("batch_year"));
                    user.put("subject", rs.getString("subject"));
                    user.put("isActive", rs.getBoolean("is_active"));
                    user.put("securityQuestion", rs.getString("security_question"));
                    user.put("hasFaceData", rs.getInt("has_face_data") == 1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }
        return user;
    }

    /**
     * Retrieves all active users from the database.
     * 
     * @return List of user maps containing id, name, email, and role
     */
    public static List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT id, name, email, role, batch_year, subject, CASE WHEN face_data IS NULL THEN 0 ELSE 1 END AS has_face_data "
                                +
                                "FROM users WHERE is_active = 1 ORDER BY name")) {

            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getString("id"));
                user.put("name", rs.getString("name"));
                user.put("email", rs.getString("email"));
                user.put("role", rs.getString("role"));
                user.put("batchYear", rs.getObject("batch_year") == null ? null : rs.getInt("batch_year"));
                user.put("subject", rs.getString("subject"));
                user.put("hasFaceData", rs.getInt("has_face_data") == 1);
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }
        return users;
    }

    /**
     * Deletes a user and all user-linked records from the database.
     * This is a hard delete and removes attendance, leaves, OTPs, and login
     * attempts before deleting the user row.
     * 
     * @param userId The user's unique identifier
     * @return true if deletion successful, false otherwise
     */
    public static boolean deactivateUser(String userId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String[] deleteQueries = {
                    "DELETE FROM teacher_batch_subjects WHERE teacher_id = ?",
                    "DELETE FROM attendance WHERE user_id = ?",
                    "DELETE FROM leaves WHERE user_id = ?",
                    "DELETE FROM otp_tokens WHERE user_id = ?",
                    "DELETE FROM login_attempts WHERE user_id = ?",
                    "DELETE FROM users WHERE id = ?"
            };

            int userDeleteResult = 0;
            for (int i = 0; i < deleteQueries.length; i++) {
                try (PreparedStatement stmt = conn.prepareStatement(deleteQueries[i])) {
                    stmt.setString(1, userId);
                    int affected = stmt.executeUpdate();
                    if (i == deleteQueries.length - 1) {
                        userDeleteResult = affected;
                    }
                }
            }

            if (userDeleteResult == 0) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting user data: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error rolling back user deletion: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error restoring autocommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Logs login attempts for security auditing.
     * Records both successful and failed login attempts with timestamp and IP.
     * 
     * @param userId    User identifier or email
     * @param success   Whether the login was successful
     * @param ipAddress IP address of the login attempt
     */
    private static void logLoginAttempt(String userId, boolean success, String ipAddress) {
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO login_attempts (user_id, success, ip_address) VALUES (?, ?, ?)")) {

            stmt.setString(1, userId);
            stmt.setBoolean(2, success);
            stmt.setString(3, ipAddress);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging login attempt: " + e.getMessage());
        }
    }

    /**
     * Generates and stores a One-Time Password (OTP) for a user.
     * OTP is valid for 5 minutes and stored in the database.
     * 
     * @param userId The user's unique identifier
     * @return Generated 6-digit OTP, or null if generation fails
     */
    public static String generateAndStoreOTP(String userId) {
        userId = normalizeUserId(userId);
        String otp = SecurityUtils.generateOTP();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO otp_tokens (user_id, otp_code, expires_at) " +
                                "VALUES (?, ?, DATEADD(MINUTE, 5, GETDATE()))")) {

            stmt.setString(1, userId);
            stmt.setString(2, otp);
            stmt.executeUpdate();
            return otp;
        } catch (SQLException e) {
            System.err.println("Error generating OTP: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifies an OTP for a user in an atomic transaction.
     * Checks if the OTP is valid, unused, and not expired.
     * Marks the OTP as used after successful verification in the same transaction.
     * Uses explicit transaction control to prevent race conditions where
     * multiple requests could use the same OTP simultaneously.
     * 
     * @param userId The user's unique identifier
     * @param otp    The OTP to verify
     * @return true if OTP is valid, false otherwise
     */
    public static boolean verifyOTP(String userId, String otp) {
        userId = normalizeUserId(userId);
        // Validate OTP format
        if (!ValidationUtils.isValidOTP(otp)) {
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Disable autocommit to create explicit transaction
            conn.setAutoCommit(false);

            // SELECT and UPDATE in same transaction to prevent race conditions
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id FROM otp_tokens WHERE user_id = ? AND otp_code = ? " +
                            "AND is_used = 0 AND expires_at > GETDATE()")) {

                stmt.setString(1, userId);
                stmt.setString(2, otp);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Mark OTP as used in same transaction
                        int otpId = rs.getInt("id");
                        try (PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE otp_tokens SET is_used = 1 WHERE id = ?")) {
                            updateStmt.setInt(1, otpId);
                            updateStmt.executeUpdate();
                        }
                        conn.commit();
                        return true;
                    }
                }
            }
            conn.rollback();
            return false;
        } catch (SQLException e) {
            System.err.println("Error verifying OTP: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            // Restore autocommit to default state
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error restoring autocommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Returns configured security question for an email, or null if none is set.
     */
    public static String getSecurityQuestionByEmail(String email) {
        email = normalizeEmail(email);
        if (!ValidationUtils.isValidEmail(email)) {
            return null;
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT security_question FROM users WHERE email = ? AND is_active = 1")) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("security_question");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching security question: " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns configured security question for a user ID, or null if none is set.
     */
    public static String getSecurityQuestionByUserId(String userId) {
        userId = normalizeUserId(userId);
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT security_question FROM users WHERE id = ? AND is_active = 1")) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("security_question");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching security question by user id: " + e.getMessage());
        }

        return null;
    }

    /**
     * Resets user password after validating security answer.
     */
    public static boolean resetPasswordWithSecurityAnswerByUserId(String userId, String securityAnswer,
            String newPassword) {
        userId = normalizeUserId(userId);
        if (userId == null || userId.isEmpty() || !ValidationUtils.isStrongPassword(newPassword)
                || securityAnswer == null || securityAnswer.trim().isEmpty()) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement readStmt = conn.prepareStatement(
                        "SELECT security_answer_hash FROM users WHERE id = ? AND is_active = 1")) {
            readStmt.setString(1, userId);

            try (ResultSet rs = readStmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                String storedHash = rs.getString("security_answer_hash");
                if (storedHash == null
                        || !SecurityUtils.verifyPassword(securityAnswer.trim().toLowerCase(), storedHash)) {
                    logLoginAttempt(userId, false, "127.0.0.1");
                    return false;
                }

                String newPasswordHash = SecurityUtils.hashPassword(newPassword);
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE users SET password_hash = ? WHERE id = ?")) {
                    updateStmt.setString(1, newPasswordHash);
                    updateStmt.setString(2, userId);
                    int changed = updateStmt.executeUpdate();
                    return changed > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error resetting password by user id: " + e.getMessage());
        }

        return false;
    }

    /**
     * Resets user password after validating security answer.
     */
    public static boolean resetPasswordWithSecurityAnswer(String email, String securityAnswer, String newPassword) {
        email = normalizeEmail(email);
        if (!ValidationUtils.isValidEmail(email) || !ValidationUtils.isStrongPassword(newPassword)
                || securityAnswer == null || securityAnswer.trim().isEmpty()) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement readStmt = conn.prepareStatement(
                        "SELECT id, security_answer_hash FROM users WHERE email = ? AND is_active = 1")) {
            readStmt.setString(1, email);

            try (ResultSet rs = readStmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                String userId = rs.getString("id");
                return resetPasswordWithSecurityAnswerByUserId(userId, securityAnswer, newPassword);
            }
        } catch (SQLException e) {
            System.err.println("Error resetting password: " + e.getMessage());
        }

        return false;
    }

    private static boolean upsertTeacherBatchSubject(Connection conn, String teacherId, int batchYear, String subject)
            throws SQLException {
        try (PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE teacher_batch_subjects SET subject = ?, created_at = GETDATE() WHERE teacher_id = ? AND batch_year = ?")) {
            updateStmt.setString(1, subject);
            updateStmt.setString(2, teacherId);
            updateStmt.setInt(3, batchYear);
            int updated = updateStmt.executeUpdate();
            if (updated > 0) {
                return true;
            }
        }

        try (PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO teacher_batch_subjects (teacher_id, batch_year, subject) VALUES (?, ?, ?)")) {
            insertStmt.setString(1, teacherId);
            insertStmt.setInt(2, batchYear);
            insertStmt.setString(3, subject);
            return insertStmt.executeUpdate() > 0;
        }
    }

    /**
     * Adds or updates the subject a teacher handles for a specific batch.
     */
    public static boolean upsertTeacherBatchSubject(String teacherId, int batchYear, String subject) {
        teacherId = normalizeUserId(teacherId);
        subject = normalizeSubject(subject);
        if (!isBatchYearValid(batchYear) || subject == null || subject.isEmpty()) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            return upsertTeacherBatchSubject(conn, teacherId, batchYear, subject);
        } catch (SQLException e) {
            System.err.println("Error saving teacher batch subject: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns all batch-subject mappings for a teacher.
     */
    public static List<Map<String, Object>> getTeacherBatchSubjects(String teacherId) {
        List<Map<String, Object>> mappings = new ArrayList<>();
        teacherId = normalizeUserId(teacherId);

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT batch_year, subject FROM teacher_batch_subjects WHERE teacher_id = ? ORDER BY batch_year")) {
            stmt.setString(1, teacherId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("batchYear", rs.getInt("batch_year"));
                    row.put("subject", rs.getString("subject"));
                    mappings.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching teacher batch subjects: " + e.getMessage());
        }

        return mappings;
    }

    private static int getMaxSuffixForRolePattern(String role, String prefix, int suffixLength) {
        int max = 0;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT id FROM users WHERE role = ? AND id LIKE ?")) {
            stmt.setString(1, role);
            stmt.setString(2, prefix + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    if (id == null || id.length() != prefix.length() + suffixLength) {
                        continue;
                    }
                    String suffix = id.substring(prefix.length());
                    if (!suffix.matches("\\d{" + suffixLength + "}")) {
                        continue;
                    }
                    int value = Integer.parseInt(suffix);
                    if (value > max) {
                        max = value;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating next ID: " + e.getMessage());
        }
        return max;
    }

    /**
     * Generates next unique student roll number for a batch year (e.g., 24ITM001).
     */
    public static String getNextStudentRollNo(int batchYear) {
        if (!isBatchYearValid(batchYear)) {
            return "";
        }
        String prefix = String.format("%02dITM", batchYear % 100);
        int next = getMaxSuffixForRolePattern("STUDENT", prefix, 3) + 1;
        return prefix + String.format("%03d", next);
    }

    /**
     * Generates next unique teacher ID for a batch year (e.g., 24ITM01).
     */
    public static String getNextTeacherIdForBatch(int batchYear) {
        if (!isBatchYearValid(batchYear)) {
            return "";
        }
        String prefix = String.format("%02dITM", batchYear % 100);
        int next = getMaxSuffixForRolePattern("TEACHER", prefix, 2) + 1;
        return prefix + String.format("%02d", next);
    }

    /**
     * Generates next unique admin ID (e.g., ADMIN01).
     */
    public static String getNextAdminId() {
        int next = getMaxSuffixForRolePattern("ADMIN", "ADMIN", 2) + 1;
        return "ADMIN" + String.format("%02d", next);
    }

    /**
     * Generates next unique employee ID (e.g., EMP001).
     */
    public static String getNextEmployeeId() {
        int next = getMaxSuffixForRolePattern("EMPLOYEE", "EMP", 3) + 1;
        return "EMP" + String.format("%03d", next);
    }

    /**
     * Gets the batch prefix for a given batch year (e.g., 24ITM for 2024).
     * 
     * @param batchYear The batch year (2023-2029)
     * @return Batch prefix (e.g., "24ITM") or empty string if invalid
     */
    public static String getBatchPrefixForYear(int batchYear) {
        if (!isBatchYearValid(batchYear)) {
            return "";
        }
        return String.format("%02dITM", batchYear % 100);
    }

    /**
     * Extracts the roll number from a user ID given a prefix.
     * Example: extractRollNumberFromId("24ITM020", "24ITM") returns "020"
     * 
     * @param userId The user ID (e.g., "24ITM020")
     * @param prefix The batch prefix (e.g., "24ITM")
     * @return The roll number portion, or empty string if invalid
     */
    public static String extractRollNumberFromId(String userId, String prefix) {
        if (userId == null || prefix == null || !userId.startsWith(prefix)) {
            return "";
        }
        return userId.substring(prefix.length());
    }

    /**
     * Checks if a user ID already exists in the database.
     * 
     * @param userId The user ID to check
     * @return true if user ID exists, false otherwise
     */
    public static boolean checkUserIdExists(String userId) {
        userId = normalizeUserId(userId);
        if (userId == null || userId.isEmpty()) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) as cnt FROM users WHERE id = ?")) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking user ID existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if a specific user ID is available in a batch.
     * This prevents duplicate roll numbers within the same batch.
     * Example: If 24ITM020 exists, it returns false; otherwise true.
     * 
     * @param userId    The user ID to check (e.g., "24ITM020")
     * @param batchYear The batch year to verify against
     * @return true if ID is available (not taken), false if already in use
     */
    public static boolean isUserIdAvailableInBatch(String userId, int batchYear) {
        userId = normalizeUserId(userId);
        if (!isBatchYearValid(batchYear) || userId == null || userId.isEmpty()) {
            return false;
        }

        String prefix = getBatchPrefixForYear(batchYear);
        if (!userId.startsWith(prefix)) {
            return false; // ID doesn't match batch prefix
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) as cnt FROM users WHERE id = ? AND batch_year = ?")) {
            stmt.setString(1, userId);
            stmt.setInt(2, batchYear);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // If count is 0, the ID is available; if > 0, it's taken
                    return rs.getInt("cnt") == 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking user ID availability in batch: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks whether the provided student/teacher ID code is available for the
     * specific role and batch.
     */
    public static boolean isRoleCodeAvailableInBatch(String userId, String role, int batchYear) {
        userId = normalizeUserId(userId);
        role = normalizeRole(role);
        if (!isBatchYearValid(batchYear) || userId == null || userId.isEmpty()) {
            return false;
        }

        if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
            return false;
        }

        String prefix = getBatchPrefixForYear(batchYear);
        if (!userId.startsWith(prefix)) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) as cnt FROM users WHERE id = ? AND batch_year = ? AND role = ?")) {
            stmt.setString(1, userId);
            stmt.setInt(2, batchYear);
            stmt.setString(3, role);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") == 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking role ID availability in batch: " + e.getMessage());
        }
        return false;
    }

    /**
     * Validates that a user ID matches the expected format for its role and batch.
     * For students/teachers: prefix must match batch year + course code
     * 
     * @param userId    The user ID to validate
     * @param role      The user role (STUDENT, TEACHER, etc.)
     * @param batchYear The batch year (if applicable)
     * @return true if ID format is valid for the role and batch
     */
    public static boolean validateUserIdFormat(String userId, String role, Integer batchYear) {
        userId = normalizeUserId(userId);
        role = normalizeRole(role);

        if (userId == null || userId.isEmpty()) {
            return false;
        }

        if ("STUDENT".equals(role)) {
            if (!isBatchYearValid(batchYear)) {
                return false;
            }
            String expectedPrefix = getBatchPrefixForYear(batchYear);
            if (!userId.startsWith(expectedPrefix)) {
                return false;
            }
            String rollNo = extractRollNumberFromId(userId, expectedPrefix);
            // Roll number should be exactly 3 digits
            return rollNo.matches("\\d{3}");
        } else if ("TEACHER".equals(role)) {
            if (!isBatchYearValid(batchYear)) {
                return false;
            }
            String expectedPrefix = getBatchPrefixForYear(batchYear);
            if (!userId.startsWith(expectedPrefix)) {
                return false;
            }
            String teacherNo = extractRollNumberFromId(userId, expectedPrefix);
            // Teacher number should be exactly 2 digits
            return teacherNo.matches("\\d{2}");
        } else if ("ADMIN".equals(role)) {
            return userId.startsWith("ADMIN") && userId.substring(5).matches("\\d{2}");
        } else if ("EMPLOYEE".equals(role)) {
            return userId.startsWith("EMP") && userId.substring(3).matches("\\d{3}");
        }

        return false;
    }
}
