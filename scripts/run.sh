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
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"   # script is in root
cd "$PROJECT_ROOT" || exit 1

echo "Project root: $PROJECT_ROOT"

# ----- Configuration -----
SERVER_MAIN="server.app.ServerMain"
CLIENT_MAIN="client.app.ClientMain"
LIB_DIR="$PROJECT_ROOT/lib"
OUT_DIR="$PROJECT_ROOT/target/classes"
SERVER_PORT=8080
SERVER_LOG="$PROJECT_ROOT/server.log"

# Check that the required Gson JAR exists
GSON_JAR="$LIB_DIR/gson-2.10.1.jar"
if [ ! -f "$GSON_JAR" ]; then
    echo "ERROR: $GSON_JAR not found. Please place gson-2.10.1.jar in $LIB_DIR"
    exit 1
fi

# ----- Clean & prepare -----
echo "Cleaning previous build..."
rm -rf "$PROJECT_ROOT/target"
mkdir -p "$OUT_DIR"

# Locate source directory (maven style or flat)
SRC_DIR="$PROJECT_ROOT/src/main/java"
if [ ! -d "$SRC_DIR" ]; then
    SRC_DIR="$PROJECT_ROOT/src"
fi
echo "Using source directory: $SRC_DIR"

# ----- Compile (using find -exec to handle many files safely) -----
echo "Compiling project..."
if ! find "$SRC_DIR" -name "*.java" -type f -exec javac -d "$OUT_DIR" -cp "$GSON_JAR" {} +; then
    echo "Compilation failed!"
    exit 1
fi
echo "Compilation successful."

# ----- Runtime classpath -----
CP_RUNTIME="$OUT_DIR:$GSON_JAR"

# ----- Start server in background -----
echo "Starting server (logging to $SERVER_LOG)..."
java -cp "$CP_RUNTIME" "$SERVER_MAIN" > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!

# ----- Cleanup trap (kills server + removes target) -----
cleanup() {
    echo "Cleaning up..."
    if kill -0 "$SERVER_PID" 2>/dev/null; then
        kill "$SERVER_PID" 2>/dev/null
        wait "$SERVER_PID" 2>/dev/null || true
    fi
    echo "Removing compiled classes..."
    rm -rf "$PROJECT_ROOT/target"
    echo "Done."
}
trap cleanup EXIT

# ----- Wait for server to be ready (port check) -----
echo "Waiting for server on port $SERVER_PORT..."
while ! timeout 1 bash -c "echo >/dev/tcp/localhost/$SERVER_PORT" 2>/dev/null; do
    # Fallback: try using nc or lsof if /dev/tcp is not available
    if command -v nc &>/dev/null; then
        if nc -z localhost "$SERVER_PORT" 2>/dev/null; then
            break
        fi
    elif command -v lsof &>/dev/null; then
        if lsof -i :"$SERVER_PORT" >/dev/null 2>&1; then
            break
        fi
    fi
    sleep 1
done
echo "Server is ready!"

# ----- Run client -----
echo "Starting client..."
java -cp "$CP_RUNTIME" "$CLIENT_MAIN"

echo "Client exited. Shutting down..."