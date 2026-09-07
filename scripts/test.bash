#!/bin/bash
set -euo pipefail

# ----- Determine project root (script is inside a subfolder) -----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT" || exit 1

echo "Project root: $PROJECT_ROOT"

# ----- Configuration -----
LIB_DIR="$PROJECT_ROOT/lib"
MAIN_SRC="$PROJECT_ROOT/src/main/java"
TEST_SRC="$PROJECT_ROOT/test"
OUT_DIR="$PROJECT_ROOT/target/test-classes"

# Check that the required Gson JAR exists
GSON_JAR="$LIB_DIR/gson-2.10.1.jar"
if [ ! -f "$GSON_JAR" ]; then
    echo "ERROR: $GSON_JAR not found. Please place gson-2.10.1.jar in $LIB_DIR"
    exit 1
fi

# ----- Clean previous test build -----
echo "Cleaning previous test build..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# ----- Locate source directories (with fallbacks) -----
if [ ! -d "$MAIN_SRC" ]; then
    MAIN_SRC="$PROJECT_ROOT/src"
fi
if [ ! -d "$TEST_SRC" ]; then
    TEST_SRC="$PROJECT_ROOT/test"   # keep as-is; if missing, find will just return nothing
fi

echo "Main source: $MAIN_SRC"
echo "Test source: $TEST_SRC"

# ----- Compile all Java files (main + test) together -----
echo "Compiling project and tests..."
# FIXED: -type f, and stderr redirection moved to the end
if ! find "$MAIN_SRC" "$TEST_SRC" -name "*.java" -type f 2>/dev/null \
    -exec javac -d "$OUT_DIR" -cp "$GSON_JAR" {} +; then
    echo "Compilation failed!" >&2
    exit 1
fi
echo "Compilation successful."

# ----- Runtime classpath -----
CP_RUNTIME="$OUT_DIR:$GSON_JAR"

# ----- Find all compiled test classes (ending with "Test.class") -----
TEST_CLASS_FILES=$(find "$OUT_DIR" -name "*Test.class" -type f)

if [ -z "$TEST_CLASS_FILES" ]; then
    echo "No test classes found."
    exit 0
fi

# ----- Run each test class individually -----
echo "Running tests..."
FAILED=0
for TEST_CLASS_FILE in $TEST_CLASS_FILES; do
    REL_PATH="${TEST_CLASS_FILE#$OUT_DIR/}"
    CLASS_NAME="$(echo "$REL_PATH" | sed 's/\.class$//' | tr '/' '.')"

    echo "Running $CLASS_NAME..."
    if java -cp "$CP_RUNTIME" "$CLASS_NAME"; then
        echo "[PASS] $CLASS_NAME"
    else
        echo "[FAIL] $CLASS_NAME"
        FAILED=1
    fi
done

exit $FAILED