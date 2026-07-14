/*
================================================================================
 Face Recognition Attendance System - VIVA DEMO SQL FILE (SSMS)
================================================================================
How to use:
1) Open this file in SSMS and connect to your SQL Server.
2) Run section by section during viva (top to bottom).
3) Read comments before each section; they explain what each query proves.

Database expected: FaceAttendanceDB
================================================================================
*/

USE [FaceAttendanceDB];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/* ============================================================================
   1) SERVER + DATABASE PROOF
   Purpose: Show teacher that you are connected to correct SQL Server + DB.
============================================================================ */
SELECT
  @@SERVERNAME AS server_name,
  DB_NAME() AS current_database,
  SUSER_SNAME() AS sql_login,
  GETDATE() AS server_time;
GO

SELECT
  SERVERPROPERTY('ProductVersion') AS sql_version,
  SERVERPROPERTY('Edition') AS sql_edition,
  SERVERPROPERTY('MachineName') AS machine_name;
GO

/* ============================================================================
   2) TABLE INVENTORY + ROW COUNTS
   Purpose: Show all core project tables and current number of records.
============================================================================ */
SELECT
  t.name AS table_name,
  SUM(p.rows) AS row_count
FROM sys.tables t
JOIN sys.partitions p
  ON t.object_id = p.object_id
   AND p.index_id IN (0,1)
WHERE t.name IN ('users', 'attendance', 'leaves', 'otp_tokens', 'login_attempts')
GROUP BY t.name
ORDER BY t.name;
GO

/* ============================================================================
   3) USERS MASTER DATA (includes role + active status + face data presence)
   Purpose: Show student/teacher/admin/employee users and whether face data exists.
============================================================================ */
SELECT
  id,
  name,
  email,
  role,
  is_active,
  CASE WHEN face_data IS NULL THEN 0 ELSE 1 END AS has_face_data,
  DATALENGTH(face_data) AS face_data_bytes,
  created_at
FROM dbo.users
ORDER BY role, name;
GO

/* ============================================================================
   4) TODAY ATTENDANCE (check-in/check-out/status) - raw attendance records
   Purpose: Show actual attendance rows created by your app today.
============================================================================ */
SELECT
  a.id,
  a.user_id,
  u.name,
  u.role,
  a.attendance_date,
  a.check_in_time,
  a.check_out_time,
  a.status,
  a.confidence_score,
  a.created_at
FROM dbo.attendance a
JOIN dbo.users u ON u.id = a.user_id
WHERE a.attendance_date = CAST(GETDATE() AS DATE)
ORDER BY a.check_in_time DESC;
GO

/* ============================================================================
   5) ABSENT LOGIC PROOF (LEFT JOIN)
   Purpose: Show that users with NO attendance row today are counted as ABSENT.
============================================================================ */
SELECT
  u.id,
  u.name,
  u.role,
  COALESCE(CONVERT(VARCHAR(8), a.check_in_time, 108), '-') AS check_in_time,
  COALESCE(CONVERT(VARCHAR(8), a.check_out_time, 108), '-') AS check_out_time,
  COALESCE(a.status, 'ABSENT') AS computed_status_today
FROM dbo.users u
LEFT JOIN dbo.attendance a
  ON a.user_id = u.id
   AND a.attendance_date = CAST(GETDATE() AS DATE)
WHERE u.is_active = 1
ORDER BY u.role, u.name;
GO

/* ============================================================================
   6) PRESENT/LATE WINDOW RULE PROOF (09:30 to 10:10)
   Purpose: Show expected status by configured time window for today rows.
   Rule: between 09:30 and 10:10 => PRESENT, else => LATE
============================================================================ */
DECLARE @PresentStart TIME = '09:30';
DECLARE @PresentEnd TIME = '10:10';

SELECT
  a.user_id,
  u.name,
  a.attendance_date,
  a.check_in_time,
  CASE
    WHEN a.check_in_time >= @PresentStart AND a.check_in_time <= @PresentEnd THEN 'PRESENT'
    ELSE 'LATE'
  END AS expected_status_by_rule,
  a.status AS stored_status
FROM dbo.attendance a
JOIN dbo.users u ON u.id = a.user_id
WHERE a.attendance_date = CAST(GETDATE() AS DATE)
ORDER BY a.check_in_time;
GO

