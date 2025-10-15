#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_ENV_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$BUILD_ENV_DIR")"
CONFIG_DIR="$BUILD_ENV_DIR/config"

# Source version configuration
source "$CONFIG_DIR/versions.txt"

TOOLS_DIR="$BUILD_ENV_DIR/tools"
BUILD_DIR="$BUILD_ENV_DIR/build"
KEYS_DIR="$BUILD_ENV_DIR/keys"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== ReVanced TikTok Share Sanitizer - Setup ===${NC}"
echo ""

# Check Java
echo -e "${YELLOW}Checking Java 17...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java not found. Install Java 17 and ensure it's in PATH.${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | sed -E 's/.*"([0-9]+).*/\1/' | head -1)
if [ "$JAVA_VERSION" != "17" ] && [ -n "$JAVA_VERSION" ]; then
    echo -e "${YELLOW}Warning: Java $JAVA_VERSION detected (Java 17 recommended)${NC}"
fi
echo -e "${GREEN}✓ Java OK${NC}"
echo ""

# Create directories
echo -e "${YELLOW}Creating directories...${NC}"
mkdir -p "$TOOLS_DIR" "$BUILD_DIR" "$KEYS_DIR"
echo -e "${GREEN}✓ Directories created${NC}"
echo ""

# Download ReVanced CLI
echo -e "${YELLOW}Setting up ReVanced CLI v$REVANCED_CLI_VERSION...${NC}"
CLI_JAR="$TOOLS_DIR/revanced-cli-$REVANCED_CLI_VERSION-all.jar"

if [ -f "$CLI_JAR" ]; then
    echo -e "${GREEN}✓ CLI already downloaded: $CLI_JAR${NC}"
else
    echo "Downloading from GitHub releases..."
    DOWNLOAD_URL="https://github.com/ReVanced/revanced-cli/releases/download/v$REVANCED_CLI_VERSION/revanced-cli-$REVANCED_CLI_VERSION-all.jar"
    
    if ! curl -L -o "$CLI_JAR" "$DOWNLOAD_URL" 2>/dev/null; then
        echo -e "${RED}ERROR: Failed to download CLI${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ CLI downloaded${NC}"
fi
echo ""

# Download integrations APK
echo -e "${YELLOW}Setting up ReVanced Integrations APK v$REVANCED_INTEGRATIONS_VERSION...${NC}"
INTEGRATIONS_APK="$TOOLS_DIR/revanced-integrations-$REVANCED_INTEGRATIONS_VERSION.apk"

if [ -f "$INTEGRATIONS_APK" ]; then
    echo -e "${GREEN}✓ Integrations APK already available: $INTEGRATIONS_APK${NC}"
else
    echo "Downloading from GitHub releases..."
    DOWNLOAD_URL="https://github.com/ReVanced/revanced-integrations/releases/download/v$REVANCED_INTEGRATIONS_VERSION/integrations-$REVANCED_INTEGRATIONS_VERSION.apk"
    
    if curl -L -o "$INTEGRATIONS_APK" "$DOWNLOAD_URL" 2>/dev/null; then
        echo -e "${GREEN}✓ Integrations APK downloaded${NC}"
    else
        echo -e "${RED}ERROR: Could not download integrations APK${NC}"
        echo "Ensure v$REVANCED_INTEGRATIONS_VERSION exists at: $DOWNLOAD_URL"
        exit 1
    fi
fi
echo ""

# Create signing keystore
echo -e "${YELLOW}Setting up signing keystore...${NC}"
KEYSTORE_FILE="$KEYS_DIR/tiktok-key.jks"

if [ -f "$KEYSTORE_FILE" ]; then
    echo -e "${GREEN}✓ Keystore already exists: $KEYSTORE_FILE${NC}"
else
    echo "Generating new keystore..."
    keytool -genkey -v \
        -keystore "$KEYSTORE_FILE" \
        -keyalg RSA \
        -keysize 2048 \
        -validity "$KEYSTORE_VALIDITY_DAYS" \
        -alias "$KEYSTORE_ALIAS" \
        -storepass "$KEYSTORE_PASSWORD" \
        -keypass "$KEYSTORE_PASSWORD" \
        -dname "CN=ReVanced,O=ReVanced,L=Internet,C=US" \
        2>&1 | grep -E "generated|Keystore"
    
    echo -e "${GREEN}✓ Keystore created${NC}"
fi
echo ""

# Create keystore properties file
KEYSTORE_PROPS="$KEYS_DIR/keystore.properties"
cat > "$KEYSTORE_PROPS" << EOF
keystore.file=$KEYSTORE_FILE
keystore.alias=$KEYSTORE_ALIAS
keystore.password=$KEYSTORE_PASSWORD
EOF
echo -e "${GREEN}✓ Keystore properties saved${NC}"
echo ""

# Summary
echo -e "${GREEN}=== Setup Complete ===${NC}"
echo ""
echo "Next steps:"
echo "1. Download TikTok APK from https://www.apkmirror.com/apk/tiktok-pte-ltd/tiktok/"
echo "2. Place it in: $BUILD_DIR/tiktok-base.apk"
echo "3. Run: ./build-env/scripts/2-build-patch.sh"
echo ""
echo "Tools ready at:"
echo "  - CLI: $CLI_JAR"
echo "  - Integrations: $INTEGRATIONS_APK"
echo "  - Keystore: $KEYSTORE_FILE"
echo ""
