@echo off
setlocal enabledelayedexpansion
REM ===================================================
REM Face Recognition App - Build & Package Script
REM ===================================================

echo.
echo ============================================
echo   Building Face Recognition Application
echo ============================================
echo.

REM Set project directories
set PROJECT_DIR=%~dp0..
set SRC_DIR=%PROJECT_DIR%\app\src
set CLASSES_DIR=%PROJECT_DIR%\build\classes
set CONFIG_DIR=%PROJECT_DIR%\app\config
set DIST_DIR=%PROJECT_DIR%\dist
set JAVAFX_DIR=%PROJECT_DIR%\dist\javafx-sdk-17.0.2

REM Build classpath with all required JARs
set CLASSPATH=%PROJECT_DIR%\dist\mssql-jdbc-12.8.1.jre11.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\javacv-1.5.11.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\javacpp-1.5.11.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\javacpp-1.5.11-windows-x86_64.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\opencv-4.10.0-1.5.11.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\opencv-4.10.0-1.5.11-windows-x86_64.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\ffmpeg-7.1.1-1.5.11.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\ffmpeg-7.1.1-1.5.11-windows-x86_64.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\videoinput-0.200-1.5.9.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\videoinput-0.200-1.5.9-windows-x86_64.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\openblas-0.3.28-1.5.11.jar
set CLASSPATH=%CLASSPATH%;%PROJECT_DIR%\dist\javacv\openblas-0.3.28-1.5.11-windows-x86_64.jar
set CLASSPATH=%CLASSPATH%;%JAVAFX_DIR%\lib\javafx.base.jar
set CLASSPATH=%CLASSPATH%;%JAVAFX_DIR%\lib\javafx.controls.jar
set CLASSPATH=%CLASSPATH%;%JAVAFX_DIR%\lib\javafx.fxml.jar
set CLASSPATH=%CLASSPATH%;%JAVAFX_DIR%\lib\javafx.graphics.jar
set CLASSPATH=%CLASSPATH%;%JAVAFX_DIR%\lib\javafx.swing.jar


REM Validate JavaFX SDK path
if not exist "%JAVAFX_DIR%\lib" (
  echo ERROR: JavaFX SDK not found at %JAVAFX_DIR%
  echo Please copy javafx-sdk-17.0.2 into dist\javafx-sdk-17.0.2
  exit /b 1
)


REM Clean previous build
echo [1/3] Cleaning previous build...
if exist "%CLASSES_DIR%" rmdir /s /q "%CLASSES_DIR%"
mkdir "%CLASSES_DIR%"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

REM Ensure data directories exist
if not exist "%PROJECT_DIR%\data\face_data" mkdir "%PROJECT_DIR%\data\face_data"
if not exist "%PROJECT_DIR%\data\logs" mkdir "%PROJECT_DIR%\data\logs"

REM Compile Java source files
echo [2/3] Compiling source files...

REM Expand all .java files in SRC_DIR and compile them
pushd "%SRC_DIR%"
set JAVA_FILES=
for %%f in (*.java) do set JAVA_FILES=!JAVA_FILES! "%SRC_DIR%\%%f"
popd

javac -encoding UTF-8 -d "%CLASSES_DIR%" ^
  -cp "%CLASSPATH%" ^
  !JAVA_FILES!

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Compilation failed!
    exit /b 1
)


REM Copy configuration and resources to build classes directory
echo Copying configuration and resources to classpath...
if exist "%CONFIG_DIR%\config.properties" copy "%CONFIG_DIR%\config.properties" "%CLASSES_DIR%\" >nul
if exist "%PROJECT_DIR%\app\resources\haarcascade_frontalface_default.xml" copy "%PROJECT_DIR%\app\resources\haarcascade_frontalface_default.xml" "%CLASSES_DIR%\" >nul
if exist "%PROJECT_DIR%\app\resources\javacv-platform-1.5.11-bin\samples\haarcascade_frontalface_alt2.xml" copy "%PROJECT_DIR%\app\resources\javacv-platform-1.5.11-bin\samples\haarcascade_frontalface_alt2.xml" "%CLASSES_DIR%\" >nul

REM Visually separate the next step
echo.
REM Copy dependencies to dist
echo [3/3] Copying dependencies and finishing...
REM This step is no longer needed as we assume dependencies are already in the dist folder.
REM You can add commands here to copy from a central 'libs' folder if you have one.
echo Dependencies are expected to be in the 'dist' folder.

REM Build complete - all dependencies already in dist folder

REM Copy configuration files
echo Copying configuration files...
if exist "%CONFIG_DIR%\config.properties" copy "%CONFIG_DIR%\config.properties" "%DIST_DIR%\" >nul

echo.
echo ============================================
echo   Build Complete!
echo ============================================
echo.
echo Application created in: %DIST_DIR%
echo Run scripts\run.bat to start the application
echo.
