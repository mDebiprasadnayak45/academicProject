#!/bin/bash
###################################################
# Face Recognition App - Mac/Linux Launcher
# Double-click or run this script to start the app
###################################################

# Get the directory where this script is located
APP_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"

cd "$APP_DIR"

# Set Java options
JAVA_OPTS="-Xms512m -Xmx2048m"
JAVAFX_PATH="$APP_DIR/libs/javafx-sdk-17.0.2/lib"
JAVACPP_CACHE="$APP_DIR/data/javacpp-cache"
mkdir -p "$JAVACPP_CACHE"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed!"
    echo "Please install Java 17 or later."
    read -p "Press Enter to exit..."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17 or later is required!"
    echo "Current Java version: $JAVA_VERSION"
    read -p "Press Enter to exit..."
    exit 1
fi

# Check build output
if [ ! -d "$APP_DIR/build/classes" ]; then
    echo "ERROR: build/classes not found!"
    echo "Run scripts/build.sh first."
    read -p "Press Enter to exit..."
    exit 1
fi

# Check JavaFX SDK
if [ ! -d "$JAVAFX_PATH" ]; then
    echo "ERROR: JavaFX SDK not found at $JAVAFX_PATH"
    echo "Please copy javafx-sdk-17.0.2 into libs/javafx-sdk-17.0.2"
    read -p "Press Enter to exit..."
    exit 1
fi

# Check JavaCV libraries
if [ ! -d "$APP_DIR/libs/javacv" ]; then
    echo "ERROR: JavaCV libraries not found in libs/javacv"
    echo "Please ensure JavaCV jars are copied into libs/javacv"
    read -p "Press Enter to exit..."
    exit 1
fi

# Launch the application
echo "Starting Face Recognition Attendance System..."
echo ""

java $JAVA_OPTS \
    -Djava.util.logging.ConsoleHandler.level=SEVERE \
    --module-path "$JAVAFX_PATH" \
    --add-modules javafx.controls,javafx.fxml,javafx.swing \
    -Dorg.bytedeco.javacpp.cachedir="$JAVACPP_CACHE" \
    -cp "$APP_DIR/build/classes:$APP_DIR/libs/javacv/*:$APP_DIR/libs/mssql-jdbc-12.8.1.jre11.jar:$APP_DIR/libs/javafx-sdk-17.0.2/lib/*" \
    FaceAttendanceApp

# Check exit code
if [ $? -ne 0 ]; then
    echo ""
    echo "Application exited with errors."
    read -p "Press Enter to exit..."
fi
