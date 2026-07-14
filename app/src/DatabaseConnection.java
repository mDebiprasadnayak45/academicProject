import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Connection Manager for MS SQL Server.
 * Manages database connections and initializes the database schema
 * for the Face Recognition Attendance System.
 * 
 * @author Face Recognition Team
 * @version 2.0
 */
public class DatabaseConnection {
    // SQL Server connection URLs (AppConfig.getSqlServerDatabaseUrl() already
    // includes databaseName)
    private static final String DB_URL = AppConfig.getSqlServerDatabaseUrl();

    // Master database URL (for creating application database if it doesn't exist)
    private static final String MASTER_URL = buildMasterUrl();

    private static final String DB_USER = AppConfig.SQLSERVER_USER;
    private static final String DB_PASSWORD = AppConfig.SQLSERVER_PASSWORD;

    /**
     * Builds JDBC URL for master database used during initialization.
     * 
     * @return JDBC connection URL for master database
     */
    private static String buildMasterUrl() {
                String base;
                if (AppConfig.SQLSERVER_INSTANCE != null && !AppConfig.SQLSERVER_INSTANCE.isEmpty()) {
                        base = "jdbc:sqlserver://" + AppConfig.SQLSERVER_HOST + "\\" + AppConfig.SQLSERVER_INSTANCE + ";";
                } else {
                        base = "jdbc:sqlserver://" + AppConfig.SQLSERVER_HOST + ":" + AppConfig.SQLSERVER_PORT + ";";
                }
        String trustCert = "trustServerCertificate=" + AppConfig.SQLSERVER_TRUST_SERVER_CERT + ";";
        String integrated = AppConfig.SQLSERVER_INTEGRATED_SECURITY ? "integratedSecurity=true;" : "";
        return base + trustCert + "databaseName=master;" + integrated;
    }

    /**
     * Gets or creates a connection to the database.
     * Reuses existing connection if still valid.
     * 
     * @return Active database connection
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[DB] New SQL Server connection established.");
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MS SQL Server Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB] Failed to establish SQL Server connection: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Initializes the database schema with all required tables.
     * Creates the database if it doesn't exist, then creates all application
     * tables:
     * users, attendance, login_attempts, and otp_tokens.
     * Uses try-with-resources for proper resource management.
     * 
     * @throws SQLException if schema initialization fails
     */
    public static void initializeDatabase() throws SQLException {
        // Ensure driver is available
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MS SQL Server Driver not found: " + e.getMessage());
        }

        // Ensure application database exists
        try (Connection masterConn = DriverManager.getConnection(MASTER_URL, DB_USER, DB_PASSWORD);
                Statement masterStmt = masterConn.createStatement()) {
            masterStmt.executeUpdate("IF DB_ID('" + AppConfig.SQLSERVER_DB_NAME + "') IS NULL CREATE DATABASE "
                    + AppConfig.SQLSERVER_DB_NAME);
        }

