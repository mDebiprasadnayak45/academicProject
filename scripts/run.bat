@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
REM ===================================================
REM Face Recognition App - Launcher
REM ===================================================

title Face Recognition Attendance System

REM Get the directory where this launcher is located
set APP_DIR=%~dp0..

cd /d "%APP_DIR%"

REM Use JavaCPP cache for native binaries
set "JAVACPP_CACHE=%APP_DIR%\data\javacpp-cache"
if not exist "%JAVACPP_CACHE%" mkdir "%JAVACPP_CACHE%"
set "VC_RUNTIME_DIR=%APP_DIR%\dist\runtime\vc"
set "PATH=%JAVACPP_CACHE%;%VC_RUNTIME_DIR%;%PATH%"

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java is not installed or not in PATH
  echo Please install Java 17 or later and add it to PATH.
  echo.
  pause
  exit /b 1
)

REM Check build output
if not exist "%APP_DIR%\build\classes" (
  echo ERROR: build\classes not found!
  echo Run scripts\build.bat first.
  echo.
  pause
  exit /b 1
)

REM Check dist folder
if not exist "%APP_DIR%\dist" (
  echo ERROR: dist directory not found!
  echo Run scripts\build.bat first.
  echo.
  pause
  exit /b 1
)

REM Check JavaFX SDK
if not exist "%APP_DIR%\dist\javafx-sdk-17.0.2\lib" (
  echo ERROR: JavaFX SDK not found at dist\javafx-sdk-17.0.2\lib
  echo Please copy javafx-sdk-17.0.2 into dist\javafx-sdk-17.0.2
  echo.
  pause
  exit /b 1
)

REM Check JavaCV libraries
if not exist "%APP_DIR%\dist\javacv" (
  echo ERROR: JavaCV libraries not found in dist\javacv
  echo Please ensure JavaCV jars are copied into dist\javacv
  echo.
  pause
  exit /b 1
)

REM Check for Visual C++ Runtime (prefer bundled DLLs)
if exist "%VC_RUNTIME_DIR%\vcruntime140.dll" if exist "%VC_RUNTIME_DIR%\vcruntime140_1.dll" if exist "%VC_RUNTIME_DIR%\msvcp140.dll" (
  echo [OK] Using bundled Visual C++ runtime from dist\runtime\vc
) else (
  where vcruntime140.dll >nul 2>&1
  if errorlevel 1 (
    echo.
    echo WARNING: Visual C++ Runtime not found in bundled folder or PATH
    echo This may cause OpenCV DLL loading to fail.
    echo.
    if exist C:\Windows\System32\vcruntime140.dll if exist C:\Windows\System32\vcruntime140_1.dll if exist C:\Windows\System32\msvcp140.dll (
      echo Found Visual C++ runtime in System32
      set "PATH=C:\Windows\System32;!PATH!"
    ) else (
      echo.
      echo ERROR: Visual C++ Runtime not found!
      echo Include these files in dist\runtime\vc or install Visual C++ Redistributable 2015-2022 x64.
      echo Required: vcruntime140.dll, vcruntime140_1.dll, msvcp140.dll
      echo.
      pause
      exit /b 1
    )
  )
)

echo Starting Face Recognition Attendance System...
echo.

REM Run with all libraries and JavaFX
java -Xms512m -Xmx2048m ^
  -Dprism.order=sw ^
  -Dfile.encoding=UTF-8 ^
  -Dsun.stdout.encoding=UTF-8 ^
  -Dsun.stderr.encoding=UTF-8 ^
  -Djava.util.logging.ConsoleHandler.level=SEVERE ^
  -Djava.util.logging.SimpleFormatter.format=%%5$s%%n ^
  --module-path "%APP_DIR%\dist\javafx-sdk-17.0.2\lib" ^
  --add-modules javafx.controls,javafx.fxml,javafx.swing ^
  -Dorg.bytedeco.javacpp.cachedir="%JAVACPP_CACHE%" ^
  -cp "%APP_DIR%\dist;%APP_DIR%\build\classes;%APP_DIR%\dist\javacv\*;%APP_DIR%\dist\mssql-jdbc-12.8.1.jre11.jar;%APP_DIR%\dist\javafx-sdk-17.0.2\lib\*" ^
  FaceAttendanceApp

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with errors.
    pause
)
