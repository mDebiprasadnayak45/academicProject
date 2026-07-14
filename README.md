# Face Recognition Attendance System

Face Recognition based Attendance System

A Java desktop application for face-based attendance tracking, user management, and leave handling. The project combines JavaFX UI, OpenCV/JavaCV face detection, and SQL Server connectivity for a practical attendance workflow that can be demonstrated locally or over a network.

## Highlights

- Face recognition-based attendance capture
- Role-based user management for admin, teacher, and student workflows
- Leave request and attendance reporting support
- CSV export for reports
- Secure login flow with optional OTP support
- Bundled JavaFX, OpenCV, and JavaCV dependencies for easier setup

## Tech Stack

- Java 11+
- JavaFX
- OpenCV / JavaCV
- SQL Server
- Batch and shell scripts for build and run automation

## Project Structure

- `app/src/` - Java source code
- `app/resources/` - Cascade files and application resources
- `app/config/` - Local and example configuration
- `scripts/` - Build and run scripts
- `data/` - Local data, logs, and runtime storage

## Run the Project

### Windows

1. Open Command Prompt in the project folder.
2. Run:
   ```
   scripts\build.bat
   scripts\run.bat
   ```

The required Visual C++ runtime is already bundled in the project distribution, so a separate install is usually not needed.

### Linux or macOS

1. Open Terminal in the project folder.
2. Run:
   ```
   bash scripts/build.sh
   bash scripts/run.sh
   ```

## Configuration

Use [app/config/config.properties](app/config/config.properties) for local development, or the generated [dist/config.properties](dist/config.properties) after build.

Typical SQL Server settings:

- `db.server=<your-server-ip-or-hostname>`
- `db.name=FaceAttendanceDB`
- `db.username=sa`
- `db.password=<your-password>`

Choose one connection style:

- Instance mode: `db.instance=SQLEXPRESS`
- Port mode: leave `db.instance` empty and set `db.port=1433`

## Two-PC Demo Setup

This project supports a simple viva/demo setup where one machine hosts SQL Server and another machine runs the app.

1. Enable TCP/IP for the SQL Server instance on the host machine.
2. Open firewall access for the SQL port you are using.
3. Set `db.server` in the client configuration to the host machine IP address.
4. Start the app from the client machine using the normal run script.

Quick network check from the client machine:

```powershell
Test-NetConnection <SERVER_IP> -Port 1433
```

## Notes

- The repository is intentionally kept free of generated build output and local machine files.
- If you are sharing this project publicly, update the configuration file with non-sensitive example values only.
- Screenshots can be added later in the README if you want a stronger portfolio presentation.

## Troubleshooting

- Confirm that Java is installed and available in your PATH.
- Run the scripts from the project root so relative paths resolve correctly.
- If database connection fails, verify the SQL Server host, instance name, port, and firewall rules.
- Check the log files in `data/logs/` when debugging runtime issues.
