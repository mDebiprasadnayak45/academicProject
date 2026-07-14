import java.io.InputStream;
import java.time.LocalTime;
import java.util.Properties;

/**
 * Application Configuration Constants.
 * Centralized configuration settings for the Face Recognition Attendance
 * System.
 * Contains database settings, face detection parameters, camera settings,
 * GUI configuration, security settings, and application metadata.
 * 
 * @author Face Recognition Team
 * @version 2.0
 */
public class AppConfig {

    // Static configuration properties loaded from config.properties
    private static final Properties properties = new Properties();

    static {
        try {
            // Load from classpath (works both in development and when packaged as JAR)
            try (InputStream input = AppConfig.class.getResourceAsStream("/config.properties")) {
                if (input != null) {
                    properties.load(input);
                    System.out.println("[OK] Configuration loaded from config.properties");
                } else {
                    System.err.println("[WARN] config.properties not found on classpath");
                    System.err.println("[WARN] Using default hardcoded values");
                    System.err.println(
                            "[INFO] Defaults: db.server=localhost, db.instance=SQLEXPRESS, db.name=FaceAttendanceDB");
                }
            }
            // Validate critical configurations
            validateConfiguration();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load config.properties: " + e.getMessage());
            System.err.println("[WARN] Using default hardcoded values");
        }
    }

    /**
     * Validates critical configuration values at startup.
     * Warns if important settings like database password are empty or invalid.
     */
    private static void validateConfiguration() {
        // Check if database password is empty (critical configuration)
        String password = properties.getProperty("db.password", "");
        if (password.isEmpty()) {
            System.err.println("[WARN] Database password is empty or not configured in config.properties");
            System.err.println("[WARN] Database connection may fail");
        }

        // Check if database host is configured
        String host = properties.getProperty("db.server", "");
        if (host.isEmpty()) {
            System.err.println("[WARN] Database server/host not configured, using default: localhost");
        }
    }

    /**
     * Reads and normalizes time values from config.
     * Supports both HH:MM and HH.MM inputs.
     */
    private static String getTimeProperty(String key, String fallback) {
        String configuredValue = properties.getProperty(key, fallback);
        String normalizedConfigured = configuredValue == null ? "" : configuredValue.trim().replace('.', ':');
        String normalizedFallback = fallback.trim().replace('.', ':');

        try {
            LocalTime.parse(normalizedConfigured);
            return normalizedConfigured;
        } catch (Exception ex) {
            System.err.println("[WARN] Invalid time format for " + key + ": '" + configuredValue
                    + "'. Using fallback " + normalizedFallback);
            return normalizedFallback;
        }
    }

    // ==================== SQL Server Configuration ====================
    /** SQL Server host address */
    public static final String SQLSERVER_HOST = properties.getProperty("db.server", "localhost");

    /** SQL Server instance name */
    public static final String SQLSERVER_INSTANCE = properties.getProperty("db.instance", "SQLEXPRESS");

    /** SQL Server port (used when instance is blank) */
    public static final String SQLSERVER_PORT = properties.getProperty("db.port", "1433");

    /** SQL Server database name (avoid using master) */
    public static final String SQLSERVER_DB_NAME = properties.getProperty("db.name", "FaceAttendanceDB");

    /** SQL Server username */
    public static final String SQLSERVER_USER = properties.getProperty("db.username", "sa");

    /** SQL Server password - LOADED FROM config.properties (secure) */
    public static final String SQLSERVER_PASSWORD = properties.getProperty("db.password", "");

    /** Trust SQL Server certificate (local dev) - loaded from config.properties */
    public static final boolean SQLSERVER_TRUST_SERVER_CERT = Boolean
            .parseBoolean(properties.getProperty("db.trustCert", "true"));

    /** Use Windows Integrated Security - loaded from config.properties */
    public static final boolean SQLSERVER_INTEGRATED_SECURITY = Boolean
            .parseBoolean(properties.getProperty("db.integratedSecurity", "false"));

    // ==================== Face Detection Configuration ====================
    /** Scale factor for multi-scale face detection */
    public static final float FACE_DETECTION_SCALE_FACTOR = 1.05f; // more sensitive, less jitter

    /** Minimum number of neighboring rectangles for detection */
    public static final int FACE_DETECTION_MIN_NEIGHBORS = 6; // more strict, less false positives

    /** Minimum face size in pixels */
    public static final int MIN_FACE_SIZE = 60; // ignore tiny faces

    /** Maximum face size in pixels */
    public static final int MAX_FACE_SIZE = 400; // allow larger faces

    /** Standard size for cropped face images */
    public static final int FACE_CROP_SIZE = 100;

    /**
     * Confidence threshold for face recognition (0.0 to 1.0) - 0.5% match required
     * (demo)
     */
    public static final float CONFIDENCE_THRESHOLD = 0.005f;

    // ==================== Attendance Configuration ====================
    /** Present window start time (HH:MM) */
    public static final String ATTENDANCE_PRESENT_START_TIME = getTimeProperty(
            "attendance.present.start.time",
            properties.getProperty("attendance.late.time", "09:30"));

    /** Present window end time (HH:MM) */
    public static final String ATTENDANCE_PRESENT_END_TIME = getTimeProperty(
            "attendance.present.end.time",
            "10:10");

