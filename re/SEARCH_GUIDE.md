# TikTok Share Hook Point Discovery Guide

## Current Workspace

**Location:** `re/tiktok-36.5.4/`

**Artifacts:**
- ✅ `dex/` - 49 DEX files extracted (194MB)
- ✅ `jar/` - dex2jar converted JARs (261MB)
- ✅ `cfr/` - CFR decompiled Java sources (219MB)

**Tools available:**
- `ripgrep (rg)` - Fast source code search
- `fd` - Fast file finder
- `cfr` - Java decompiler

---

## Quick Search Commands

### 1. Find Share-Related Code

**Search for share intent construction:**
```bash
cd re/tiktok-36.5.4/cfr

# Find ACTION_SEND usage (Android share framework)
rg -n "ACTION_SEND" --type java | head -20

# Find share-related classes
fd -a '.*(Share|Sharing).*\.java'

# Find Intent.createChooser calls
rg -n "createChooser" --type java

# Find share menu/button handlers
rg -n "share.*button|onShare|handleShare" -i --type java
```

**Search for clipboard operations:**
```bash
# ClipboardManager usage
rg -n "ClipboardManager" --type java

# setPrimaryClip calls
rg -n "setPrimaryClip" --type java

# Clipboard + TikTok URL patterns
rg -n "ClipboardManager.*tiktok|setPrimaryClip.*vm\.tiktok" --type java
```

### 2. Find URL Generation Logic

**TikTok URL patterns:**
```bash
# vm.tiktok.com shortlinks
rg -n "vm\.tiktok\.com" --type java

# vt.tiktok.com shortlinks
rg -n "vt\.tiktok\.com" --type java

# Canonical www.tiktok.com URLs
rg -n "www\.tiktok\.com/@" --type java

# /video/ path construction
rg -n '"/video/"' --type java | head -30

# URL builder patterns
rg -n "buildUrl|createUrl|generateUrl|shareUrl" -i --type java
```

**Query parameter handling:**
```bash
# Find tracking parameter append logic
rg -n "addQueryParameter|setQuery|withQuery" --type java

# utm_ tracking parameters
rg -n 'utm_source|utm_medium|utm_campaign' --type java

# TikTok-specific tracking params
rg -n '_r=|is_copy_url|is_from_webapp' --type java
```

### 3. Cross-Reference with Smali (If Needed)

If CFR output is too obfuscated, check smali for exact bytecode:

```bash
# Search in apktool output (if you ran apktool d)
rg -n "vm\.tiktok\.com|ACTION_SEND" re/tiktok-36.5.4/apktool/smali*

# Find const-string opcodes with TikTok URLs
rg -A2 'const-string.*vm\.tiktok' re/tiktok-36.5.4/apktool/smali*
```

---

## Systematic Hook Point Discovery

### Step 1: Identify Share Entry Points

```bash
cd re/tiktok-36.5.4/cfr

# Find all share-related files
fd -a 'Share.*\.java' > /tmp/share_files.txt
cat /tmp/share_files.txt

# Analyze top candidates
rg -n "ACTION_SEND|createChooser" $(cat /tmp/share_files.txt | head -10)
```

**Look for patterns like:**
```java
Intent intent = new Intent("android.intent.action.SEND");
intent.putExtra("android.intent.extra.TEXT", videoUrl);
intent.setType("text/plain");
startActivity(Intent.createChooser(intent, ...));
```

### Step 2: Find URL Construction

```bash
# Find where share URLs are built
rg -B5 -A10 'vm\.tiktok\.com' --type java | less

# Look for methods that take video ID and return URL
rg -n "String.*Url.*video|String.*buildShare|String.*getShare" -i --type java
```

**Expected patterns:**
```java
String shareUrl = "https://vm.tiktok.com/" + shortCode;
// or
String shareUrl = buildVideoUrl(username, videoId);
```

### Step 3: Trace Call Hierarchy

Once you find a promising method:

1. **Note the class and method name**
2. **Search for callers:**
   ```bash
   rg -n "ClassName\.methodName|new ClassName" --type java
   ```

