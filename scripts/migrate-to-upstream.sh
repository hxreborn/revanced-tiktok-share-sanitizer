#!/usr/bin/env bash
#
# Migrate Share Sanitizer Patch to Upstream Repos
#
# This script copies the patch implementation from this standalone incubator
# to your local forks of revanced-patches and revanced-integrations.
#
# Prerequisites:
# - revanced-patches fork at: /Users/rafa/Documents/GitHub/revanced-patches
# - revanced-integrations fork at: /Users/rafa/Documents/GitHub/revanced-integrations
# - Feature branch: feat/tiktok-share-sanitizer (created automatically)
#
# Usage:
#   ./scripts/migrate-to-upstream.sh [--dry-run]
#
# Options:
#   --dry-run    Show what would be copied without making changes
#   --help       Show this help message

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INCUBATOR_ROOT="$(dirname "$SCRIPT_DIR")"
PATCHES_REPO="/Users/rafa/Documents/GitHub/revanced-patches"
INTEGRATIONS_REPO="/Users/rafa/Documents/GitHub/revanced-integrations"
BRANCH_NAME="feat/tiktok-share-sanitizer"

DRY_RUN=false

# Parse args
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
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

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_dry_run() { echo -e "${YELLOW}[DRY-RUN]${NC} $1"; }

check_dir() {
    if [[ ! -d "$1" ]]; then
        log_error "Directory not found: $1"
        exit 1
    fi
}

# Banner
echo -e "${BLUE}"
echo "=================================================="
echo " Migrate Share Sanitizer to Upstream Repos"
echo "=================================================="
echo -e "${NC}"

if [[ "$DRY_RUN" == true ]]; then
    log_warning "DRY-RUN MODE: No changes will be made"
    echo ""
fi

# Validate directories
log_info "Validating repositories..."
check_dir "$INCUBATOR_ROOT"
check_dir "$PATCHES_REPO"
check_dir "$INTEGRATIONS_REPO"
log_success "All repositories found"

# Check git status
log_info "Checking git status..."
cd "$PATCHES_REPO"
if [[ -n $(git status --porcelain) ]]; then
    log_warning "revanced-patches has uncommitted changes"
    log_info "Commit or stash changes before migration"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

cd "$INTEGRATIONS_REPO"
if [[ -n $(git status --porcelain) ]]; then
    log_warning "revanced-integrations has uncommitted changes"
    log_info "Commit or stash changes before migration"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# ============================================================
# PART 1: Migrate Patch to revanced-patches
# ============================================================

echo ""
log_info "PART 1: Migrating patch to revanced-patches..."

cd "$PATCHES_REPO"

# Create/switch to feature branch
if git rev-parse --verify "$BRANCH_NAME" >/dev/null 2>&1; then
    log_info "Switching to existing branch: $BRANCH_NAME"
    if [[ "$DRY_RUN" == false ]]; then
        git checkout "$BRANCH_NAME"
    else
        log_dry_run "Would checkout: $BRANCH_NAME"
    fi
else
    log_info "Creating new branch: $BRANCH_NAME"
    if [[ "$DRY_RUN" == false ]]; then
        git checkout -b "$BRANCH_NAME"
    else
        log_dry_run "Would create branch: $BRANCH_NAME"
    fi
fi

# Define source and destination paths
PATCH_SRC="$INCUBATOR_ROOT/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer"
PATCH_DST="$PATCHES_REPO/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer"

TEST_SRC="$INCUBATOR_ROOT/src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer"
TEST_DST="$PATCHES_REPO/patches/src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer"

log_info "Copying patch sources..."

# Files to copy to patches repo (patch-only files)
PATCH_FILES=(
    "ShareSanitizerPatch.kt"
    "fingerprints/ClipboardCopyFingerprint.kt"
)

if [[ "$DRY_RUN" == false ]]; then
    mkdir -p "$PATCH_DST/fingerprints"

    for file in "${PATCH_FILES[@]}"; do
        if [[ -f "$PATCH_SRC/$file" ]]; then
            cp "$PATCH_SRC/$file" "$PATCH_DST/$file"
            log_success "Copied: $file"
        else
            log_warning "Skipped (not found): $file"
        fi
    done

    # Copy tests
    if [[ -d "$TEST_SRC" ]]; then
        mkdir -p "$TEST_DST"
        # Only copy core logic tests (not hook tests, those go to integrations)
        for file in UrlNormalizerTest.kt ShortlinkExpanderTest.kt OkHttpClientAdapterTest.kt; do
            if [[ -f "$TEST_SRC/$file" ]]; then
                cp "$TEST_SRC/$file" "$TEST_DST/$file"
                log_success "Copied test: $file"
            fi
        done
    fi
