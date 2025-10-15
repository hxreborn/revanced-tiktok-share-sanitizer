# TikTok APK Reverse Engineering Workspace

This directory contains decompiled TikTok APK artifacts for developing the Share Sanitizer patch.

## Directory Structure

```
apk/
├── orig/                 # Original TikTok APK files (gitignored)
├── jadx/                 # Reopen-able JADX project (versioned)
│   └── tiktok.jadx/      # Session state, bookmarks, deobfuscation map
├── src/                  # Exported Java sources from JADX (gitignored, large)
├── apktool/              # Smali + exact resources from apktool (gitignored, large)
└── notes/                # Metadata + small artifacts (versioned)
    ├── checksums.txt     # SHA256 hash of original APK
    ├── aapt.txt          # Package metadata (version, permissions, etc.)
    ├── certs.txt         # APK signing certificate info
    ├── lib/              # Native libraries (gitignored)
    └── dex/              # DEX files (gitignored)
```

## Setup Instructions

### 1. Obtain TikTok APK

**Recommended version:** 36.5.4

**Variants:**
- US: `com.zhiliaoapp.musically`
- Global: `com.ss.android.ugc.trill`

**Sources:**
- APKMirror: https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/
- APKPure: https://apkpure.com/tiktok/com.zhiliaoapp.musically

Place the APK in `apk/orig/` directory:
```bash
cp ~/Downloads/tiktok-36.5.4.apk apk/orig/
```

### 2. Extract Metadata

```bash
# Generate checksums
sha256sum apk/orig/*.apk > apk/notes/checksums.txt

# Extract package info
aapt dump badging apk/orig/*.apk > apk/notes/aapt.txt

# Extract signing certificate
apksigner verify --print-certs apk/orig/*.apk > apk/notes/certs.txt
```

### 3. Decompile with JADX

**Option A: GUI (Recommended)**
```bash
jadx-gui apk/orig/tiktok-*.apk
```

In JADX GUI:
1. Enable: **View → Deobfuscation**
2. Enable: **View → Show inconsistent code**
3. **File → Save project** → `apk/jadx/tiktok.jadx`
4. **File → Save code** → `apk/src/`

**Option B: CLI**
```bash
jadx -j 8 --deobf --show-bad-code -d apk/src/ apk/orig/tiktok-*.apk
```

**To reopen JADX project later:**
```bash
jadx-gui apk/jadx/tiktok.jadx
```

### 4. Extract Smali with apktool

```bash
apktool d -f -o apk/apktool/ apk/orig/tiktok-*.apk
```

This provides:
- `apk/apktool/AndroidManifest.xml` (decoded XML)
- `apk/apktool/smali*/` (exact bytecode, no decompilation artifacts)
- `apk/apktool/res/` (exact resources)

### 5. Extract Optional Artifacts

```bash
# Native libraries (if analyzing NDK code)
unzip -j apk/orig/*.apk 'lib/*' -d apk/notes/lib/

# DEX files (for manual analysis)
unzip -j apk/orig/*.apk 'classes*.dex' -d apk/notes/dex/
```

## Finding Hook Points

### Search Strategy

**Target patterns in `apk/src/`:**

1. **Share Intent Construction**
   ```bash
   grep -r "Intent.ACTION_SEND" apk/src/
   grep -r "createChooser" apk/src/
   grep -r "startActivity" apk/src/ | grep -i share
   ```

2. **Clipboard Operations**
   ```bash
   grep -r "ClipboardManager" apk/src/
   grep -r "setPrimaryClip" apk/src/
   ```

3. **URL Generation**
   ```bash
   grep -r "vm.tiktok.com" apk/src/
   grep -r "vt.tiktok.com" apk/src/
   grep -r "@.*video" apk/src/ | head -20
   ```

### Using JADX GUI

1. Open `apk/jadx/tiktok.jadx`
2. **Navigation → Text Search** (Ctrl+Shift+F)
3. Search for strings: `"vm.tiktok.com"`, `"ACTION_SEND"`
4. Bookmark promising methods: Right-click → Add bookmark
5. Add comments: Right-click on line → Add comment

**Bookmarks and comments are saved in the JADX project file.**

## Documenting Findings

Update `docs/REVERSE_ENGINEERING.md` with:
- Class names
- Method signatures
- Smali bytecode patterns
- Injection points (instruction indexes)

Example:
```markdown
### Hook Point 1: Share Intent Construction

**Class Name:** `com.ss.android.ugc.aweme.share.ShareServiceImpl`
**Method Signature:** `void prepareShareIntent(Context, String)`
**Bytecode Pattern:** (copy from apktool/smali/)
**Injection Point:** Instruction 15 (before `invoke-virtual`)
```

## Testing Patches

After creating fingerprints and implementing `ShareSanitizerPatch.kt`:

```bash
# Build patch bundle
gradle build

# Apply patch with ReVanced CLI
revanced-cli patch \
  --patch-bundle build/libs/revanced-tiktok-share-sanitizer.jar \
  --integrations revanced-integrations.apk \
  --out apk/tiktok-patched.apk \
  apk/orig/tiktok-*.apk

# Sign the patched APK
apksigner sign --ks your.jks --out apk/tiktok-signed.apk apk/tiktok-patched.apk

# Install and test
adb install -r apk/tiktok-signed.apk
adb logcat | grep -i "ShareSanitizer"
```

## Notes

- **Keep original APK safe:** Store SHA256 in `notes/checksums.txt` before any modifications
- **Version compatibility:** Each TikTok version may have different bytecode patterns
- **Test both variants:** US (`musically`) and Global (`trill`) packages may differ
- **Obfuscation:** TikTok uses ProGuard/R8; expect obfuscated names like `a.b.c.d`
- **JADX project persistence:** Always save project before closing to preserve bookmarks/comments

## Upstream Resources

- ReVanced Patches repository: https://github.com/revanced/revanced-patches
- ReVanced Patcher docs: https://github.com/revanced/revanced-patcher
- Existing TikTok patches: `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/`
