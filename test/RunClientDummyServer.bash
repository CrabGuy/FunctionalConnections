#!/bin/bash

# ----- Determine project root (since script is inside test/) -----
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT" || exit 1

echo "Project root: $PROJECT_ROOT"

# ----- Configuration -----
SERVER_MAIN="server.DummyServerMain"    # Change to "server.DummyServerMain" if needed
CLIENT_MAIN="client.ClientMain"
LIB_DIR="$PROJECT_ROOT/lib"
OUT_DIR="$PROJECT_ROOT/target/classes"
SERVER_PORT=5000
SERVER_LOG="$PROJECT_ROOT/server.log"

# ----- Clean previous build -----
echo "Cleaning previous build..."
rm -rf "$PROJECT_ROOT/target"       # Remove entire target directory
mkdir -p "$OUT_DIR"

# ----- Locate source files -----
SRC_DIR="$PROJECT_ROOT/src/main/java"
if [ ! -d "$SRC_DIR" ]; then
    SRC_DIR="$PROJECT_ROOT/src"
fi
echo "Using source directory: $SRC_DIR"

JAVA_FILES=$(find "$SRC_DIR" -name "*.java" -type f)
if [ -z "$JAVA_FILES" ]; then
    echo "ERROR: No Java files found in $SRC_DIR"
    echo "Please check your folder structure."
    exit 1
fi
echo "Found $(echo "$JAVA_FILES" | wc -l) Java files."

# ----- Compile -----
echo "Compiling project..."
CP_COMPILE="$LIB_DIR/gson-2.10.1.jar"

if ! javac -d "$OUT_DIR" -cp "$CP_COMPILE" $JAVA_FILES; then
    echo "Compilation failed!"
    exit 1
fi
echo "Compilation successful."

# ----- Runtime classpath -----
CP_RUNTIME="$OUT_DIR:$CP_COMPILE"

# ----- Start server in background -----
echo "Starting server (logging to $SERVER_LOG)..."
java -cp "$CP_RUNTIME" "$SERVER_MAIN" > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!

# Cleanup on exit
cleanup() {
    echo "Cleaning up..."
    if kill -0 $SERVER_PID 2>/dev/null; then
        kill $SERVER_PID 2>/dev/null
        wait $SERVER_PID 2>/dev/null
    fi
    echo "Removing compiled classes..."
    rm -rf "$PROJECT_ROOT/target"   # Remove entire target directory
    echo "Done."
}
trap cleanup EXIT

# ----- Wait for server -----
echo "Waiting for server on port $SERVER_PORT..."
while ! timeout 1 bash -c "</dev/tcp/localhost/$SERVER_PORT" 2>/dev/null; do
    sleep 1
done
echo "Server is ready!"

# ----- Run client -----
echo "Starting client..."
java -cp "$CP_RUNTIME" "$CLIENT_MAIN"

# Client exits; cleanup triggers automatically.
echo "Client exited. Shutting down..."