else
    log_dry_run "Would copy ${#PATCH_FILES[@]} patch files"
    log_dry_run "Would copy 3 test files"
fi

log_success "Patch migration complete"

# ============================================================
# PART 2: Migrate Extension to revanced-integrations
# ============================================================

echo ""
log_info "PART 2: Migrating extension to revanced-integrations..."

cd "$INTEGRATIONS_REPO"

# Create/switch to feature branch
if git rev-parse --verify "$BRANCH_NAME" >/dev/null 2>&1; then
    log_info "Switching to existing branch: $BRANCH_NAME"
    if [[ "$DRY_RUN" == false ]]; then
        git checkout "$BRANCH_NAME"
    else
        log_dry_run "Would checkout: $BRANCH_NAME"
    fi
else
    log_info "Creating new branch: $BRANCH_NAME"
    if [[ "$DRY_RUN" == false ]]; then
        git checkout -b "$BRANCH_NAME"
    else
        log_dry_run "Would create branch: $BRANCH_NAME"
    fi
fi

# Extension destination (Java directory, not Kotlin)
EXT_DST="$INTEGRATIONS_REPO/app/src/main/java/app/revanced/extension/tiktok/sharesanitizer"

log_info "Copying extension sources..."

# Files to copy to integrations repo (core logic + hook)
EXTENSION_FILES=(
    "UrlNormalizer.kt"
    "ShortlinkExpander.kt"
    "HttpClient.kt"
    "OkHttpClientAdapter.kt"
    "Result.kt"
    "SanitizerError.kt"
    "Settings.kt"
    "ShareSanitizerSettings.kt"
    "ShareSanitizerHook.kt"
)

if [[ "$DRY_RUN" == false ]]; then
    mkdir -p "$EXT_DST"

    for file in "${EXTENSION_FILES[@]}"; do
        if [[ -f "$PATCH_SRC/$file" ]]; then
            cp "$PATCH_SRC/$file" "$EXT_DST/$file"
            log_success "Copied: $file"
        else
            log_warning "Skipped (not found): $file"
        fi
    done

    # Update package declaration (patches → extension)
    log_info "Updating package declarations..."
    find "$EXT_DST" -name "*.kt" -type f -exec sed -i.bak \
        's/package app\.revanced\.patches\.tiktok/package app.revanced.extension.tiktok/g' {} \;
    find "$EXT_DST" -name "*.bak" -delete
    log_success "Package declarations updated"

    # Copy extension tests
    EXT_TEST_DST="$INTEGRATIONS_REPO/app/src/test/java/app/revanced/extension/tiktok/sharesanitizer"
    mkdir -p "$EXT_TEST_DST"
    if [[ -f "$TEST_SRC/ShareSanitizerHookTest.kt" ]]; then
        cp "$TEST_SRC/ShareSanitizerHookTest.kt" "$EXT_TEST_DST/"
        sed -i.bak 's/package app\.revanced\.patches\.tiktok/package app.revanced.extension.tiktok/g' \
            "$EXT_TEST_DST/ShareSanitizerHookTest.kt"
        rm -f "$EXT_TEST_DST/ShareSanitizerHookTest.kt.bak"
        log_success "Copied hook test"
    fi
else
    log_dry_run "Would copy ${#EXTENSION_FILES[@]} extension files"
    log_dry_run "Would update package declarations"
    log_dry_run "Would copy hook tests"
fi

log_success "Extension migration complete"

# ============================================================
# Summary
# ============================================================

echo ""
echo -e "${GREEN}=================================================="
echo "Migration Complete!"
echo -e "==================================================${NC}"
echo ""

if [[ "$DRY_RUN" == false ]]; then
    echo "📦 Patch files copied to:"
    echo "   $PATCHES_REPO (branch: $BRANCH_NAME)"
    echo ""
    echo "🔌 Extension files copied to:"
    echo "   $INTEGRATIONS_REPO (branch: $BRANCH_NAME)"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo ""
    echo "1. Build integrations:"
    echo "   cd $INTEGRATIONS_REPO"
    echo "   ./gradlew build"
    echo ""
    echo "2. Build patches:"
    echo "   cd $PATCHES_REPO"
    echo "   ./gradlew build"
    echo ""
    echo "3. Test with APK:"
    echo "   cd $INCUBATOR_ROOT"
    echo "   ./scripts/test-patch.sh --help"
    echo ""
    echo "4. Commit changes:"
    echo "   cd $PATCHES_REPO && git add . && git commit"
    echo "   cd $INTEGRATIONS_REPO && git add . && git commit"
    echo ""
else
    log_warning "DRY-RUN COMPLETE - No changes were made"
    echo ""
    echo "Run without --dry-run to apply changes:"
    echo "  $0"
    echo ""
fi
