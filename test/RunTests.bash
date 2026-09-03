#!/bin/bash

# ----- Determine project root (script is inside test/) -----
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT" || exit 1

echo "Project root: $PROJECT_ROOT"

# ----- Configuration -----
LIB_DIR="$PROJECT_ROOT/lib"
MAIN_SRC="$PROJECT_ROOT/src/main/java"
TEST_SRC="$PROJECT_ROOT/test"
OUT_DIR="$PROJECT_ROOT/target/test-classes"

# ----- Clean previous test build -----
echo "Cleaning previous test build..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# ----- Collect all Java files (main + test) -----
MAIN_JAVA_FILES=$(find "$MAIN_SRC" -name "*.java" -type f 2>/dev/null)
TEST_JAVA_FILES=$(find "$TEST_SRC" -name "*.java" -type f 2>/dev/null)
ALL_JAVA_FILES="$MAIN_JAVA_FILES $TEST_JAVA_FILES"

if [ -z "$ALL_JAVA_FILES" ]; then
    echo "ERROR: No Java files found"
    exit 1
fi

echo "Found $(echo "$ALL_JAVA_FILES" | wc -w) Java files."

# ----- Classpath -----
CP_COMPILE="$LIB_DIR/gson-2.10.1.jar"
CP_RUNTIME="$OUT_DIR:$CP_COMPILE"

# ----- Compile everything -----
echo "Compiling project and tests..."
if ! javac -d "$OUT_DIR" -cp "$CP_COMPILE" $ALL_JAVA_FILES; then
    echo "Compilation failed!"
    exit 1
fi
echo "Compilation successful."

# ----- Find all test classes (compiled) -----
TEST_CLASS_FILES=$(find "$OUT_DIR" -name "*Test.class" -type f)

if [ -z "$TEST_CLASS_FILES" ]; then
    echo "No test classes found."
    exit 0
fi

# ----- Run each test class -----
echo "Running tests..."
FAILED=0
for TEST_CLASS_FILE in $TEST_CLASS_FILES; do
    # Convert file path to fully qualified class name:
    # Remove leading OUT_DIR and trailing .class, replace '/' with '.'
    REL_PATH="${TEST_CLASS_FILE#$OUT_DIR/}"
    CLASS_NAME="${REL_PATH%.class}"
    CLASS_NAME="${CLASS_NAME//\//.}"
    echo "Running $CLASS_NAME..."
    if java -cp "$CP_RUNTIME" "$CLASS_NAME"; then
        echo "[PASS] $CLASS_NAME"
    else
        echo "[FAIL] $CLASS_NAME"
        FAILED=1
    fi
done

exit $FAILED