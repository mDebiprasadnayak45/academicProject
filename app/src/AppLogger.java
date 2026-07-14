import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Application Logger
 * Centralized logging for debugging and tracking
 */
public class AppLogger {
    private static final String LOG_FILE = AppConfig.LOG_FILE_PATH;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static boolean fileLoggingEnabled = true;

    static {
        // Create logs directory if it doesn't exist
        File logDir = new File(LOG_FILE).getParentFile();
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }

    /**
     * Log info level message
     */
    public static void info(String message) {
        log("INFO", message, null);
    }

    /**
     * Log warning level message
     */
    public static void warning(String message) {
        log("WARN", message, null);
    }

    /**
     * Log error level message
     */
    public static void error(String message) {
        log("ERROR", message, null);
    }

    /**
     * Log error with exception
     */
    public static void error(String message, Exception e) {
        log("ERROR", message, e);
    }

    /**
     * Log debug message (only if debug mode enabled)
     */
    public static void debug(String message) {
        if (AppConfig.DEBUG_MODE) {
            log("DEBUG", message, null);
        }
    }

    /**
     * Main logging method
     */
    private static void log(String level, String message, Exception exception) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        // Print to console
        System.out.println(logMessage);
        if (exception != null) {
            exception.printStackTrace();
        }

        // Write to file (synchronized to prevent concurrent writes from multiple
        // threads)
        if (fileLoggingEnabled) {
            synchronized (AppLogger.class) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                    bw.write(logMessage);
                    bw.newLine();
                    if (exception != null) {
                        bw.write(exception.toString());
                        bw.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Failed to write to log file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Log user action (for audit trail)
     */
    public static void logUserAction(String userId, String action, String details) {
        String message = String.format("USER_ACTION: User=%s, Action=%s, Details=%s", userId, action, details);
        log("AUDIT", message, null);
    }

    /**
     * Log attendance event
     */
    public static void logAttendanceEvent(String userId, String event, String details) {
        String message = String.format("ATTENDANCE: User=%s, Event=%s, Details=%s", userId, event, details);
        log("ATTENDANCE", message, null);
    }

    /**
     * Clear log file
     */
    public static void clearLog() {
        synchronized (AppLogger.class) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE))) {
                bw.write("Log file cleared at " + LocalDateTime.now().format(DATE_FORMAT));
                bw.newLine();
            } catch (IOException e) {
                error("Failed to clear log file", e);
            }
        }
    }

    /**
     * Get log file path
     */
    public static String getLogFilePath() {
        return new File(LOG_FILE).getAbsolutePath();
    }

    /**
     * Enable/disable file logging
     */
    public static void setFileLoggingEnabled(boolean enabled) {
        fileLoggingEnabled = enabled;
    }
}
