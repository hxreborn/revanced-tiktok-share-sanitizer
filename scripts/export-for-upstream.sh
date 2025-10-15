#!/bin/bash

# Export script for ReVanced TikTok Share Sanitizer upstream integration
# Copies only the relevant source files to match revanced-patches structure

set -euo pipefail

# Configuration
SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_PACKAGE_DIR="$SOURCE_DIR/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer"

# Destination can be configured via env var or argument
DEFAULT_DEST="$SOURCE_DIR/../revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer"
DEST_DIR="${REVANCED_PATCHES_DIR:-${1:-$DEFAULT_DEST}}"

# Files to export (only production source files, no build artifacts)
EXPORT_FILES=(
    "ShareSanitizerPatch.kt"
    "UrlNormalizer.kt"
    "ShortlinkExpander.kt"
    "OkHttpClientAdapter.kt"
    "Result.kt"
    "SanitizerError.kt"
    "HttpClient.kt"
)

# Test files to export
TEST_FILES=(
    "UrlNormalizerTest.kt"
    "ShortlinkExpanderTest.kt"
    "OkHttpClientAdapterTest.kt"
)

# Subdirectories to export
EXPORT_DIRS=(
    "fingerprints"
)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate source directory
if [[ ! -d "$SOURCE_PACKAGE_DIR" ]]; then
    log_error "Source package directory not found: $SOURCE_PACKAGE_DIR"
    exit 1
fi

# Create destination directory
log_info "Creating destination directory: $DEST_DIR"
mkdir -p "$DEST_DIR"

# Export main source files
log_info "Exporting source files..."
for file in "${EXPORT_FILES[@]}"; do
    src_path="$SOURCE_PACKAGE_DIR/$file"
    if [[ -f "$src_path" ]]; then
        cp "$src_path" "$DEST_DIR/"
        log_info "  ✓ $file"
    else
        log_warn "  ✗ $file (not found, skipping)"
    fi
done

# Export subdirectories
log_info "Exporting subdirectories..."
for dir in "${EXPORT_DIRS[@]}"; do
    src_dir="$SOURCE_PACKAGE_DIR/$dir"
    if [[ -d "$src_dir" ]]; then
        cp -r "$src_dir" "$DEST_DIR/"
        log_info "  ✓ $dir/"
    else
        log_warn "  ✗ $dir/ (not found, skipping)"
    fi
done

# Export test files if destination test directory exists
test_dest_dir="$(dirname "$DEST_DIR")/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer"
if [[ -d "$(dirname "$test_dest_dir")" ]] || [[ "${EXPORT_TESTS:-yes}" == "yes" ]]; then
    log_info "Exporting test files..."
    mkdir -p "$test_dest_dir"

    for file in "${TEST_FILES[@]}"; do
        src_path="$SOURCE_DIR/src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/$file"
        if [[ -f "$src_path" ]]; then
            cp "$src_path" "$test_dest_dir/"
            log_info "  ✓ test/$file"
        else
            log_warn "  ✗ test/$file (not found, skipping)"
        fi
    done
fi

# Create export info file
script_name="$(basename "$0")"
cat > "$DEST_DIR/EXPORT_INFO.md" << EOF
# Export Information

**Exported from:** \`$SOURCE_DIR\`
**Exported on:** $(date -u +"%Y-%m-%dT%H:%M:%SZ")
**Export script:** $script_name

## Files Included

### Source Files
$(printf "- %s\n" "${EXPORT_FILES[@]}")

### Subdirectories
$(printf "- %s/\n" "${EXPORT_DIRS[@]}")

### Test Files
$(printf "- test/%s\n" "${TEST_FILES[@]}")

## Integration Notes

1. This export contains only the source files needed for upstream integration
2. Build artifacts and analysis files are intentionally excluded
3. The package structure matches ReVanced conventions: \`app.revanced.patches.tiktok.misc.sharesanitizer\`
4. Test files are included in the appropriate test directory structure

## Next Steps

1. Review the exported files for any hardcoded paths or references
2. Update patch dependencies to match monorepo structure
3. Add patch to the main patches index if needed
4. Run tests to ensure everything works in the monorepo context
EOF

log_info "Export completed successfully!"
log_info "Exported to: $DEST_DIR"
log_info "Export info written to: $DEST_DIR/EXPORT_INFO.md"

# Show summary
log_info "Export summary:"
echo "  Source files: ${#EXPORT_FILES[@]}"
echo "  Subdirectories: ${#EXPORT_DIRS[@]}"
echo "  Test files: ${#TEST_FILES[@]}"
echo "  Destination: $DEST_DIR"