        // Connect to application database and create tables
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement stmt = conn.createStatement()) {

            // Users table
            String createUsersTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'users') " +
                    "CREATE TABLE users (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "security_question VARCHAR(255), " +
                    "security_answer_hash VARCHAR(255), " +
                    "batch_year INT NULL, " +
                    "subject VARCHAR(100) NULL, " +
                    "role VARCHAR(20) DEFAULT 'STUDENT' CHECK (role IN ('STUDENT', 'EMPLOYEE', 'TEACHER', 'ADMIN')), " +
                    "face_data VARBINARY(MAX), " +
                    "created_at DATETIME DEFAULT GETDATE(), " +
                    "is_active BIT DEFAULT 1)";

            // Attendance table with unique constraint to prevent duplicates
            String createAttendanceTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'attendance') " +
                    "CREATE TABLE attendance (" +
                    "id INT PRIMARY KEY IDENTITY(1,1), " +
                    "user_id VARCHAR(50) NOT NULL, " +
                    "attendance_date DATE NOT NULL, " +
                    "check_in_time TIME NOT NULL, " +
                    "check_out_time TIME, " +
                    "status VARCHAR(20) DEFAULT 'PRESENT' CHECK (status IN ('PRESENT', 'ABSENT', 'LATE')), " +
                    "confidence_score FLOAT, " +
                    "face_verified BIT NOT NULL DEFAULT 0, " +
                    "created_at DATETIME DEFAULT GETDATE(), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id), " +
                    "UNIQUE (user_id, attendance_date))";

            // Login attempts table (for security auditing)
            String createLoginAttemptsTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'login_attempts') "
                    +
                    "CREATE TABLE login_attempts (" +
                    "id INT PRIMARY KEY IDENTITY(1,1), " +
                    "user_id VARCHAR(50), " +
                    "attempt_time DATETIME DEFAULT GETDATE(), " +
                    "success BIT, " +
                    "ip_address VARCHAR(45), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";

            // OTP table for two-factor authentication
            String createOTPTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'otp_tokens') " +
                    "CREATE TABLE otp_tokens (" +
                    "id INT PRIMARY KEY IDENTITY(1,1), " +
                    "user_id VARCHAR(50) NOT NULL, " +
                    "otp_code VARCHAR(10) NOT NULL, " +
                    "created_at DATETIME DEFAULT GETDATE(), " +
                    "expires_at DATETIME, " +
                    "is_used BIT DEFAULT 0, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";

            // Leaves table for leave management
            String createLeavesTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'leaves') " +
                    "CREATE TABLE leaves (" +
                    "id INT PRIMARY KEY IDENTITY(1,1), " +
                    "user_id VARCHAR(50) NOT NULL, " +
                    "start_date DATE NOT NULL, " +
                    "end_date DATE NOT NULL, " +
                    "leave_type VARCHAR(20) NOT NULL CHECK (leave_type IN ('SICK', 'CASUAL', 'EMERGENCY', 'PERSONAL')), "
                    +
                    "reason VARCHAR(500), " +
                    "status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')), " +
                    "request_date DATE DEFAULT CAST(GETDATE() AS DATE), " +
                    "approval_date DATE, " +
                    "rejection_reason VARCHAR(500), " +
                    "created_at DATETIME DEFAULT GETDATE(), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";

            String createTeacherBatchSubjectsTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'teacher_batch_subjects') "
                    +
                    "CREATE TABLE teacher_batch_subjects (" +
                    "id INT PRIMARY KEY IDENTITY(1,1), " +
                    "teacher_id VARCHAR(50) NOT NULL, " +
                    "batch_year INT NOT NULL CHECK (batch_year BETWEEN 2023 AND 2029), " +
                    "subject VARCHAR(100) NOT NULL, " +
                    "created_at DATETIME DEFAULT GETDATE(), " +
                    "UNIQUE (teacher_id, batch_year), " +
                    "FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE)";

            stmt.executeUpdate(createUsersTable);
            stmt.executeUpdate(createAttendanceTable);
            stmt.executeUpdate(createLoginAttemptsTable);
            stmt.executeUpdate(createOTPTable);
            stmt.executeUpdate(createLeavesTable);
            stmt.executeUpdate(createTeacherBatchSubjectsTable);

            // Backward-compatible schema updates for existing databases
            String addUsersSecurityQuestion = "IF COL_LENGTH('users', 'security_question') IS NULL " +
                    "ALTER TABLE users ADD security_question VARCHAR(255) NULL";
            String addUsersSecurityAnswerHash = "IF COL_LENGTH('users', 'security_answer_hash') IS NULL " +
                    "ALTER TABLE users ADD security_answer_hash VARCHAR(255) NULL";
            String addUsersBatchYear = "IF COL_LENGTH('users', 'batch_year') IS NULL " +
                    "ALTER TABLE users ADD batch_year INT NULL";
            String addUsersSubject = "IF COL_LENGTH('users', 'subject') IS NULL " +
                    "ALTER TABLE users ADD subject VARCHAR(100) NULL";
            String addAttendanceFaceVerified = "IF COL_LENGTH('attendance', 'face_verified') IS NULL " +
                    "ALTER TABLE attendance ADD face_verified BIT NOT NULL DEFAULT 0";
            String ensureTeacherBatchSubjects = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'teacher_batch_subjects') "
                    +
                    "CREATE TABLE teacher_batch_subjects (" +
                    "id INT PRIMARY KEY IDENTITY(1,1), " +
                    "teacher_id VARCHAR(50) NOT NULL, " +
                    "batch_year INT NOT NULL CHECK (batch_year BETWEEN 2023 AND 2029), " +
                    "subject VARCHAR(100) NOT NULL, " +
                    "created_at DATETIME DEFAULT GETDATE(), " +
                    "UNIQUE (teacher_id, batch_year), " +
                    "FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE)";

            stmt.execute(addUsersSecurityQuestion);
            stmt.execute(addUsersSecurityAnswerHash);
            stmt.execute(addUsersBatchYear);
            stmt.execute(addUsersSubject);
            stmt.execute(addAttendanceFaceVerified);
            stmt.execute(ensureTeacherBatchSubjects);

            // Ensure users.role check constraint supports all application roles on
            // existing databases too
            String fixUserRoleConstraint = "DECLARE @constraintName NVARCHAR(128); " +
                    "SELECT TOP 1 @constraintName = cc.name " +
                    "FROM sys.check_constraints cc " +
                    "JOIN sys.columns c ON cc.parent_object_id = c.object_id AND cc.parent_column_id = c.column_id " +
                    "WHERE cc.parent_object_id = OBJECT_ID('users') AND c.name = 'role'; " +
                    "IF @constraintName IS NOT NULL " +
                    "BEGIN " +
                    "  DECLARE @dropSql NVARCHAR(400); " +
                    "  SET @dropSql = N'ALTER TABLE users DROP CONSTRAINT [' + REPLACE(@constraintName, ']', ']]') + N']'; "
                    +
                    "  EXEC(@dropSql); " +
                    "END; " +
                    "IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_users_role' AND parent_object_id = OBJECT_ID('users')) "
                    +
                    "BEGIN " +
                    "  ALTER TABLE users WITH NOCHECK ADD CONSTRAINT CK_users_role CHECK (role IN ('STUDENT', 'EMPLOYEE', 'TEACHER', 'ADMIN')); "
                    +
                    "END;";

            stmt.execute(fixUserRoleConstraint);
            System.out.println("Database schema initialized successfully!");
        }
    }

    /**
     * Closes the active database connection.
     * Should be called when the application shuts down.
     * 
     * @throws SQLException if connection closure fails
     */
    public static void closeConnection() throws SQLException {
        // No-op: connections are now managed per call via try-with-resources.
    }

    /**
     * Tests database connectivity by executing a simple query.
     * 
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }
}