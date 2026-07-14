import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leave Management System for the Face Recognition Application.
 * Handles leave requests, approvals, rejections, and validations.
 * 
 * Leave Types: SICK, CASUAL, EMERGENCY, PERSONAL
 * Statuses: PENDING, APPROVED, REJECTED
 * 
 * @author Face Recognition Team
 * @version 1.0
 */
public class LeaveManager {

    /**
     * Request leave for a user
     * 
     * @param userId    User requesting leave
     * @param startDate Leave start date
     * @param endDate   Leave end date
     * @param reason    Reason for leave
     * @param leaveType Type of leave (SICK, CASUAL, EMERGENCY, PERSONAL)
     * @return true if request submitted successfully, false otherwise
     */
    public static boolean requestLeave(String userId, LocalDate startDate, LocalDate endDate, String reason,
            String leaveType) {
        // Validate inputs
        if (userId == null || userId.isEmpty() || startDate == null || endDate == null) {
            System.out.println("Invalid leave request parameters");
            return false;
        }

        if (startDate.isAfter(endDate)) {
            System.out.println("Start date cannot be after end date");
            return false;
        }

        // Validate leave type
        if (!isValidLeaveType(leaveType)) {
            System.out.println("Invalid leave type: " + leaveType);
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check for overlapping leaves
            String checkOverlapSql = "SELECT id FROM leaves WHERE user_id = ? AND status = 'APPROVED' " +
                    "AND ((start_date <= ? AND end_date >= ?) OR " +
                    "(start_date <= ? AND end_date >= ?) OR " +
                    "(start_date >= ? AND end_date <= ?))";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkOverlapSql)) {
                checkStmt.setString(1, userId);
                checkStmt.setDate(2, Date.valueOf(endDate));
                checkStmt.setDate(3, Date.valueOf(startDate));
                checkStmt.setDate(4, Date.valueOf(endDate));
                checkStmt.setDate(5, Date.valueOf(startDate));
                checkStmt.setDate(6, Date.valueOf(startDate));
                checkStmt.setDate(7, Date.valueOf(endDate));

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Overlapping approved leave already exists for this period");
                        return false;
                    }
                }
            }

            // Insert leave request
            String insertSql = "INSERT INTO leaves (user_id, start_date, end_date, reason, leave_type, status, request_date) "
                    +
                    "VALUES (?, ?, ?, ?, ?, 'PENDING', CAST(GETDATE() AS DATE))";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, userId);
                insertStmt.setDate(2, Date.valueOf(startDate));
                insertStmt.setDate(3, Date.valueOf(endDate));
                insertStmt.setString(4, reason);
                insertStmt.setString(5, leaveType);

                int result = insertStmt.executeUpdate();
                if (result > 0) {
                    long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
                    System.out.println("Leave request submitted - Days: " + daysDiff + ", Type: " + leaveType);
                    AppLogger.logUserAction(userId, "LEAVE_REQUESTED", leaveType + " leave for " + daysDiff + " days");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error submitting leave request: " + e.getMessage());
            AppLogger.error("Error submitting leave request for " + userId, e);
        }
        return false;
    }

    /**
     * Approve a pending leave request (Admin/Teacher only)
     * 
     * @param leaveId Leave ID to approve
     * @return true if approved successfully, false otherwise
     */
    public static boolean approveLeave(int leaveId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE leaves SET status = 'APPROVED', approval_date = CAST(GETDATE() AS DATE) " +
                    "WHERE id = ? AND status = 'PENDING'";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, leaveId);
                int result = stmt.executeUpdate();

                if (result > 0) {
                    System.out.println("Leave #" + leaveId + " approved successfully");
                    AppLogger.info("Leave #" + leaveId + " approved");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error approving leave: " + e.getMessage());
        }
        return false;
    }

    /**
     * Reject a pending leave request
     * 
     * @param leaveId         Leave ID to reject
     * @param rejectionReason Reason for rejection
     * @return true if rejected successfully, false otherwise
     */
    public static boolean rejectLeave(int leaveId, String rejectionReason) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE leaves SET status = 'REJECTED', rejection_reason = ?, approval_date = CAST(GETDATE() AS DATE) "
                    +
                    "WHERE id = ? AND status = 'PENDING'";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, rejectionReason);
                stmt.setInt(2, leaveId);
                int result = stmt.executeUpdate();

                if (result > 0) {
                    System.out.println("Leave #" + leaveId + " rejected");
                    AppLogger.info("Leave #" + leaveId + " rejected: " + rejectionReason);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error rejecting leave: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get all pending leave requests (for admin/teacher approval)
     * 
     * @return List of pending leave requests with user details
     */
    public static List<Map<String, Object>> getPendingLeaves() {
        List<Map<String, Object>> leaves = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT l.id, u.id as userId, u.name as userName, u.role, l.start_date, l.end_date, " +
                                "l.leave_type, l.reason, l.request_date, l.status " +
                                "FROM leaves l " +
                                "JOIN users u ON l.user_id = u.id " +
                                "WHERE l.status = 'PENDING' " +
                                "ORDER BY l.request_date DESC")) {

            while (rs.next()) {
                Map<String, Object> leave = new HashMap<>();
                leave.put("leaveId", rs.getInt("id"));
                leave.put("userId", rs.getString("userId"));
                leave.put("userName", rs.getString("userName"));
                leave.put("userRole", rs.getString("role"));
                leave.put("startDate", rs.getDate("start_date").toString());
                leave.put("endDate", rs.getDate("end_date").toString());
                leave.put("leaveType", rs.getString("leave_type"));
                leave.put("reason", rs.getString("reason"));
                leave.put("requestDate", rs.getDate("request_date").toString());
                leave.put("status", rs.getString("status"));

                // Calculate number of days
                LocalDate start = rs.getDate("start_date").toLocalDate();
                LocalDate end = rs.getDate("end_date").toLocalDate();
                long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                leave.put("days", days);

                leaves.add(leave);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pending leaves: " + e.getMessage());
        }

        return leaves;
    }

    /**
     * Get pending leave requests filtered by requester role.
     * Example: STUDENT for teacher approvals, TEACHER for admin approvals.
     *
     * @param requesterRole User role to filter leave request owners by
     * @return List of pending leave requests with user details
     */
    public static List<Map<String, Object>> getPendingLeavesByRequesterRole(String requesterRole) {
        List<Map<String, Object>> leaves = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT l.id, u.id as userId, u.name as userName, u.role, l.start_date, l.end_date, " +
                                "l.leave_type, l.reason, l.request_date, l.status " +
                                "FROM leaves l " +
                                "JOIN users u ON l.user_id = u.id " +
                                "WHERE l.status = 'PENDING' AND u.role = ? " +
                                "ORDER BY l.request_date DESC")) {

            stmt.setString(1, requesterRole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> leave = new HashMap<>();
                    leave.put("leaveId", rs.getInt("id"));
                    leave.put("userId", rs.getString("userId"));
                    leave.put("userName", rs.getString("userName"));
                    leave.put("userRole", rs.getString("role"));
                    leave.put("startDate", rs.getDate("start_date").toString());
                    leave.put("endDate", rs.getDate("end_date").toString());
                    leave.put("leaveType", rs.getString("leave_type"));
                    leave.put("reason", rs.getString("reason"));
                    leave.put("requestDate", rs.getDate("request_date").toString());
                    leave.put("status", rs.getString("status"));

                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                    leave.put("days", days);

                    leaves.add(leave);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pending leaves by role: " + e.getMessage());
        }

        return leaves;
    }

    /**
     * Get all approved leaves for a user
     * 
     * @param userId User ID
     * @return List of approved leaves
     */
    public static List<Map<String, Object>> getApprovedLeaves(String userId) {
        List<Map<String, Object>> leaves = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT id, start_date, end_date, leave_type, reason, request_date, approval_date " +
                                "FROM leaves " +
                                "WHERE user_id = ? AND status = 'APPROVED' " +
                                "ORDER BY start_date DESC")) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> leave = new HashMap<>();
                    leave.put("leaveId", rs.getInt("id"));
                    leave.put("startDate", rs.getDate("start_date").toString());
                    leave.put("endDate", rs.getDate("end_date").toString());
                    leave.put("leaveType", rs.getString("leave_type"));
                    leave.put("reason", rs.getString("reason"));
                    leave.put("requestDate", rs.getDate("request_date").toString());

                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                    leave.put("days", days);

                    leaves.add(leave);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching approved leaves: " + e.getMessage());
        }

        return leaves;
    }

    /**
     * Get user's leave history (all requests)
     * 
     * @param userId User ID
     * @return List of all leave requests for the user
     */
    public static List<Map<String, Object>> getUserLeaveHistory(String userId) {
        List<Map<String, Object>> leaves = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT id, start_date, end_date, leave_type, reason, request_date, status, " +
                                "approval_date, rejection_reason " +
                                "FROM leaves " +
                                "WHERE user_id = ? " +
                                "ORDER BY request_date DESC")) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> leave = new HashMap<>();
                    leave.put("leaveId", rs.getInt("id"));
                    leave.put("startDate", rs.getDate("start_date").toString());
                    leave.put("endDate", rs.getDate("end_date").toString());
                    leave.put("leaveType", rs.getString("leave_type"));
                    leave.put("reason", rs.getString("reason"));
                    leave.put("requestDate", rs.getDate("request_date").toString());
                    leave.put("status", rs.getString("status"));

                    java.sql.Date approvalDate = rs.getDate("approval_date");
                    leave.put("approvalDate", approvalDate != null ? approvalDate.toString() : null);

                    leave.put("rejectionReason", rs.getString("rejection_reason"));

                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                    leave.put("days", days);

                    leaves.add(leave);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching leave history: " + e.getMessage());
        }

        return leaves;
    }

    /**
     * Get leave history for all students (admin view)
     *
     * @return List of all student leave requests with user details
     */
    public static List<Map<String, Object>> getStudentLeaveHistory() {
        List<Map<String, Object>> leaves = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT l.id, u.id as userId, u.name as userName, l.start_date, l.end_date, " +
                                "l.leave_type, l.reason, l.request_date, l.status, l.approval_date, l.rejection_reason "
                                +
                                "FROM leaves l " +
                                "JOIN users u ON l.user_id = u.id " +
                                "WHERE u.role = 'STUDENT' " +
                                "ORDER BY l.request_date DESC")) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> leave = new HashMap<>();
                    leave.put("leaveId", rs.getInt("id"));
                    leave.put("userId", rs.getString("userId"));
                    leave.put("userName", rs.getString("userName"));
                    leave.put("startDate", rs.getDate("start_date").toString());
                    leave.put("endDate", rs.getDate("end_date").toString());
                    leave.put("leaveType", rs.getString("leave_type"));
                    leave.put("reason", rs.getString("reason"));
                    leave.put("requestDate", rs.getDate("request_date").toString());
                    leave.put("status", rs.getString("status"));

                    java.sql.Date approvalDate = rs.getDate("approval_date");
                    leave.put("approvalDate", approvalDate != null ? approvalDate.toString() : null);
                    leave.put("rejectionReason", rs.getString("rejection_reason"));

                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                    leave.put("days", days);

                    leaves.add(leave);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching student leave history: " + e.getMessage());
        }

        return leaves;
    }

    /**
     * Check if a date is covered by an approved leave
     * 
     * @param userId User ID
     * @param date   Date to check
     * @return true if date is covered by approved leave, false otherwise
     */
    public static boolean isApprovedLeave(String userId, LocalDate date) {
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) as count FROM leaves " +
                                "WHERE user_id = ? AND status = 'APPROVED' " +
                                "AND start_date <= ? AND end_date >= ?")) {

            stmt.setString(1, userId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking approved leave: " + e.getMessage());
        }

        return false;
    }

    /**
     * Validate if leave type is valid
     * 
     * @param leaveType Type to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidLeaveType(String leaveType) {
        return leaveType != null && (leaveType.equals("SICK") ||
                leaveType.equals("CASUAL") ||
                leaveType.equals("EMERGENCY") ||
                leaveType.equals("PERSONAL"));
    }

    /**
     * Get leave statistics for a user in date range
     * 
     * @param userId    User ID
     * @param startDate Period start date
     * @param endDate   Period end date
     * @return Map with approved, pending, rejected counts
     */
    public static Map<String, Integer> getUserLeaveStats(String userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("approved", 0);
        stats.put("pending", 0);
        stats.put("rejected", 0);

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT status, COUNT(*) as count FROM leaves " +
                                "WHERE user_id = ? AND start_date >= ? AND end_date <= ? " +
                                "GROUP BY status")) {

            stmt.setString(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    int count = rs.getInt("count");
                    stats.put(status.toLowerCase(), count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching leave stats: " + e.getMessage());
        }

        return stats;
    }
}
