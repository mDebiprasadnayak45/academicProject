#!/bin/bash
###################################################
# Face Recognition App - Build & Package Script
# For Mac and Linux
###################################################

echo ""
echo "============================================"
echo "   Building Face Recognition Application"
echo "============================================"
echo ""

# Get script directory
PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
SRC_DIR="$PROJECT_DIR/app/src"
BIN_DIR="$PROJECT_DIR/build/classes"
LIB_DIR="$PROJECT_DIR/libs"
RESOURCES_DIR="$PROJECT_DIR/app/resources"
CONFIG_DIR="$PROJECT_DIR/app/config"
DIST_DIR="$PROJECT_DIR/dist"
JAVAFX_DIR="$PROJECT_DIR/libs/javafx-sdk-17.0.2"
NATIVE_LIBS="$PROJECT_DIR/native"

# Clean previous build
echo "[1/5] Cleaning previous build..."
rm -rf "$BIN_DIR"
rm -rf "$DIST_DIR"
mkdir -p "$BIN_DIR"
mkdir -p "$DIST_DIR"
mkdir -p "$PROJECT_DIR/data/face_data" "$PROJECT_DIR/data/logs"

# Compile Java source files
echo "[2/5] Compiling source files..."
javac -encoding UTF-8 -d "$BIN_DIR" \
  --module-path "$JAVAFX_DIR/lib" \
  --add-modules javafx.controls,javafx.fxml,javafx.swing \
  -cp "$LIB_DIR/mssql-jdbc-12.8.1.jre11.jar:$LIB_DIR/javacv/*" \
  "$SRC_DIR"/*.java

if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed!"
    exit 1
fi

# Copy dependencies to dist
echo "[3/3] Copying dependencies..."
mkdir -p "$DIST_DIR/lib"
cp "$LIB_DIR/mssql-jdbc-12.8.1.jre11.jar" "$DIST_DIR/lib/"
cp "$LIB_DIR/opencv-4120.jar" "$DIST_DIR/lib/"
cp "$LIB_DIR/javacv"/*.jar "$DIST_DIR/lib/" 2>/dev/null

# Build complete - JavaFX SDK and JavaCV already in dist folder
chmod +x "$DIST_DIR/run.sh"

# Copy configuration and resources to classpath
echo "Copying configuration and resources..."
[ -f "$CONFIG_DIR/config.properties" ] && cp "$CONFIG_DIR/config.properties" "$BIN_DIR/"
[ -f "$RESOURCES_DIR/haarcascade_frontalface_default.xml" ] && cp "$RESOURCES_DIR/haarcascade_frontalface_default.xml" "$BIN_DIR/"
[ -f "$CONFIG_DIR/config.properties" ] && cp "$CONFIG_DIR/config.properties" "$DIST_DIR/"
[ -f "$PROJECT_DIR/database.properties" ] && cp "$PROJECT_DIR/database.properties" "$DIST_DIR/"

echo ""
echo "============================================"
echo "   Build Complete!"
echo "============================================"
echo ""
echo "Application created in: $DIST_DIR"
echo "Run scripts/run.sh to start the application"
echo ""
