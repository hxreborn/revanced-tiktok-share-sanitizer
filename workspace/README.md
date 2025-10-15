# Reverse Engineering Workspace

This directory contains decompiled TikTok APK artifacts using the **dex2jar + CFR** workflow.

## Workspace: `tiktok-36.5.4/`

### Directory Structure

```
re/tiktok-36.5.4/
├── dex/              # Extracted DEX files (194MB, gitignored)
│   ├── classes.dex
│   ├── classes2.dex
│   └── ... (49 total)
├── jar/              # dex2jar output (261MB, gitignored)
│   ├── classes.jar
│   ├── classes2.jar
│   └── ... (49 total)
├── cfr/              # CFR decompiled Java (219MB, gitignored)
│   ├── com/
│   ├── bytedance/
│   ├── androidx/
│   └── ... (all packages)
└── notes/            # Analysis notes (versioned)
    └── share-map.md  # Hook point findings
```

**Total Size:** ~674MB (all gitignored except `notes/`)

### Decompilation Toolchain

**Tools Used:**
1. **unzip** - Extract DEX from APK
2. **dex2jar** - Convert DEX → JAR
3. **CFR** - Decompile JAR → Java
4. **ripgrep (rg)** - Fast source search
5. **fd** - Fast file finder

**Environment:**
- Java: Homebrew OpenJDK (JAVA_HOME configured)
- Heap: 16GB (`JAVA_TOOL_OPTIONS="-Xmx16g -XX:+UseG1GC"`)

### Why dex2jar + CFR Instead of JADX?

**JADX Issues:**
- Slow on large APKs (397MB TikTok)
- High memory usage with `--deobf`
- Can hang on complex obfuscation

**CFR Advantages:**
- Faster decompilation (per-JAR parallelization)
- Lower memory footprint
- Good enough for string/pattern search
- Can fallback to JADX for specific classes later

### Actual Workflow Executed

```bash
# 1. Extract DEX files
unzip -j apk/orig/*.apk 'classes*.dex' -d re/tiktok-36.5.4/dex/

# 2. Convert each DEX to JAR
for dex in re/tiktok-36.5.4/dex/*.dex; do
  d2j-dex2jar -f "$dex" -o "re/tiktok-36.5.4/jar/$(basename $dex .dex).jar"
done

# 3. Decompile all JARs with CFR
for jar in re/tiktok-36.5.4/jar/*.jar; do
  java -jar /opt/homebrew/Cellar/cfr-decompiler/*/libexec/cfr.jar \
    "$jar" --outputdir re/tiktok-36.5.4/cfr/
done

# 4. Verify output
tree -L 2 re/tiktok-36.5.4/cfr/
du -sh re/tiktok-36.5.4/*
```

**Decompilation Time:** ~15 minutes (vs. JADX's >1 hour)

---

## Search Guide

**See:** `re/SEARCH_GUIDE.md` for comprehensive search commands.

### Quick Start

```bash
cd re/tiktok-36.5.4/cfr

# Find share intent code
rg -n "ACTION_SEND" --type java | head -20

# Find clipboard writes
rg -n "ClipboardManager|setPrimaryClip" --type java

# Find TikTok URL generation
rg -n "vm\.tiktok\.com|vt\.tiktok\.com" --type java

# Find share-related files
fd -a 'Share.*\.java' | head -20
```

---

## Documentation Workflow

### 1. Search for Hook Points

Use `re/SEARCH_GUIDE.md` commands to find:
- Share intent construction
- Clipboard operations
- URL generation logic

### 2. Document Findings

Update `re/tiktok-36.5.4/notes/share-map.md` with:
- Class names
- Method signatures
- Code snippets
- CFR file paths

### 3. Cross-Reference with Smali (Optional)

If CFR output is too obfuscated:

```bash
# Run apktool (if not done yet)
apktool d -f -o re/tiktok-36.5.4/apktool/ apk/orig/*.apk

# Search smali for exact bytecode
rg -n "vm\.tiktok\.com" re/tiktok-36.5.4/apktool/smali*
```

### 4. Finalize in Project Docs

Copy findings to `docs/REVERSE_ENGINEERING.md` and create patch fingerprints.

---

## Next Steps

1. **Run searches** → `re/SEARCH_GUIDE.md`
2. **Document findings** → `re/tiktok-36.5.4/notes/share-map.md`
3. **Create fingerprints** → `src/main/kotlin/.../ShareSanitizerPatch.kt`
4. **Test patch** → `apk/NEXT_STEPS.md`

---

## Git Strategy

**Gitignored (Large Files):**
- `re/*/dex/` - Original DEX files
- `re/*/jar/` - dex2jar output
- `re/*/cfr/` - CFR decompiled sources
- `re/*/*.apk` - Original APKs

**Versioned (Metadata Only):**
- `re/README.md` - This file
- `re/SEARCH_GUIDE.md` - Search commands
- `re/tiktok-36.5.4/notes/` - Analysis findings

**Rationale:** Decompiled sources are reproducible from APK; only findings matter.

---

## Alternative: Focused JADX (If Needed)

If you find a specific class that CFR mangles, re-decompile with JADX:

```bash
jadx --single-class com.example.ShareBuilder \
     --single-class-output re/tiktok-36.5.4/jadx-focus \
     -r apk/orig/*.apk
```

This gives cleaner output for targeted classes without full APK decompilation.

---

## Troubleshooting

**dex2jar fails:**
```bash
# Update dex2jar
brew upgrade dex2jar

# Increase heap
export JAVA_TOOL_OPTIONS="-Xmx16g"
```

**CFR produces unreadable code:**
- Check smali instead: `apktool d ...`
- Try JADX on specific class (see above)
- Use `--comments false` flag with CFR

**Search is too slow:**
```bash
# Use ripgrep (rg) not grep
brew install ripgrep

# Limit search depth
rg -n "pattern" --max-depth 3
```

---

## Resources

- **dex2jar:** https://github.com/pxb1988/dex2jar
- **CFR:** https://www.benf.org/other/cfr/
- **apktool:** https://apktool.org/
- **ripgrep:** https://github.com/BurntSushi/ripgrep
- **ReVanced Patches:** https://github.com/revanced/revanced-patches
