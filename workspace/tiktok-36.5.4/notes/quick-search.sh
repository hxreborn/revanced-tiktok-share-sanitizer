#!/usr/bin/env bash
# Quick search helper for TikTok share hook discovery
# Usage: ./quick-search.sh [search-type]

set -euo pipefail

CFR_DIR="$(cd "$(dirname "$0")/../cfr" && pwd)"
cd "$CFR_DIR"

echo "🔍 TikTok Share Hook Discovery"
echo "Searching in: $CFR_DIR"
echo ""

case "${1:-help}" in
  share-intent|1)
    echo "=== Share Intent Construction ==="
    echo "Searching for ACTION_SEND..."
    rg -n "ACTION_SEND" --type java | head -20
    echo ""
    echo "Searching for createChooser..."
    rg -n "createChooser" --type java | head -10
    ;;

  clipboard|2)
    echo "=== Clipboard Operations ==="
    echo "Searching for ClipboardManager..."
    rg -n "ClipboardManager" --type java | head -20
    echo ""
    echo "Searching for setPrimaryClip..."
    rg -n "setPrimaryClip" --type java | head -20
    ;;

  urls|3)
    echo "=== TikTok URL Generation ==="
    echo "Searching for vm.tiktok.com..."
    rg -n "vm\.tiktok\.com" --type java | head -15
    echo ""
    echo "Searching for vt.tiktok.com..."
    rg -n "vt\.tiktok\.com" --type java | head -15
    echo ""
    echo "Searching for /video/ paths..."
    rg -n '"/video/"' --type java | head -15
    ;;

  files|4)
    echo "=== Share-Related Files ==="
    echo "Java files with 'Share' in name:"
    fd -a 'Share.*\.java' | head -20
    echo ""
    echo "Java files with 'Url' in name (filtered for share):"
    fd -a 'Url.*\.java' | xargs rg -l "share|Share" 2>/dev/null | head -15
    ;;

  tracking|5)
    echo "=== Tracking Parameters ==="
    echo "Searching for query parameter builders..."
    rg -n "addQueryParameter|setQuery|withQuery" --type java | head -20
    echo ""
    echo "Searching for utm parameters..."
    rg -n "utm_source|utm_medium|utm_campaign" --type java | head -10
    ;;

  all)
    echo "Running all searches (this may take a minute)..."
    echo ""
    "$0" share-intent
    echo ""
    "$0" clipboard
    echo ""
    "$0" urls
    echo ""
    "$0" files
    ;;

  help|*)
    cat << 'EOF'
Usage: ./quick-search.sh [search-type]

Available searches:
  1 | share-intent   - Find ACTION_SEND and createChooser
  2 | clipboard      - Find ClipboardManager and setPrimaryClip
  3 | urls           - Find vm.tiktok.com, vt.tiktok.com, /video/
  4 | files          - List Share/Url related files
  5 | tracking       - Find query parameter logic
  all                - Run all searches (verbose)
  help               - Show this message

Examples:
  ./quick-search.sh 1           # Search for share intents
  ./quick-search.sh clipboard   # Search for clipboard code
  ./quick-search.sh all         # Run all searches

Tips:
  - Pipe to less for long output: ./quick-search.sh urls | less
  - Increase results: Edit this script and change | head -N
  - Custom search: cd ../cfr && rg "your pattern" --type java
EOF
    ;;
esac