/* ============================================================================
   7) SUMMARY COUNTS FOR TODAY (Present / Late / Absent)
   Purpose: Viva-friendly summary card values like app dashboard.
============================================================================ */
WITH TodayStatus AS (
  SELECT
    u.id,
    u.role,
    COALESCE(a.status, 'ABSENT') AS status_today
  FROM dbo.users u
  LEFT JOIN dbo.attendance a
    ON a.user_id = u.id
     AND a.attendance_date = CAST(GETDATE() AS DATE)
  WHERE u.is_active = 1
)
SELECT
  role,
  SUM(CASE WHEN status_today = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
  SUM(CASE WHEN status_today = 'LATE' THEN 1 ELSE 0 END) AS late_count,
  SUM(CASE WHEN status_today = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count
FROM TodayStatus
GROUP BY role
ORDER BY role;
GO

/* ============================================================================
   8) LEAVE MANAGEMENT PROOF
   Purpose: Show leave requests lifecycle (PENDING/APPROVED/REJECTED).
============================================================================ */
SELECT
  l.id AS leave_id,
  l.user_id,
  u.name,
  u.role,
  l.leave_type,
  l.start_date,
  l.end_date,
  l.status,
  l.reason,
  l.rejection_reason,
  l.request_date,
  l.approval_date
FROM dbo.leaves l
JOIN dbo.users u ON u.id = l.user_id
ORDER BY l.request_date DESC, l.id DESC;
GO

/* Pending only */
SELECT
  l.id AS leave_id,
  l.user_id,
  u.name,
  u.role,
  l.leave_type,
  l.start_date,
  l.end_date,
  l.reason
FROM dbo.leaves l
JOIN dbo.users u ON u.id = l.user_id
WHERE l.status = 'PENDING'
ORDER BY l.id DESC;
GO

/* ============================================================================
   9) LOGIN SECURITY PROOF (OTP + login attempts)
   Purpose: Show security activity in DB.
============================================================================ */
SELECT TOP 20
  id,
  user_id,
  otp_code,
  created_at,
  expires_at,
  is_used
FROM dbo.otp_tokens
ORDER BY created_at DESC;
GO

SELECT TOP 50
  id,
  user_id,
  attempt_time,
  success,
  ip_address
FROM dbo.login_attempts
ORDER BY attempt_time DESC;
GO

/* ============================================================================
   10) WEEKLY REPORT QUERY (Present/Late/Absent per user)
   Purpose: Show report logic equivalent to dashboard weekly report.
============================================================================ */
DECLARE @WeekStart DATE = DATEADD(DAY, -6, CAST(GETDATE() AS DATE));
DECLARE @WeekEnd DATE = CAST(GETDATE() AS DATE);

SELECT
  u.id,
  u.name,
  u.role,
  SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
  SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS late_count,
  COUNT(DISTINCT a.attendance_date) AS marked_days,
  (7 - COUNT(DISTINCT a.attendance_date)) AS absent_count
FROM dbo.users u
LEFT JOIN dbo.attendance a
  ON a.user_id = u.id
   AND a.attendance_date BETWEEN @WeekStart AND @WeekEnd
WHERE u.is_active = 1
GROUP BY u.id, u.name, u.role
ORDER BY u.role, u.name;
GO

/* ============================================================================
   11) USER-WISE DATA VIEW (for explaining deletion impact)
   Purpose: Pick a user id and show all related records before delete.
============================================================================ */
DECLARE @DemoUserId VARCHAR(50) = 'PUT_USER_ID_HERE';

SELECT 'users' AS table_name, COUNT(*) AS row_count FROM dbo.users WHERE id = @DemoUserId
UNION ALL
SELECT 'attendance', COUNT(*) FROM dbo.attendance WHERE user_id = @DemoUserId
UNION ALL
SELECT 'leaves', COUNT(*) FROM dbo.leaves WHERE user_id = @DemoUserId
UNION ALL
SELECT 'otp_tokens', COUNT(*) FROM dbo.otp_tokens WHERE user_id = @DemoUserId
UNION ALL
SELECT 'login_attempts', COUNT(*) FROM dbo.login_attempts WHERE user_id = @DemoUserId;
GO

/* ============================================================================
   12) OPTIONAL: HARD DELETE A SINGLE USER + ALL RELATED DATA
   Purpose: Same behavior as your Admin 'Deactivate Selected' flow (hard delete).
   IMPORTANT: Replace @DeleteUserId first.
============================================================================ */
/*
DECLARE @DeleteUserId VARCHAR(50) = 'PUT_USER_ID_HERE';

BEGIN TRY
  BEGIN TRAN;

  DELETE FROM dbo.attendance WHERE user_id = @DeleteUserId;
  DELETE FROM dbo.leaves WHERE user_id = @DeleteUserId;
  DELETE FROM dbo.otp_tokens WHERE user_id = @DeleteUserId;
  DELETE FROM dbo.login_attempts WHERE user_id = @DeleteUserId;
  DELETE FROM dbo.users WHERE id = @DeleteUserId;

  COMMIT TRAN;
  PRINT 'SUCCESS: User and related data deleted.';
END TRY
BEGIN CATCH
  IF @@TRANCOUNT > 0 ROLLBACK TRAN;
  PRINT 'FAILED: User deletion rolled back.';
  SELECT ERROR_MESSAGE() AS ErrorMessage;
END CATCH;
GO
*/

/* ============================================================================
   13) OPTIONAL: FULL DATABASE DATA RESET (keep schema, clear all data)
   Purpose: Start viva with clean DB.
============================================================================ */
/*
BEGIN TRY
  BEGIN TRAN;

  DELETE FROM dbo.attendance;
  DELETE FROM dbo.leaves;
  DELETE FROM dbo.otp_tokens;
  DELETE FROM dbo.login_attempts;
  DELETE FROM dbo.users;

  DBCC CHECKIDENT ('dbo.attendance', RESEED, 0) WITH NO_INFOMSGS;
  DBCC CHECKIDENT ('dbo.leaves', RESEED, 0) WITH NO_INFOMSGS;
  DBCC CHECKIDENT ('dbo.otp_tokens', RESEED, 0) WITH NO_INFOMSGS;
  DBCC CHECKIDENT ('dbo.login_attempts', RESEED, 0) WITH NO_INFOMSGS;

  COMMIT TRAN;
  PRINT 'SUCCESS: Full app data reset complete.';
END TRY
BEGIN CATCH
  IF @@TRANCOUNT > 0 ROLLBACK TRAN;
  PRINT 'FAILED: Full reset rolled back.';
  SELECT ERROR_MESSAGE() AS ErrorMessage;
END CATCH;
GO
*/

/* ============================================================================
   14) QUICK SESSION CHECK (optional)
   Purpose: Show active SQL login sessions for 'sa'.
============================================================================ */
SELECT
  login_name,
  host_name,
  program_name,
  status,
  login_time
FROM sys.dm_exec_sessions
WHERE login_name = 'sa'
ORDER BY login_time DESC;
GO
