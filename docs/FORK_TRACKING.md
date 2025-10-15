# Fork Tracking

This document tracks the forked ReVanced repositories and feature branches for the TikTok Share Sanitizer patch development.

## Repository Forks

### revanced-patches
- **Upstream**: https://github.com/ReVanced/revanced-patches
- **Fork**: https://github.com/hxreborn/revanced-patches
- **Local Path**: `/Users/rafa/Documents/GitHub/revanced-patches`
- **Feature Branch**: `feat/tiktok-share-sanitizer`
- **Base Branch**: `main`

### revanced-integrations
- **Upstream**: https://github.com/ReVanced/revanced-integrations
- **Fork**: https://github.com/hxreborn/revanced-integrations
- **Local Path**: `/Users/rafa/Documents/GitHub/revanced-integrations`
- **Feature Branch**: `feat/tiktok-share-sanitizer-extension`
- **Base Branch**: `main`

## Directory Structure

### revanced-patches
```
patches/src/main/kotlin/app/revanced/patches/tiktok/
├── feedfilter/
├── interaction/
├── misc/
│   └── sharesanitizer/         # Our patch location
└── shared/
```

### revanced-integrations
```
app/src/main/java/app/revanced/integrations/tiktok/
├── cleardisplay/
├── download/
├── feedfilter/
├── settings/
├── speed/
├── spoof/
└── sharesanitizer/             # Our extension location (to be created)
```

## Exported Files

### Source Files (7)
- `ShareSanitizerPatch.kt` - Main patch declaration
- `UrlNormalizer.kt` - URL normalization logic
- `ShortlinkExpander.kt` - Shortlink expansion
- `OkHttpClientAdapter.kt` - HTTP client implementation
- `HttpClient.kt` - HTTP abstraction interface
- `Result.kt` - Result type for error handling
- `SanitizerError.kt` - Error types

### Test Files (3)
- `UrlNormalizerTest.kt` - URL normalization tests (10 tests)
- `ShortlinkExpanderTest.kt` - Shortlink expansion tests (4 tests)
- `OkHttpClientAdapterTest.kt` - HTTP client tests (7 tests)

### Subdirectories (1)
- `fingerprints/` - Bytecode fingerprint definitions

## Workflow

### Syncing with Upstream
```bash
# In revanced-patches
cd /Users/rafa/Documents/GitHub/revanced-patches
git checkout main
git pull upstream main
git push origin main

# In revanced-integrations
cd /Users/rafa/Documents/GitHub/revanced-integrations
git checkout main
git pull upstream main
git push origin main
```

### Updating Feature Branches
```bash
# In revanced-patches
cd /Users/rafa/Documents/GitHub/revanced-patches
git checkout feat/tiktok-share-sanitizer
git merge main

# In revanced-integrations
cd /Users/rafa/Documents/GitHub/revanced-integrations
git checkout feat/tiktok-share-sanitizer-extension
git merge main
```

### Re-exporting After Changes
```bash
cd /Users/rafa/Documents/GitHub/revanced-tiktok-share-sanitizer
./scripts/export-for-upstream.sh
rm /Users/rafa/Documents/GitHub/revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/EXPORT_INFO.md
cd /Users/rafa/Documents/GitHub/revanced-patches
git status  # Review changes
```

## PR Preparation Checklist

### Before Creating PR to revanced-patches
- [ ] Sync feature branch with upstream main
- [ ] Run tests in monorepo context: `./gradlew :patches:test --tests *ShareSanitizer*`
- [ ] Verify patch builds in monorepo: `./gradlew :patches:build`
- [ ] Update CHANGELOG.md in upstream repo
- [ ] Remove any incubator-specific files (EXPORT_INFO.md, etc.)
- [ ] Add patch description strings for ReVanced Manager UI
- [ ] Document tested TikTok versions in compatibility annotations

### Before Creating PR to revanced-integrations
- [ ] Create extension hook classes in `app/src/main/java/app/revanced/integrations/tiktok/sharesanitizer/`
- [ ] Sync feature branch with upstream main
- [ ] Verify extension builds in monorepo: `./gradlew build`
- [ ] Add any required settings integration
- [ ] Test with patched APK

## Notes

- **Directory Naming**: Forks are cloned as `revanced-patches` and `revanced-integrations` to match export script defaults
- **Remote Configuration**: Both forks have `origin` pointing to personal fork and `upstream` pointing to ReVanced org
- **Test Location**: Tests were exported to `patches/src/main/kotlin/app/revanced/patches/tiktok/misc/test/` (may need to be moved to proper test directory in monorepo)
- **Integrations Structure**: The actual path is `app/src/main/java/` not `extensions/` as documented in older BEST_PRACTICES.md

## Last Updated

2025-10-15
