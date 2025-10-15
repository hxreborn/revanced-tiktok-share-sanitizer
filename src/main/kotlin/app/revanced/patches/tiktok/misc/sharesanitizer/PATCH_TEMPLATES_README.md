# Patch Templates

**Note:** Files in this directory are **templates for upstream integration**.

They reference ReVanced Patcher APIs which are not dependencies of this incubator project, so they **will not compile here** - this is intentional!

## Files

- `ShareSanitizerPatch.kt` - Main patch with bytecode injection
- `fingerprints/ClipboardCopyFingerprint.kt` - Method fingerprint for pattern matching

## Usage

These files are ready to copy into the upstream `revanced-patches` repository where they will compile successfully.

See `docs/INTEGRATION_GUIDE.md` for complete integration instructions.

## Why Separate?

This incubator project is for **fast iteration on core logic** (UrlNormalizer, ShortlinkExpander) without the overhead of the full ReVanced build system.

The patch templates are maintained here for version control and documentation purposes.
