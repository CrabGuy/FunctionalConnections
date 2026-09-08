#!/bin/bash
set -euo pipefail

# ----- Check Java Version (Requires >= 21) -----
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed -e 's/^1\.//' -e 's/\..*//' -e 's/-.*//' || echo "0")
if ! [[ "$JAVA_VERSION" =~ ^[0-9]+$ ]] || [ "$JAVA_VERSION" -lt 21 ]; then
    echo "ERROR: Java 21 or higher is required. Found version: $JAVA_VERSION" >&2
    exit 1
fi

# ----- Determine project root -----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."   # build.sh is in the project root
cd "$PROJECT_ROOT" || exit 1

echo "Project root: $PROJECT_ROOT"

# ----- Configuration -----
SERVER_MAIN="server.app.ServerMain"
CLIENT_MAIN="client.app.ClientMain"
LIB_DIR="$PROJECT_ROOT/lib"
OUT_DIR="$PROJECT_ROOT/out"
CLASSES_DIR="$PROJECT_ROOT/target/classes"

# Check that the required Gson JAR exists
GSON_JAR="$LIB_DIR/gson-2.10.1.jar"
if [ ! -f "$GSON_JAR" ]; then
    echo "ERROR: $GSON_JAR not found. Please place gson-2.10.1.jar in $LIB_DIR"
    exit 1
fi

# ----- Clean & prepare -----
echo "Cleaning previous build..."
# Remove only compiled classes (temporary)
rm -rf "$PROJECT_ROOT/target"
# Create output directory if missing (preserves existing .gitkeep)
mkdir -p "$OUT_DIR"
# Delete only JAR files inside OUT_DIR (keeps .gitkeep and other files)
find "$OUT_DIR" -maxdepth 1 -name "*.jar" -type f -delete

# Prepare class output directory
mkdir -p "$CLASSES_DIR"

# Locate source directory (maven style or flat)
SRC_DIR="$PROJECT_ROOT/src/main/java"
if [ ! -d "$SRC_DIR" ]; then
    SRC_DIR="$PROJECT_ROOT/src"
fi
echo "Using source directory: $SRC_DIR"

# ----- Compile -----
echo "Compiling project..."
if ! find "$SRC_DIR" -name "*.java" -type f -exec javac -d "$CLASSES_DIR" -cp "$GSON_JAR" {} +; then
    echo "Compilation failed!"
    exit 1
fi
echo "Compilation successful."

# ----- Function to package a single JAR -----
package_jar() {
    local main_class="$1"
    local jar_name="$2"
    echo "Packaging $jar_name..."
    
    local staging="$PROJECT_ROOT/target/staging_$jar_name"
    mkdir -p "$staging"
    
    # Copy compiled classes
    cp -R "$CLASSES_DIR"/. "$staging"/
    
    # Extract Gson classes into staging (create fat JAR)
    ( cd "$staging" && jar xf "$GSON_JAR" )
    
    # Create manifest
    echo "Main-Class: $main_class" > "$PROJECT_ROOT/target/manifest_$jar_name.txt"
    
    # Build JAR
    jar cfm "$OUT_DIR/$jar_name" "$PROJECT_ROOT/target/manifest_$jar_name.txt" -C "$staging" .
    
    # Clean staging
    rm -rf "$staging" "$PROJECT_ROOT/target/manifest_$jar_name.txt"
}

# ----- Package both applications -----
package_jar "$CLIENT_MAIN" "client.jar"
package_jar "$SERVER_MAIN" "server.jar"

# ----- Clean temporary compilation directory -----
rm -rf "$PROJECT_ROOT/target"

echo "Build complete. JARs are in $OUT_DIR/:"
ls -lh "$OUT_DIR"/*.jar