# Next Steps: TikTok APK Reverse Engineering

## Quick Start

You have:
- ✅ TikTok APK v36.5.4 in `apk/orig/`
- ✅ Workspace directories created
- ✅ SHA256 checksum recorded: `b560a53fcea5246f03416556fead581cb49e37ff938c60ed10c8ec53defadb3c`

## Step 1: Decompile with JADX

**Install JADX** (if not already installed):
```bash
# macOS
brew install jadx

# Or download from: https://github.com/skylot/jadx/releases
```

**Open in JADX GUI:**
```bash
jadx-gui apk/orig/com.zhiliaoapp.musically_36.5.4.apk
```

**Enable deobfuscation:**
1. **View → Deobfuscation** (enable)
2. **View → Show inconsistent code** (enable)

**Save project for later reuse:**
1. **File → Save project**
2. Choose location: `apk/jadx/tiktok.jadx`

**Export sources for grepping:**
1. **File → Save code**
2. Export to: `apk/src/`

**To reopen later:**
```bash
jadx-gui apk/jadx/tiktok.jadx
```

## Step 2: Extract Smali with apktool

**Install apktool** (if not already installed):
```bash
# macOS
brew install apktool

# Or download from: https://apktool.org/
```

**Decompile:**
```bash
apktool d -f -o apk/apktool/ apk/orig/com.zhiliaoapp.musically_36.5.4.apk
```

This creates:
- `apk/apktool/AndroidManifest.xml` (decoded, human-readable)
- `apk/apktool/smali*/` (exact bytecode)
- `apk/apktool/res/` (resources)

## Step 3: Search for Hook Points

### Target 1: Share Intent Construction

**In JADX GUI:**
1. **Navigation → Text Search** (Ctrl+Shift+F)
2. Search for: `"Intent.ACTION_SEND"`
3. Also search: `"createChooser"`, `"vm.tiktok.com"`
4. Bookmark promising methods: Right-click → Add bookmark

**In terminal (apk/src/):**
```bash
grep -r "Intent.ACTION_SEND" apk/src/
grep -r "createChooser" apk/src/
grep -r "vm.tiktok.com" apk/src/
grep -r "vt.tiktok.com" apk/src/
```

### Target 2: Clipboard Operations

```bash
grep -r "ClipboardManager" apk/src/
grep -r "setPrimaryClip" apk/src/
```

### Target 3: URL Generation Logic

```bash
grep -r "www.tiktok.com/@" apk/src/
grep -r "/video/" apk/src/ | grep -i url
```

## Step 4: Document Findings

Update `docs/REVERSE_ENGINEERING.md` with:

```markdown
## Hook Point 1: Share Intent Construction

**Status:** ✅ Identified

**Class Name:** `com.ss.android.ugc.aweme.share.ShareServiceImpl` (example)
**Method Signature:** `void prepareShareIntent(Context, String)`
**File Location:** `apk/src/sources/com/ss/android/ugc/aweme/share/ShareServiceImpl.java`

**Smali Location:** `apk/apktool/smali_classes3/com/ss/android/ugc/aweme/share/ShareServiceImpl.smali`

**Injection Point:** Instruction 15 (before `invoke-virtual`)

**Bytecode Pattern:**
\`\`\`smali
const-string v1, "android.intent.extra.TEXT"
invoke-virtual {v0, v1, p2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;
\`\`\`
```

## Step 5: Create Fingerprints

In your project, create `src/main/kotlin/.../ShareSanitizerPatch.kt`:

```kotlin
val shareIntentFingerprint = fingerprint {
    returns("V")
    parameters("Landroid/content/Context;", "Ljava/lang/String;")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        // ... from apktool smali
    )
    strings("https://vm.tiktok.com", "android.intent.extra.TEXT")
}
```

## Step 6: Test Decompilation Quality

**Check for obfuscation:**
```bash
# Look for meaningful names
grep -r "class ShareService" apk/src/

# vs obfuscated names
grep -r "class a.b.c.d" apk/src/ | head -5
```

**Validate AndroidManifest:**
```bash
cat apk/apktool/AndroidManifest.xml | head -50
```

**Check for string resources:**
```bash
cat apk/apktool/res/values/strings.xml | grep -i share | head -10
```

## Troubleshooting

**JADX crashes on large APK:**
- Increase heap: `jadx -Xmx4g -d apk/src/ apk/orig/*.apk`

**Apktool fails:**
- Update framework: `apktool empty-framework-dir --force`
- Retry decompilation

**Can't find hook points:**
- TikTok may use native code (JNI) for share logic
- Check `apk/notes/lib/` for `.so` files
- Use Ghidra/IDA for native analysis if needed

## Resources

- JADX GitHub: https://github.com/skylot/jadx
- Apktool docs: https://apktool.org/docs/
- ReVanced fingerprint examples: https://github.com/revanced/revanced-patches/tree/main/patches
- Smali opcodes reference: https://source.android.com/docs/core/runtime/dalvik-bytecode

## Timeline Estimate

- Decompilation: **30 minutes**
- Hook point discovery: **2-4 hours** (depends on obfuscation)
- Fingerprint creation: **1-2 hours**
- Patch implementation: **2-3 hours**
- Testing: **1-2 hours**

**Total:** ~8-12 hours for first-time RE
