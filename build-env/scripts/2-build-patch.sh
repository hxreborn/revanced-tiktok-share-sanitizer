#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_ENV_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$BUILD_ENV_DIR")"
BUILD_DIR="$BUILD_ENV_DIR/build"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}=== Building ReVanced Patch JAR ===${NC}"
echo ""

# Check if gradlew exists
GRADLEW="$PROJECT_DIR/gradlew"
if [ ! -f "$GRADLEW" ]; then
    echo -e "${RED}ERROR: gradlew not found in $PROJECT_DIR${NC}"
    exit 1
fi
chmod +x "$GRADLEW"

cd "$PROJECT_DIR"

echo -e "${YELLOW}Running tests...${NC}"
if ! "$GRADLEW" test; then
    echo -e "${RED}ERROR: Tests failed. Fix issues before building JAR.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Tests passed${NC}"
echo ""

echo -e "${YELLOW}Building patch JAR...${NC}"
if ! "$GRADLEW" build; then
    echo -e "${RED}ERROR: Build failed.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Build complete${NC}"
echo ""

# Find and copy the JAR
echo -e "${YELLOW}Locating compiled JAR...${NC}"
JAR_FILES=$(find "$PROJECT_DIR/build/libs" -name "*.jar" -type f)

if [ -z "$JAR_FILES" ]; then
    echo -e "${RED}ERROR: No JAR files found in build/libs/${NC}"
    exit 1
fi

# Copy all JARs to build-env
mkdir -p "$BUILD_DIR"
for jar in $JAR_FILES; do
    BASENAME=$(basename "$jar")
    cp "$jar" "$BUILD_DIR/$BASENAME"
    echo -e "${GREEN}✓ Copied: $BASENAME${NC}"
done

# Look for the main patch JAR (without -sources or -javadoc)
PATCH_JAR=$(ls -t "$BUILD_DIR"/*.jar 2>/dev/null | grep -v sources | grep -v javadoc | head -1)

if [ -z "$PATCH_JAR" ]; then
    echo -e "${RED}ERROR: Could not find patch JAR${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}=== Build Complete ===${NC}"
echo ""
echo "Patch JAR ready at:"
echo "  $PATCH_JAR"
echo ""
echo "Next step: Run ./build-env/scripts/3-patch-apk.sh"
echo ""
