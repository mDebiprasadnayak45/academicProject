# Face Recognition Attendance System

## Prerequisites
- Java JDK 11 or higher (https://adoptopenjdk.net/ or https://jdk.java.net/)
- No need to install JavaFX or OpenCV separately (included in project resources)

## Folder Structure
- `app/` - Source code and resources
- `build/` - Compiled classes and runtime resources
- `scripts/` - Build and run scripts for Windows/Linux/Mac
- `data/` - Database, face data, logs

## How to Build and Run

### On Windows
1. Open Command Prompt in the project folder.
2. Run:
   ```
   scripts\build.bat
   scripts\run.bat
   ```
3. Visual C++ runtime is bundled in `dist/runtime/vc` (vcruntime140.dll, vcruntime140_1.dll, msvcp140.dll), so the receiver typically does not need separate VC++ installation.

### On Linux/Mac
1. Open Terminal in the project folder.
2. Run:
   ```
   bash scripts/build.sh
   bash scripts/run.sh
   ```

## Features
- Face recognition-based attendance
- User management (admin/teacher/student)
- Leave management
- Attendance and leave reports
- Export reports as CSV
- Secure login (password + optional OTP)

## Notes
- All dependencies (JavaFX, JavaCV, OpenCV) are included in the `resources` folder.
- If you encounter errors about missing JavaFX or OpenCV, check the `jpackage-input/javafx-sdk-*` and `javacv-platform-*` folders are present.
- Database is file-based and included in `data/attendance_h2/`.

## Troubleshooting
- Make sure Java is installed and added to your PATH.
- If you see errors about missing libraries, ensure you are running the scripts from the project root.
- For any issues, check the logs in `data/logs/`.

## Two-PC Viva Setup (Your PC = DB Server, Friend PC = App Client)

Use this setup when you want to show examiner that one machine hosts DB and another uses the app.

### 1. Prepare SQL Server on Your PC (Server Machine)
1. Keep your database with existing data in SQL Server (for example `FaceAttendanceDB`).
2. In SQL Server Configuration Manager:
    - Enable `TCP/IP` for your SQL instance.
    - Restart SQL Server service.
3. Open Windows Firewall inbound rule for SQL Server port (`1433`) and SQL Browser (`UDP 1434`) if needed.
4. Find your server machine IPv4 address using `ipconfig`.

### 2. Configure App on Friend PC (Client Machine)
Edit [app/config/config.properties](app/config/config.properties) (or [dist/config.properties](dist/config.properties) after build) and set:

- `db.server=<YOUR_SERVER_PC_IP>`
- `db.name=FaceAttendanceDB`
- `db.username=sa`
- `db.password=<your_sql_password>`

Choose one connection mode:

- Instance mode (if SQL Browser is available):
   - `db.instance=SQLEXPRESS`
- Port mode (recommended for reliable viva demo):
   - `db.instance=` (leave empty)
   - `db.port=1433`

### 3. Run Demo
1. On your server PC, keep SQL Server running.
2. On friend PC, run:
    - `scripts\build.bat`
    - `scripts\run.bat`
3. Any new attendance/users inserted from friend PC are stored in your server PC database.

### 4. Quick Connectivity Check
- From friend PC PowerShell: `Test-NetConnection <SERVER_IP> -Port 1433`
- If failed, recheck firewall, SQL TCP/IP, and server IP.

---
For questions or help, contact the project maintainer.
