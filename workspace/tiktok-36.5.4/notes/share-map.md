# TikTok 36.5.4 Share Flow Mapping

**Status:** 🔍 In Progress
**Package:** com.zhiliaoapp.musically
**Version:** 36.5.4
**Decompilation Method:** dex2jar + CFR

---

## Hook Point 1: Share Intent Construction

**Status:** ⏳ Searching

**Target Behavior:** Find where TikTok constructs the Intent for the Android share sheet.

### Search Results

```bash
# Run this command to start searching:
cd re/tiktok-36.5.4/cfr
rg -n "ACTION_SEND" --type java | head -20
```

### Findings

**Class Name:** TBD
**Package:** TBD
**Method Signature:** TBD
**CFR File Path:** TBD

**Code Snippet:**
```java
// Paste relevant code here after discovery
```

**Smali Path:** TBD (check `re/tiktok-36.5.4/apktool/` if needed)

**Hook Strategy:** TBD

---

## Hook Point 2: Clipboard Write

**Status:** ⏳ Searching

**Target Behavior:** Find where "Copy Link" button writes to clipboard.

### Search Results

```bash
cd re/tiktok-36.5.4/cfr
rg -n "ClipboardManager|setPrimaryClip" --type java | head -20
```

### Findings

**Class Name:** TBD
**Package:** TBD
**Method Signature:** TBD
**CFR File Path:** TBD

**Code Snippet:**
```java
// Paste relevant code here
```

**Hook Strategy:** TBD

---

## Hook Point 3: URL Generation

**Status:** ⏳ Searching

**Target Behavior:** Find where TikTok generates vm.tiktok.com or vt.tiktok.com shortlinks.

### Search Results

```bash
cd re/tiktok-36.5.4/cfr
rg -n "vm\.tiktok\.com|vt\.tiktok\.com" --type java | head -30
```

### Findings

**URL Builder Class:** TBD
**Method:** TBD
**CFR File Path:** TBD

**Code Snippet:**
```java
// Paste URL building logic here
```

**Parameters:**
- Video ID: TBD
- Username: TBD
- Additional params: TBD

---

## Class Hierarchy

Document call chain from UI button → Share logic → URL generation:

```
TBD: ShareButton.onClick()
  → TBD: ShareManager.share()
    → TBD: UrlBuilder.buildShareUrl()
      → TBD: Intent creation
```

---

## Obfuscation Notes

**Obfuscation Level:** TBD (Low/Medium/High)

**Common Patterns Observed:**
- Package naming: TBD
- Method naming: TBD
- String obfuscation: TBD

**Deobfuscation Clues:**
- String literals (URLs) are usually not obfuscated
- Android framework calls (Intent, ClipboardManager) are identifiable
- Method signatures can be cross-referenced with smali

---

## Testing Notes

**Manual Testing Plan:**

1. Open TikTok app
2. Navigate to any video
3. Tap share button
4. Observe:
   - Share sheet appears
   - What URL is shown in preview?
   - Copy link → check clipboard content
5. Logcat filter: `adb logcat | grep -i "share\|clipboard\|intent"`

**Expected Behavior:**
- Share intent with `vm.tiktok.com/XXXX` or `vt.tiktok.com/XXXX`
- Clipboard contains shortlink + tracking params

**Desired Behavior After Patch:**
- Share intent with `https://www.tiktok.com/@user/video/id`
- Clipboard contains canonical URL (no tracking)

---

## Related Files

**Java classes to investigate:**
- [ ] `*Share*.java` files
- [ ] `*Url*.java` files
- [ ] `*Clipboard*.java` files
- [ ] `*Intent*.java` files

**Resource files to check:**
```bash
# If apktool was run:
grep -r "share" re/tiktok-36.5.4/apktool/res/values*/strings.xml
```

---

## Next Actions

1. **Run initial searches** (see commands above)
2. **Document findings** in each section as discovered
3. **Cross-reference CFR output with smali** if code is unclear
4. **Map complete call chain** from UI to Intent creation
5. **Identify injection points** for ReVanced patch
6. **Create fingerprints** based on bytecode patterns
7. **Update** `docs/REVERSE_ENGINEERING.md` with finalized findings

---

## Quick Reference Commands

**Search share logic:**
```bash
cd re/tiktok-36.5.4/cfr
fd -a 'Share.*\.java' | head -20
rg -n "ACTION_SEND" --type java
```

**Search URL builders:**
```bash
rg -n "vm\.tiktok\.com" --type java
fd -a '.*Url.*\.java' | xargs rg -l "tiktok\.com"
```

**Search clipboard:**
```bash
rg -n "ClipboardManager|setPrimaryClip" --type java
```

**Find calling code:**
```bash
# Replace ClassName with actual class
rg -n "ClassName\." --type java
```

---

## Metadata

**Created:** 2025-10-15
**Last Updated:** 2025-10-15
**Decompilation Tools:** dex2jar 2.x + CFR
**Source:** `apk/orig/com.zhiliaoapp.musically_36.5.4.apk` (SHA256: b560a53f...)