    /** Backward-compatible alias for older code */
    public static final String ATTENDANCE_START_TIME = ATTENDANCE_PRESENT_START_TIME;

    /** Backward-compatible alias for older code */
    public static final String LATE_TIME = ATTENDANCE_PRESENT_END_TIME;

    /** OTP validity duration in minutes */
    public static final int OTP_VALIDITY_MINUTES = 5;

    /** Maximum login attempts before lockout */
    public static final int LOGIN_MAX_ATTEMPTS = 5;

    /** Login lockout duration in minutes */
    public static final int LOGIN_LOCKOUT_MINUTES = 15;

    // ==================== Camera Configuration ====================
    /** Default camera device index */
    public static final int DEFAULT_CAMERA_INDEX = 0;

    /** Camera capture width in pixels */
    public static final int CAMERA_WIDTH = 640;

    /** Camera capture height in pixels */
    public static final int CAMERA_HEIGHT = 480;

    /** Camera frames per second */
    public static final int CAMERA_FPS = 60;

    // ==================== GUI Configuration ====================
    /** Main window width in pixels */
    public static final int WINDOW_WIDTH = 1200;

    /** Main window height in pixels */
    public static final int WINDOW_HEIGHT = 700;

    /** Camera window width in pixels */
    public static final int CAMERA_WINDOW_WIDTH = 1000;

    /** Camera window height in pixels */
    public static final int CAMERA_WINDOW_HEIGHT = 850;

    /** Primary UI color (hex) */
    public static final String PRIMARY_COLOR = "#3498db";

    /** Success message color (hex) */
    public static final String SUCCESS_COLOR = "#27ae60";

    /** Error message color (hex) */
    public static final String ERROR_COLOR = "#e74c3c";

    /** Warning message color (hex) */
    public static final String WARNING_COLOR = "#f39c12";

    // ==================== File Paths ====================
    /** Cascade classifier XML file resource path */
    public static final String CASCADE_CLASSIFIER_RESOURCE = "/haarcascade_frontalface_default.xml";

    /** Directory for storing face data */
    public static final String FACE_DATA_DIRECTORY = "data/face_data/";

    // ==================== Security Configuration ====================
    /** Minimum password length */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /** Require uppercase letter in password */
    public static final boolean REQUIRE_UPPERCASE = true;

    /** Require lowercase letter in password */
    public static final boolean REQUIRE_LOWERCASE = true;

    /** Require digit in password */
    public static final boolean REQUIRE_DIGIT = true;

    /** Require special character in password */
    public static final boolean REQUIRE_SPECIAL_CHAR = true;

    // ==================== Application Info ====================
    /** Application name */
    public static final String APP_NAME = "Face Recognition Attendance System";

    /** Application version */
    public static final String APP_VERSION = "2.0.0";

    /** Application author/team */
    public static final String APP_AUTHOR = "Development Team";

    // ==================== Logging Configuration ====================
    /** Enable debug mode */
    public static final boolean DEBUG_MODE = false;

    /** Log file path */
    public static final String LOG_FILE_PATH = "data/logs/app.log";

    /**
     * Constructs the SQL Server connection URL from configuration parameters.
     * Safely handles empty instance names to prevent malformed URLs.
     * 
     * @return JDBC connection URL string for SQL Server
     */
    public static String getSqlServerDatabaseUrl() {
        // Use instance if provided, otherwise use port
        String base;
        if (!SQLSERVER_INSTANCE.isEmpty()) {
            base = "jdbc:sqlserver://" + SQLSERVER_HOST + "\\" + SQLSERVER_INSTANCE + ";";
        } else {
            base = "jdbc:sqlserver://" + SQLSERVER_HOST + ":" + SQLSERVER_PORT + ";";
        }
        String trustCert = "trustServerCertificate=" + SQLSERVER_TRUST_SERVER_CERT + ";";
        String dbName = "databaseName=" + SQLSERVER_DB_NAME + ";";
        String integrated = SQLSERVER_INTEGRATED_SECURITY ? "integratedSecurity=true;" : "";
        return base + trustCert + dbName + integrated;
    }

    /**
     * Prints current configuration settings to console.
     * Useful for debugging and verifying configuration on startup.
     * Shows which values are from config file vs defaults.
     */
    public static void printConfig() {
        System.out.println("===== Application Configuration =====");
        System.out.println("App: " + APP_NAME + " v" + APP_VERSION);
        System.out.println("Database Host: " + SQLSERVER_HOST);
        System.out.println("Database Instance: " + SQLSERVER_INSTANCE);
        System.out.println("Database Name: " + SQLSERVER_DB_NAME);
        System.out.println("Database User: " + SQLSERVER_USER);
        System.out.println("Trust Cert: " + SQLSERVER_TRUST_SERVER_CERT);
        System.out.println("Integrated Security: " + SQLSERVER_INTEGRATED_SECURITY);
        System.out.println("Face Detection Threshold: " + CONFIDENCE_THRESHOLD);
        System.out.println("Camera: " + CAMERA_WIDTH + "x" + CAMERA_HEIGHT + " @ " + CAMERA_FPS + " FPS");
        System.out.println("Log File: " + LOG_FILE_PATH);
        System.out.println("=====================================");
    }
}