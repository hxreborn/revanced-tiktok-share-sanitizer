#!/usr/bin/env bash
#
# TikTok Share Sanitizer - APK Testing Script
#
# This script automates the process of patching TikTok with your Share Sanitizer
# patch and installing it on a connected Android device.
#
# Prerequisites:
# - ReVanced CLI jar
# - ReVanced Patches jar (built from upstream)
# - ReVanced Integrations APK (built from upstream)
# - TikTok 36.5.4 APK
# - ADB installed and device connected
#
# Usage:
#   ./scripts/test-patch.sh [options]
#
# Options:
#   --cli <path>          Path to revanced-cli.jar
#   --patches <path>      Path to revanced-patches.jar
#   --integrations <path> Path to revanced-integrations.apk
#   --apk <path>          Path to TikTok APK
#   --only-sanitizer      Apply only Share Sanitizer patch (skip other patches)
#   --install             Install patched APK after patching
#   --logcat              Start logcat monitoring after install
#   --help                Show this help message

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default paths (adjust based on your setup)
CLI_JAR="${REVANCED_CLI:-./revanced-cli.jar}"
PATCHES_JAR="${REVANCED_PATCHES:-./revanced-patches.jar}"
INTEGRATIONS_APK="${REVANCED_INTEGRATIONS:-./revanced-integrations.apk}"
TIKTOK_APK="${TIKTOK_APK:-./apk/orig/tiktok-36.5.4.apk}"
OUTPUT_APK="./tiktok-patched.apk"
ONLY_SANITIZER=false
DO_INSTALL=false
DO_LOGCAT=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --cli)
            CLI_JAR="$2"
            shift 2
            ;;
        --patches)
            PATCHES_JAR="$2"
            shift 2
            ;;
        --integrations)
            INTEGRATIONS_APK="$2"
            shift 2
            ;;
        --apk)
            TIKTOK_APK="$2"
            shift 2
            ;;
        --only-sanitizer)
            ONLY_SANITIZER=true
            shift
            ;;
        --install)
            DO_INSTALL=true
            shift
            ;;
        --logcat)
            DO_LOGCAT=true
            shift
            ;;
        --help)
            grep '^#' "$0" | sed 's/^# //g' | tail -n +3 | head -n -1
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

check_file() {
    if [[ ! -f "$1" ]]; then
        log_error "File not found: $1"
        exit 1
    fi
}

check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "Command not found: $1"
        log_info "Install it and try again"
        exit 1
    fi
}

# Banner
echo -e "${BLUE}"
echo "======================================"
echo " TikTok Share Sanitizer - Test Script"
echo "======================================"
echo -e "${NC}"

# Validate prerequisites
log_info "Checking prerequisites..."

check_command "java"
check_command "adb"

check_file "$CLI_JAR"
check_file "$PATCHES_JAR"
check_file "$INTEGRATIONS_APK"
check_file "$TIKTOK_APK"

# Check ADB connection
if ! adb devices | grep -q "device$"; then
    log_warning "No Android device connected"
    log_info "Connect a device or start an emulator, then try again"
    exit 1
fi

log_success "All prerequisites met"

# List available patches
log_info "Listing available patches..."
echo ""

java -jar "$CLI_JAR" list-patches \
    --patch-bundle "$PATCHES_JAR" \
    | grep -A 3 "Share link sanitizer" || {
        log_error "Share link sanitizer patch not found in bundle"
        log_info "Make sure you've built the patch and added it to revanced-patches"
        exit 1
    }

echo ""
log_success "Share link sanitizer patch found"

# Build patch command
PATCH_CMD="java -jar \"$CLI_JAR\" patch \
    --patch-bundle \"$PATCHES_JAR\" \
    --integrations \"$INTEGRATIONS_APK\" \
    --out \"$OUTPUT_APK\""

if [[ "$ONLY_SANITIZER" == true ]]; then
    log_info "Applying ONLY Share link sanitizer patch..."
    PATCH_CMD="$PATCH_CMD --include \"Share link sanitizer\""
else
    log_info "Applying all recommended patches (including Share link sanitizer)..."
fi

PATCH_CMD="$PATCH_CMD \"$TIKTOK_APK\""

# Execute patching
log_info "Patching TikTok APK..."
log_info "This may take 2-5 minutes..."
echo ""

eval "$PATCH_CMD" || {
    log_error "Patching failed"
    exit 1
}

echo ""
log_success "Patching completed successfully!"
log_info "Output: $OUTPUT_APK"

# Verify patch was applied
log_info "Verifying patch application..."

# Check if patched APK contains expected extension class
VERIFY_CMD="unzip -p \"$OUTPUT_APK\" classes.dex | strings | grep -q \"ShareSanitizerHook\" && echo 'found' || echo 'not found'"

if [[ $(eval "$VERIFY_CMD") == "found" ]]; then
    log_success "ShareSanitizerHook found in patched APK"
else
    log_warning "Could not verify ShareSanitizerHook in APK"
    log_warning "This might be a false negative (dex may be compressed)"
fi

# Install if requested
if [[ "$DO_INSTALL" == true ]]; then
    log_info "Installing patched APK on device..."

    # Uninstall existing TikTok
    PKG_NAME="com.zhiliaoapp.musically"

    if adb shell pm list packages | grep -q "$PKG_NAME"; then
        log_info "Uninstalling existing TikTok..."
        adb uninstall "$PKG_NAME" || log_warning "Failed to uninstall existing app"
    fi

    # Install patched APK
    adb install "$OUTPUT_APK" || {
        log_error "Installation failed"
        log_info "Try installing manually: adb install $OUTPUT_APK"
        exit 1
    }

    log_success "Installation completed!"

    # Launch app
    log_info "Launching TikTok..."
    adb shell am start -n "$PKG_NAME/com.ss.android.ugc.aweme.splash.SplashActivity" || \
        log_warning "Could not launch app automatically"
fi

# Start logcat monitoring if requested
if [[ "$DO_LOGCAT" == true ]]; then
    echo ""
    log_info "Starting logcat monitoring..."
    log_info "Press Ctrl+C to stop"
    echo ""
    echo -e "${YELLOW}=== Logcat (ShareSanitizer + Clipboard) ===${NC}"
    echo ""

    adb logcat -c  # Clear existing logs
    adb logcat | grep -E "ShareSanitizer|Clipboard|revanced" --color=auto
fi

# Print summary
echo ""
echo -e "${GREEN}======================================"
echo "Test Summary"
echo -e "======================================${NC}"
echo "✅ Patch applied successfully"
echo "📦 Output: $OUTPUT_APK"

if [[ "$DO_INSTALL" == true ]]; then
    echo "📱 Installed on device"
fi

echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "1. Open TikTok app"
echo "2. Navigate to any video"
echo "3. Tap Share → Copy Link"
echo "4. Paste clipboard content to verify sanitization"
echo ""
echo "Monitor logs:"
echo "  adb logcat | grep -i ShareSanitizer"
echo ""
echo "See docs/TESTING.md for complete validation checklist"
echo ""
