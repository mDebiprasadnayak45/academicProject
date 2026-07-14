import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attendance Management System for the Face Recognition Application.
 * Handles marking attendance, tracking check-in/check-out times,
 * generating reports, and preventing duplicate attendance records.
 * 
 * @author Face Recognition Team
 * @version 2.0
 */
public class AttendanceManager {

    private static boolean isBatchYearValid(Integer batchYear) {
        return batchYear != null && batchYear >= 2023 && batchYear <= 2029;
    }

    private static String getBatchPrefix(int batchYear) {
        return String.format("%02dITM", batchYear % 100);
    }

    /**
     * Marks attendance for a user with duplicate prevention.
     * Ensures a student can only be marked present once per day.
     * Automatically determines if the check-in is late based on the configured
     * time.
     * 
     * @param userId          The user's unique identifier
     * @param confidenceScore The face recognition confidence score (0.0 to 1.0)
     * @return true if attendance marked successfully, false if already marked or
     *         error
     */
    public static boolean markAttendance(String userId, float confidenceScore) {
        return markAttendance(userId, confidenceScore, false);
    }

    /**
     * Marks attendance and records whether check-in was face-verified.
     */
    public static boolean markAttendance(String userId, float confidenceScore, boolean faceVerified) {
        // Validate confidence score
        if (!ValidationUtils.isValidConfidenceScore(confidenceScore)) {
            System.out.println("Invalid confidence score: " + confidenceScore);
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            LocalDate today = LocalDate.now();
            LocalTime checkInTime = LocalTime.now();

            // CHECK APPROVED LEAVE: Skip attendance if on approved leave
            if (LeaveManager.isApprovedLeave(userId, today)) {
                System.out.println("User " + userId + " is on approved leave today. Attendance marking skipped.");
                return false;
            }

            // DUPLICATE PREVENTION: Check if user already has attendance today
            String checkSql = "SELECT id FROM attendance WHERE user_id = ? AND attendance_date = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, userId);
                checkStmt.setDate(2, java.sql.Date.valueOf(today));

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Attendance already marked for user " + userId + " today!");
                        return false;
                    }
                }
            }

            // Determine attendance status based on configured present window
            LocalTime presentStart = LocalTime.parse(AppConfig.ATTENDANCE_PRESENT_START_TIME);
            LocalTime presentEnd = LocalTime.parse(AppConfig.ATTENDANCE_PRESENT_END_TIME);
            boolean isWithinPresentWindow = !checkInTime.isBefore(presentStart) && !checkInTime.isAfter(presentEnd);
            String status = isWithinPresentWindow ? "PRESENT" : "LATE";

            // Insert attendance record
            String sql = "INSERT INTO attendance (user_id, attendance_date, check_in_time, status, confidence_score, face_verified) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                stmt.setDate(2, java.sql.Date.valueOf(today));
                stmt.setTime(3, Time.valueOf(checkInTime));
                stmt.setString(4, status);
                stmt.setFloat(5, confidenceScore);
                stmt.setBoolean(6, faceVerified);

                int result = stmt.executeUpdate();

                if (result > 0) {
                    System.out.println("Attendance marked successfully for " + userId + " - Status: " + status);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error marking attendance: " + e.getMessage());
        }
        return false;
    }

    /**
     * Marks check-out time for a user who has already checked in.
     * Updates the attendance record for the current day.
     * 
     * @param userId The user's unique identifier
     * @return true if check-out marked successfully, false otherwise
     */
    public static boolean markCheckOut(String userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            LocalDate today = LocalDate.now();
            LocalTime checkOutTime = LocalTime.now();

            String sql = "UPDATE attendance SET check_out_time = ? " +
                    "WHERE user_id = ? AND attendance_date = ? AND check_out_time IS NULL";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTime(1, Time.valueOf(checkOutTime));
                stmt.setString(2, userId);
                stmt.setDate(3, java.sql.Date.valueOf(today));

                int result = stmt.executeUpdate();

                if (result > 0) {
                    System.out.println("Check-out marked for user: " + userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error marking check out: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves today's attendance records with user details.
     * 
     * @return List of attendance records for the current date
     */
    public static List<Map<String, Object>> getTodayAttendance() {
        List<Map<String, Object>> records = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT u.id, u.name, a.attendance_date, a.check_in_time, a.check_out_time, a.status, " +
                                "a.confidence_score, ISNULL(a.face_verified, 0) AS face_verified " +
                                "FROM attendance a " +
                                "JOIN users u ON a.user_id = u.id " +
                                "WHERE a.attendance_date = CAST(GETDATE() AS DATE) " +
                                "ORDER BY a.check_in_time DESC")) {

            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("userId", rs.getString("id"));
                record.put("userName", rs.getString("name"));
                record.put("attendanceDate", rs.getDate("attendance_date").toString());
                record.put("checkInTime", rs.getTime("check_in_time").toString());

                Time checkOutTime = rs.getTime("check_out_time");
                record.put("checkOutTime", checkOutTime != null ? checkOutTime.toString() : null);
                record.put("status", rs.getString("status"));
                record.put("confidenceScore", rs.getFloat("confidence_score"));
                record.put("usedFaceRecognition", rs.getInt("face_verified") == 1 ? "YES" : "NO");
                records.add(record);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching today's attendance: " + e.getMessage());
        }
        return records;
    }

    /**
     * Retrieves today's attendance records for active users of a specific role.
     * Includes absent users (users with no attendance record today).
     *
     * @param role Role to filter (STUDENT, TEACHER, EMPLOYEE, ADMIN)
     * @return List of role-filtered attendance records including ABSENT
     */
    public static List<Map<String, Object>> getTodayAttendanceByRole(String role) {
        return getTodayAttendanceByRole(role, null);
    }

    /**
     * Retrieves today's attendance records for active users of a specific role and
     * optional batch year.
     * Includes absent users (users with no attendance record today).
     */
    public static List<Map<String, Object>> getTodayAttendanceByRole(String role, Integer batchYear) {
        List<Map<String, Object>> records = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT u.id, u.name, a.check_in_time, a.check_out_time, " +
                "COALESCE(a.status, 'ABSENT') as status, a.confidence_score, " +
                "CASE WHEN u.face_data IS NULL THEN 0 ELSE 1 END AS has_face_data, " +
                "ISNULL(a.face_verified, 0) AS face_verified " +
                "FROM users u " +
                "LEFT JOIN attendance a ON u.id = a.user_id AND a.attendance_date = CAST(GETDATE() AS DATE) " +
                "WHERE u.is_active = 1 AND u.role = ? ");

        boolean hasBatchFilter = isBatchYearValid(batchYear);
        if (hasBatchFilter) {
            sql.append("AND (u.batch_year = ? OR (u.batch_year IS NULL AND u.id LIKE ?)) ");
        }
        sql.append("ORDER BY u.name");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            stmt.setString(paramIndex++, role);
            if (hasBatchFilter) {
                stmt.setInt(paramIndex++, batchYear);
                stmt.setString(paramIndex++, getBatchPrefix(batchYear) + "%");
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("userId", rs.getString("id"));
                    record.put("userName", rs.getString("name"));

                    Time checkInTime = rs.getTime("check_in_time");
                    Time checkOutTime = rs.getTime("check_out_time");

                    record.put("attendanceDate", LocalDate.now().toString());
                    record.put("checkInTime", checkInTime != null ? checkInTime.toString() : "-");
                    record.put("checkOutTime", checkOutTime != null ? checkOutTime.toString() : "-");
                    record.put("status", rs.getString("status"));
                    record.put("confidenceScore", rs.getObject("confidence_score"));
                    record.put("faceRegistered", rs.getInt("has_face_data") == 1 ? "YES" : "NO");
                    if ("ABSENT".equalsIgnoreCase(String.valueOf(record.get("status")))) {
                        record.put("usedFaceRecognition", "-");
                    } else {
                        record.put("usedFaceRecognition", rs.getInt("face_verified") == 1 ? "YES" : "NO");
                    }
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching role-based attendance: " + e.getMessage());
        }

        return records;
    }

    /**
     * Retrieves today's attendance records for active users of a specific role and
     * status.
     * Supports PRESENT, LATE, and ABSENT.
     *
     * @param role   Role to filter
     * @param status Status to filter
     * @return Filtered attendance records
     */
    public static List<Map<String, Object>> getTodayAttendanceByRoleAndStatus(String role, String status) {
        List<Map<String, Object>> all = getTodayAttendanceByRole(role);
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> row : all) {
            String rowStatus = String.valueOf(row.get("status"));
            if (status.equalsIgnoreCase(rowStatus)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    /**
     * Retrieves role-based attendance for the last N days.
     * This view returns actual attendance entries only (no synthesized ABSENT
     * rows).
     */
    public static List<Map<String, Object>> getRoleAttendanceForLastDays(String role, int days, String status) {
        return getRoleAttendanceForLastDays(role, days, status, null);
    }

    /**
     * Retrieves role-based attendance for the last N days with optional batch
     * filter.
     */
    public static List<Map<String, Object>> getRoleAttendanceForLastDays(String role, int days, String status,
            Integer batchYear) {
        List<Map<String, Object>> records = new ArrayList<>();
        if (days <= 0) {
            return records;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);

        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.name, a.attendance_date, a.check_in_time, a.check_out_time, a.status, " +
                        "CASE WHEN u.face_data IS NULL THEN 0 ELSE 1 END AS has_face_data, " +
                        "ISNULL(a.face_verified, 0) AS face_verified " +
                        "FROM attendance a " +
                        "JOIN users u ON a.user_id = u.id " +
                        "WHERE u.is_active = 1 AND u.role = ? AND a.attendance_date BETWEEN ? AND ?");

        boolean hasBatchFilter = isBatchYearValid(batchYear);
        if (hasBatchFilter) {
            sql.append(" AND (u.batch_year = ? OR (u.batch_year IS NULL AND u.id LIKE ?))");
        }

        boolean hasStatusFilter = status != null && !status.trim().isEmpty();
        if (hasStatusFilter) {
            sql.append(" AND a.status = ?");
        }
        sql.append(" ORDER BY a.attendance_date DESC, a.check_in_time DESC");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            stmt.setString(paramIndex++, role);
            stmt.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            stmt.setDate(paramIndex++, java.sql.Date.valueOf(endDate));
            if (hasBatchFilter) {
                stmt.setInt(paramIndex++, batchYear);
                stmt.setString(paramIndex++, getBatchPrefix(batchYear) + "%");
            }
            if (hasStatusFilter) {
                stmt.setString(paramIndex, status.trim().toUpperCase());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("userId", rs.getString("id"));
                    record.put("userName", rs.getString("name"));
                    record.put("attendanceDate", rs.getDate("attendance_date").toString());

                    Time checkInTime = rs.getTime("check_in_time");
                    Time checkOutTime = rs.getTime("check_out_time");

                    record.put("checkInTime", checkInTime != null ? checkInTime.toString() : "-");
                    record.put("checkOutTime", checkOutTime != null ? checkOutTime.toString() : "-");
                    record.put("status", rs.getString("status"));
                    record.put("faceRegistered", rs.getInt("has_face_data") == 1 ? "YES" : "NO");
                    record.put("usedFaceRecognition", rs.getInt("face_verified") == 1 ? "YES" : "NO");
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching role attendance for last days: " + e.getMessage());
        }

        return records;
    }

    /**
     * Retrieves role-based attendance across the full history, with optional
     * status and batch filters.
     */
    public static List<Map<String, Object>> getRoleAttendanceAllTime(String role, String status) {
        return getRoleAttendanceAllTime(role, status, null);
    }

    /**
     * Retrieves role-based attendance across the full history, with optional
     * status and batch filters.
     */
    public static List<Map<String, Object>> getRoleAttendanceAllTime(String role, String status,
            Integer batchYear) {
        List<Map<String, Object>> records = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.name, u.role, a.attendance_date, a.check_in_time, a.check_out_time, a.status, " +
                        "CASE WHEN u.face_data IS NULL THEN 0 ELSE 1 END AS has_face_data, " +
                        "ISNULL(a.face_verified, 0) AS face_verified " +
                        "FROM attendance a " +
                        "JOIN users u ON a.user_id = u.id " +
                        "WHERE u.is_active = 1");

        boolean hasRoleFilter = role != null && !role.trim().isEmpty();
        if (hasRoleFilter) {
            sql.append(" AND u.role = ?");
        }

        boolean hasBatchFilter = isBatchYearValid(batchYear);
        if (hasBatchFilter) {
            sql.append(" AND (u.batch_year = ? OR (u.batch_year IS NULL AND u.id LIKE ?))");
        }

        boolean hasStatusFilter = status != null && !status.trim().isEmpty();
        if (hasStatusFilter) {
            sql.append(" AND a.status = ?");
        }
        sql.append(" ORDER BY a.attendance_date DESC, a.check_in_time DESC");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (hasRoleFilter) {
                stmt.setString(paramIndex++, role.trim().toUpperCase());
            }
            if (hasBatchFilter) {
                stmt.setInt(paramIndex++, batchYear);
                stmt.setString(paramIndex++, getBatchPrefix(batchYear) + "%");
            }
            if (hasStatusFilter) {
                stmt.setString(paramIndex++, status.trim().toUpperCase());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("userId", rs.getString("id"));
                    record.put("userName", rs.getString("name"));
                    record.put("userRole", rs.getString("role"));
                    record.put("attendanceDate", rs.getDate("attendance_date").toString());

                    Time checkInTime = rs.getTime("check_in_time");
                    Time checkOutTime = rs.getTime("check_out_time");

                    record.put("checkInTime", checkInTime != null ? checkInTime.toString() : "-");
                    record.put("checkOutTime", checkOutTime != null ? checkOutTime.toString() : "-");
                    record.put("status", rs.getString("status"));
                    record.put("faceRegistered", rs.getInt("has_face_data") == 1 ? "YES" : "NO");
                    record.put("usedFaceRecognition", rs.getInt("face_verified") == 1 ? "YES" : "NO");
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all-time role attendance: " + e.getMessage());
        }

        return records;
    }

    /**
     * Returns attendance status totals across the full history.
     */
    public static Map<String, Object> getAttendanceStatusSummaryAllTime(String role, Integer batchYear) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("present", 0);
        summary.put("late", 0);
        summary.put("totalRecords", 0);
        summary.put("uniqueUsers", 0);

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS total_records, COUNT(DISTINCT a.user_id) AS unique_users, " +
                        "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count, " +
                        "SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS late_count " +
                        "FROM attendance a " +
                        "JOIN users u ON a.user_id = u.id " +
                        "WHERE u.is_active = 1");

        boolean hasRoleFilter = role != null && !role.trim().isEmpty();
        if (hasRoleFilter) {
            sql.append(" AND u.role = ?");
        }

        boolean hasBatchFilter = isBatchYearValid(batchYear);
        if (hasBatchFilter) {
            sql.append(" AND (u.batch_year = ? OR (u.batch_year IS NULL AND u.id LIKE ?))");
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (hasRoleFilter) {
                stmt.setString(paramIndex++, role.trim().toUpperCase());
            }
            if (hasBatchFilter) {
                stmt.setInt(paramIndex++, batchYear);
                stmt.setString(paramIndex++, getBatchPrefix(batchYear) + "%");
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    summary.put("totalRecords", rs.getInt("total_records"));
                    summary.put("uniqueUsers", rs.getInt("unique_users"));
                    summary.put("present", rs.getInt("present_count"));
                    summary.put("late", rs.getInt("late_count"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting all-time attendance summary: " + e.getMessage());
        }

        return summary;
    }

    /**
     * Generates attendance summary (present/late/absent) for active users of a
     * specific role for today.
     *
     * @param role Role to summarize
     * @return Map with keys: present, late, absent, date
     */
    public static Map<String, Object> getAttendanceSummaryByRole(String role) {
        return getAttendanceSummaryByRole(role, null);
    }

    /**
     * Generates attendance summary (present/late/absent) for active users of a
     * specific role for today, optionally filtered by batch.
     */
    public static Map<String, Object> getAttendanceSummaryByRole(String role, Integer batchYear) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("present", 0);
        summary.put("late", 0);
        summary.put("absent", 0);
        summary.put("date", LocalDate.now().toString());

        List<Map<String, Object>> rows = getTodayAttendanceByRole(role, batchYear);
        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("status"));
            if ("PRESENT".equalsIgnoreCase(status)) {
                summary.put("present", ((Integer) summary.get("present")) + 1);
            } else if ("LATE".equalsIgnoreCase(status)) {
                summary.put("late", ((Integer) summary.get("late")) + 1);
            } else {
                summary.put("absent", ((Integer) summary.get("absent")) + 1);
            }
        }

        return summary;
    }

    /**
     * Retrieves attendance records for a specified date range.
     * 
     * @param startDate Start date of the range (inclusive)
     * @param endDate   End date of the range (inclusive)
     * @return List of attendance records within the date range
     */
    public static List<Map<String, Object>> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> records = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT u.id, u.name, a.attendance_date, a.check_in_time, a.check_out_time, a.status " +
                                "FROM attendance a " +
                                "JOIN users u ON a.user_id = u.id " +
                                "WHERE a.attendance_date BETWEEN ? AND ? " +
                                "ORDER BY a.attendance_date DESC, a.check_in_time DESC")) {

            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("userId", rs.getString("id"));
                    record.put("userName", rs.getString("name"));
                    record.put("attendanceDate", rs.getDate("attendance_date").toString());
                    record.put("checkInTime", rs.getTime("check_in_time").toString());

                    Time checkOutTime = rs.getTime("check_out_time");
                    record.put("checkOutTime", checkOutTime != null ? checkOutTime.toString() : null);
                    record.put("status", rs.getString("status"));
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching attendance records: " + e.getMessage());
        }
        return records;
    }

    /**
     * Retrieves attendance records for a specific user within a date range.
     * 
     * @param userId    The user's unique identifier
     * @param startDate Start date of the range (inclusive)
     * @param endDate   End date of the range (inclusive)
     * @return List of attendance records for the specified user
     */
    public static List<Map<String, Object>> getUserAttendance(String userId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> records = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT attendance_date, check_in_time, check_out_time, status " +
                                "FROM attendance " +
                                "WHERE user_id = ? AND attendance_date BETWEEN ? AND ? " +
                                "ORDER BY attendance_date DESC")) {

            stmt.setString(1, userId);
            stmt.setDate(2, java.sql.Date.valueOf(startDate));
            stmt.setDate(3, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("date", rs.getDate("attendance_date").toString());
                    record.put("checkInTime", rs.getTime("check_in_time").toString());

                    Time checkOutTime = rs.getTime("check_out_time");
                    record.put("checkOutTime", checkOutTime != null ? checkOutTime.toString() : null);
                    record.put("status", rs.getString("status"));
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user attendance: " + e.getMessage());
        }
        return records;
    }

    /**
     * Retrieves all attendance records for a specific user across the full
     * history.
     */
    public static List<Map<String, Object>> getUserAttendance(String userId) {
        List<Map<String, Object>> records = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT attendance_date, check_in_time, check_out_time, status " +
                                "FROM attendance " +
                                "WHERE user_id = ? " +
                                "ORDER BY attendance_date DESC, check_in_time DESC")) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("date", rs.getDate("attendance_date").toString());
                    record.put("checkInTime", rs.getTime("check_in_time").toString());

                    Time checkOutTime = rs.getTime("check_out_time");
                    record.put("checkOutTime", checkOutTime != null ? checkOutTime.toString() : null);
                    record.put("status", rs.getString("status"));
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all-time user attendance: " + e.getMessage());
        }
        return records;
    }

    /**
     * Generates an attendance summary for a specific date.
     * Includes counts of present, late, and absent users.
     * 
     * @param date The date to generate the summary for
     * @return Map containing summary statistics (present, late, absent counts)
     */
    public static Map<String, Object> getAttendanceSummary(LocalDate date) {
        Map<String, Object> summary = new HashMap<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Count present
            String presentSql = "SELECT COUNT(*) FROM attendance WHERE attendance_date = ? AND status = 'PRESENT'";
            try (PreparedStatement presentStmt = conn.prepareStatement(presentSql)) {
                presentStmt.setDate(1, java.sql.Date.valueOf(date));
                try (ResultSet presentRs = presentStmt.executeQuery()) {
                    if (presentRs.next()) {
                        summary.put("present", presentRs.getInt(1));
                    }
                }
            }

            // Count late
            String lateSql = "SELECT COUNT(*) FROM attendance WHERE attendance_date = ? AND status = 'LATE'";
            try (PreparedStatement lateStmt = conn.prepareStatement(lateSql)) {
                lateStmt.setDate(1, java.sql.Date.valueOf(date));
                try (ResultSet lateRs = lateStmt.executeQuery()) {
                    if (lateRs.next()) {
                        summary.put("late", lateRs.getInt(1));
                    }
                }
            }

            // Count absent
            String absentSql = "SELECT COUNT(*) FROM users WHERE is_active = 1 AND id NOT IN " +
                    "(SELECT DISTINCT user_id FROM attendance WHERE attendance_date = ?)";
            try (PreparedStatement absentStmt = conn.prepareStatement(absentSql)) {
                absentStmt.setDate(1, java.sql.Date.valueOf(date));
                try (ResultSet absentRs = absentStmt.executeQuery()) {
                    if (absentRs.next()) {
                        summary.put("absent", absentRs.getInt(1));
                    }
                }
            }

            summary.put("date", date.toString());
        } catch (SQLException e) {
            System.err.println("Error getting attendance summary: " + e.getMessage());
        }
        return summary;
    }

    /**
     * Generates a weekly attendance report for all users.
     * Shows attendance statistics for each user within the week.
     * 
     * @param weekStart Start date of the week
     * @return List of user attendance statistics for the week
     */
    public static List<Map<String, Object>> generateWeeklyReport(LocalDate weekStart) {
        List<Map<String, Object>> report = new ArrayList<>();
        LocalDate weekEnd = weekStart.plusDays(6);

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT u.id, u.name, " +
                                "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_count, " +
                                "SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) as late_count, " +
                                "COUNT(DISTINCT a.attendance_date) as total_days " +
                                "FROM users u " +
                                "LEFT JOIN attendance a ON u.id = a.user_id AND a.attendance_date BETWEEN ? AND ? " +
                                "WHERE u.is_active = 1 " +
                                "GROUP BY u.id, u.name " +
                                "ORDER BY u.name")) {

            stmt.setDate(1, java.sql.Date.valueOf(weekStart));
            stmt.setDate(2, java.sql.Date.valueOf(weekEnd));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    int totalDays = rs.getInt("total_days");
                    int absentCount = Math.max(0, 7 - totalDays);
                    row.put("userId", rs.getString("id"));
                    row.put("userName", rs.getString("name"));
                    row.put("presentCount", rs.getInt("present_count"));
                    row.put("lateCount", rs.getInt("late_count"));
                    row.put("absentCount", absentCount);
                    row.put("totalDays", totalDays);
                    report.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating weekly report: " + e.getMessage());
        }
        return report;
    }
}