3. **Find the Smali equivalent:**
   ```bash
   # Convert Java class name to smali path
   # com.example.ShareBuilder → com/example/ShareBuilder.smali
   find re/tiktok-36.5.4/apktool -name "ShareBuilder.smali"
   ```

### Step 4: Validate with String Literals

TikTok URLs must be hardcoded somewhere:

```bash
# Find all files with vm.tiktok.com
rg -l "vm\.tiktok\.com" --type java > /tmp/url_files.txt

# Check each file
for f in $(cat /tmp/url_files.txt | head -20); do
  echo "=== $f ==="
  rg -C3 "vm\.tiktok\.com" "$f"
done
```

---

## Promising Search Paths

Based on typical TikTok structure:

```bash
# Check aweme (TikTok's internal codename) package
fd -a '.*aweme.*share.*\.java' re/tiktok-36.5.4/cfr

# Check share-specific modules
fd -a '\.java$' re/tiktok-36.5.4/cfr/com | grep -i share

# Check URL utility classes
fd -a '.*[Uu]rl.*\.java' re/tiktok-36.5.4/cfr/com | head -20

# Check clipboard utilities
fd -a '.*[Cc]lipboard.*\.java' re/tiktok-36.5.4/cfr
```

---

## Narrowing Down Obfuscated Code

If you find obfuscated classes like `a.b.c.d`:

```bash
# Find classes that reference both share AND TikTok URLs
rg -l "ACTION_SEND" --type java | xargs rg -l "vm\.tiktok\.com"

# Check method signatures in those classes
for f in $(rg -l "ACTION_SEND" --type java | head -10); do
  echo "=== $f ==="
  grep -n "public.*share\|void.*share\|String.*Url" "$f" | head -5
done
```

---

## Output Format for Findings

When you find a hook point, document it in `re/tiktok-36.5.4/notes/share-map.md`:

```markdown
## Hook Point: Share Intent Construction

**Java Class:** `com.ss.android.ugc.aweme.share.ShareServiceImpl`
**CFR File:** `re/tiktok-36.5.4/cfr/com/ss/android/ugc/aweme/share/ShareServiceImpl.java`
**Method:** `void prepareShareIntent(Context context, String videoUrl)`

**Code Snippet:**
\`\`\`java
public void prepareShareIntent(Context context, String videoUrl) {
    Intent intent = new Intent("android.intent.action.SEND");
    intent.putExtra("android.intent.extra.TEXT", this.buildUrl(videoUrl));
    intent.setType("text/plain");
    context.startActivity(Intent.createChooser(intent, "Share"));
}
\`\`\`

**Smali Path:** `re/tiktok-36.5.4/apktool/smali_classes5/com/ss/android/ugc/aweme/share/ShareServiceImpl.smali`

**Hook Strategy:** Intercept `buildUrl()` or patch `prepareShareIntent` to sanitize URL before passing to Intent
```

---

## Common TikTok Obfuscation Patterns

**Package prefixes to check:**
- `com.ss.android.ugc.aweme.*` - Main TikTok app code
- `com.bytedance.*` - ByteDance SDK
- `X.*`, `Y.*`, `J.*` - Obfuscated top-level packages
- `a.b.c.*` - Heavily obfuscated classes

**Method naming patterns:**
- Share: `a()`, `b()`, `LIZ()`, `LIZIZ()` (obfuscated)
- URL builders: Often contain "Url", "Link", or obfuscated like `m1234()`

---

## Time-Saving Tips

1. **Start with string literals** - URLs are rarely obfuscated
2. **Search cross-package** - Share logic may span multiple modules
3. **Check recent files** - Often edited together:
   ```bash
   ls -lt re/tiktok-36.5.4/cfr/**/*.java | head -50
   ```
4. **Use context flags** - `-C3` shows 3 lines before/after match
5. **Pipe to less** - For long output: `rg ... | less`

---

## Next Steps After Finding Hooks

1. Copy class/method details to `re/tiktok-36.5.4/notes/share-map.md`
2. Update `docs/REVERSE_ENGINEERING.md` with findings
3. Create fingerprints in `ShareSanitizerPatch.kt` based on bytecode patterns
4. Implement hook injection logic
5. Test with ReVanced CLI

**See:** `apk/NEXT_STEPS.md` for patch implementation workflow
