#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_ENV_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_DIR="$BUILD_ENV_DIR/config"
BUILD_DIR="$BUILD_ENV_DIR/build"
KEYS_DIR="$BUILD_ENV_DIR/keys"

# Source configuration
source "$CONFIG_DIR/versions.txt"

# Read keystore properties
KEYSTORE_FILE="$KEYS_DIR/tiktok-key.jks"
KEYSTORE_PASSWORD="revanced-tiktok-share-sanitizer"
KEYSTORE_ALIAS="tiktok-sanitizer"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}=== Signing APK ===${NC}"
echo ""

INPUT_APK="$BUILD_DIR/tiktok-patched.apk"
OUTPUT_APK="$BUILD_DIR/tiktok-signed.apk"

# Validate input APK
if [ ! -f "$INPUT_APK" ]; then
    echo -e "${RED}ERROR: Patched APK not found: $INPUT_APK${NC}"
    echo "Run: ./build-env/scripts/3-patch-apk.sh"
    exit 1
fi
echo -e "${GREEN}✓ Input APK: $(basename "$INPUT_APK")${NC}"

if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${RED}ERROR: Keystore not found: $KEYSTORE_FILE${NC}"
    echo "Run: ./build-env/scripts/1-setup.sh"
    exit 1
fi
echo -e "${GREEN}✓ Keystore: $(basename "$KEYSTORE_FILE")${NC}"
echo ""

# Try apksigner first (from Android SDK)
SIGNING_TOOL=""

if command -v apksigner &> /dev/null; then
    SIGNING_TOOL="apksigner"
    echo -e "${YELLOW}Using apksigner (Android SDK)...${NC}"
elif [ -d "$ANDROID_HOME/build-tools" ]; then
    # Look for apksigner in Android SDK
    APKSIGNER_PATH=$(find "$ANDROID_HOME/build-tools" -name "apksigner" | head -1)
    if [ -n "$APKSIGNER_PATH" ]; then
        SIGNING_TOOL="$APKSIGNER_PATH"
        echo -e "${YELLOW}Using apksigner from Android SDK...${NC}"
    fi
fi

# Fallback to jarsigner
if [ -z "$SIGNING_TOOL" ] || [ "$SIGNING_TOOL" = "jarsigner" ]; then
    SIGNING_TOOL="jarsigner"
    echo -e "${YELLOW}Using jarsigner (Java built-in)...${NC}"
    echo -e "${YELLOW}Warning: jarsigner creates V1-only signatures.${NC}"
    echo -e "${YELLOW}For better compatibility, install Android SDK and use apksigner.${NC}"
fi

echo ""

# Sign the APK
echo -e "${YELLOW}Signing APK...${NC}"

if [ "$SIGNING_TOOL" = "jarsigner" ]; then
    # Use jarsigner
    if jarsigner -verbose \
        -keystore "$KEYSTORE_FILE" \
        -storepass "$KEYSTORE_PASSWORD" \
        -keypass "$KEYSTORE_PASSWORD" \
        -sigalg SHA256withRSA \
        -digestalg SHA-256 \
        "$INPUT_APK" \
        "$KEYSTORE_ALIAS"; then
        
        echo -e "${GREEN}✓ APK signed with jarsigner${NC}"
        mv "$INPUT_APK" "$OUTPUT_APK"
    else
        echo -e "${RED}ERROR: jarsigner failed${NC}"
        exit 1
    fi
else
    # Use apksigner
    if $SIGNING_TOOL sign \
        --ks "$KEYSTORE_FILE" \
        --ks-pass "pass:$KEYSTORE_PASSWORD" \
        --ks-key-alias "$KEYSTORE_ALIAS" \
        --key-pass "pass:$KEYSTORE_PASSWORD" \
        --v4-signing-enabled true \
        --out "$OUTPUT_APK" \
        "$INPUT_APK"; then
        
        echo -e "${GREEN}✓ APK signed with apksigner${NC}"
    else
        echo -e "${RED}ERROR: apksigner failed${NC}"
        exit 1
    fi
fi

echo ""

# Verify signature
echo -e "${YELLOW}Verifying signature...${NC}"

if jarsigner -verify -verbose "$OUTPUT_APK" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Signature verified${NC}"
else
    echo -e "${YELLOW}Warning: Could not verify signature (non-critical)${NC}"
fi

echo ""
echo -e "${GREEN}=== Signing Complete ===${NC}"
echo ""
echo "Signed APK ready for installation:"
echo "  $OUTPUT_APK"
echo ""
echo "To install on device:"
echo "  adb install $OUTPUT_APK"
echo ""
echo "Or copy to device and install manually:"
echo "  File size: $(du -h "$OUTPUT_APK" | cut -f1)"
echo ""
