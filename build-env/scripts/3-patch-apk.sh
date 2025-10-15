#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_ENV_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_DIR="$BUILD_ENV_DIR/config"
TOOLS_DIR="$BUILD_ENV_DIR/tools"
BUILD_DIR="$BUILD_ENV_DIR/build"

# Source version configuration
source "$CONFIG_DIR/versions.txt"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}=== Applying Patches with ReVanced CLI ===${NC}"
echo ""

# Locate files
BASE_APK="$BUILD_DIR/tiktok-base.apk"
PATCH_JAR=$(ls -t "$BUILD_DIR"/*.jar 2>/dev/null | grep -v sources | grep -v javadoc | head -1)
INTEGRATIONS_APK="$TOOLS_DIR/revanced-integrations-$REVANCED_INTEGRATIONS_VERSION.apk"
CLI_JAR="$TOOLS_DIR/revanced-cli-$REVANCED_CLI_VERSION-all.jar"
OUTPUT_APK="$BUILD_DIR/tiktok-patched.apk"

# Validate all required files exist
echo -e "${YELLOW}Checking required files...${NC}"

if [ ! -f "$BASE_APK" ]; then
    echo -e "${RED}ERROR: Base APK not found: $BASE_APK${NC}"
    echo "Please download TikTok APK from https://www.apkmirror.com/apk/tiktok-pte-ltd/tiktok/"
    echo "and place it at: $BASE_APK"
    exit 1
fi
echo -e "${GREEN}✓ Base APK: $(basename "$BASE_APK")${NC}"

if [ ! -f "$PATCH_JAR" ]; then
    echo -e "${RED}ERROR: Patch JAR not found: $PATCH_JAR${NC}"
    echo "Run: ./build-env/scripts/2-build-patch.sh"
    exit 1
fi
echo -e "${GREEN}✓ Patch JAR: $(basename "$PATCH_JAR")${NC}"

if [ ! -f "$INTEGRATIONS_APK" ]; then
    echo -e "${RED}ERROR: Integrations APK not found: $INTEGRATIONS_APK${NC}"
    echo "Run: ./build-env/scripts/1-setup.sh"
    exit 1
fi
echo -e "${GREEN}✓ Integrations APK: $(basename "$INTEGRATIONS_APK")${NC}"

if [ ! -f "$CLI_JAR" ]; then
    echo -e "${RED}ERROR: CLI JAR not found: $CLI_JAR${NC}"
    echo "Run: ./build-env/scripts/1-setup.sh"
    exit 1
fi
echo -e "${GREEN}✓ CLI JAR: $(basename "$CLI_JAR")${NC}"

echo ""

# Run revanced-cli
echo -e "${YELLOW}Running ReVanced CLI...${NC}"
echo "Command:"
echo "  java -jar $CLI_JAR patch \\"
echo "    -p=$(basename "$PATCH_JAR") \\"
echo "    -o=$(basename "$OUTPUT_APK") \\"
echo "    $BASE_APK"
echo ""

# Add OkHttp to classpath
OKHTTP_JAR="$HOME/.gradle/caches/modules-2/files-2.1/com.squareup.okhttp3/okhttp/5.0.0-alpha.14/c59864766ffc0d0dd4394ec0af5e3b60bf83d10f/okhttp-5.0.0-alpha.14.jar"

CLI_CLASSPATH="$CLI_JAR:$PATCH_JAR"
if [ -f "$OKHTTP_JAR" ]; then
    CLI_CLASSPATH="$CLI_CLASSPATH:$OKHTTP_JAR"
fi

if java -cp "$CLI_CLASSPATH" app.revanced.cli.command.MainCommandKt patch \
    -p="$PATCH_JAR" \
    -o="$OUTPUT_APK" \
    "$BASE_APK"; then
    
    echo ""
    echo -e "${GREEN}✓ Patching successful${NC}"
    echo ""
    echo -e "${GREEN}=== Patched APK Ready ===${NC}"
    echo ""
    echo "Patched APK:"
    echo "  $OUTPUT_APK"
    echo ""
    echo "Next step: Run ./build-env/scripts/4-sign.sh"
    echo ""
else
    echo ""
    echo -e "${RED}ERROR: Patching failed${NC}"
    echo "Check errors above for details."
    exit 1
fi
