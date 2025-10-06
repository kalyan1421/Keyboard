#!/bin/bash

# AI Keyboard - Safe Cleanup Script
# This script performs only confirmed safe deletions
# Manual review items are NOT deleted

set -e  # Exit on error

echo "🧹 AI Keyboard Project Cleanup - Safe Deletions Only"
echo "====================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track stats
deleted_files=0
cleaned_lines=0

echo "📋 Phase 1: Removing Confirmed Unused Dart Files"
echo "---------------------------------------------------"

if [ -f "lib/word_trie.dart" ]; then
    rm -f lib/word_trie.dart
    echo "${GREEN}✓${NC} Removed lib/word_trie.dart (295 lines)"
    deleted_files=$((deleted_files + 1))
    cleaned_lines=$((cleaned_lines + 295))
fi

if [ -f "lib/clipboard_settings_screen.dart" ]; then
    rm -f lib/clipboard_settings_screen.dart
    echo "${GREEN}✓${NC} Removed lib/clipboard_settings_screen.dart (450 lines)"
    deleted_files=$((deleted_files + 1))
    cleaned_lines=$((cleaned_lines + 450))
fi

if [ -f "lib/theme_editor_screen.dart" ]; then
    rm -f lib/theme_editor_screen.dart
    echo "${GREEN}✓${NC} Removed lib/theme_editor_screen.dart (1393 lines)"
    deleted_files=$((deleted_files + 1))
    cleaned_lines=$((cleaned_lines + 1393))
fi

if [ -f "lib/widgets/compose_keyboard.dart" ]; then
    rm -f lib/widgets/compose_keyboard.dart
    echo "${GREEN}✓${NC} Removed lib/widgets/compose_keyboard.dart (72 lines)"
    deleted_files=$((deleted_files + 1))
    cleaned_lines=$((cleaned_lines + 72))
fi

if [ -f "lib/suggestion_bar_widget.dart" ]; then
    rm -f lib/suggestion_bar_widget.dart
    echo "${GREEN}✓${NC} Removed lib/suggestion_bar_widget.dart (unknown lines)"
    deleted_files=$((deleted_files + 1))
fi

echo ""
echo "📋 Phase 2: Cleaning Flutter Cache"
echo "---------------------------------------------------"
flutter clean
echo "${GREEN}✓${NC} Flutter cache cleaned"

echo ""
echo "📋 Phase 3: Updating Dependencies"
echo "---------------------------------------------------"
flutter pub get
echo "${GREEN}✓${NC} Dependencies updated"

echo ""
echo "${YELLOW}⚠️  Phase 4: Manual Actions Required${NC}"
echo "---------------------------------------------------"
echo "Please manually edit the following files:"
echo ""
echo "1. ${YELLOW}lib/main.dart${NC}"
echo "   - Remove lines 24-29 (duplicate imports)"
echo "   - Remove lines 668-686 (commented Container)"
echo "   - Remove lines 1468-1481 (commented method)"
echo ""
echo "2. ${YELLOW}pubspec.yaml${NC}"
echo "   - Remove lines 92-96 (duplicate asset declarations)"
echo ""
echo "These require careful manual editing to avoid breaking the file structure."
echo ""

echo ""
echo "📊 Cleanup Summary"
echo "====================================================="
echo "Files deleted: ${deleted_files}"
echo "Lines removed: ~${cleaned_lines}"
echo "Cache cleaned: ✓"
echo "Dependencies updated: ✓"
echo ""
echo "${GREEN}✅ Safe cleanup completed successfully!${NC}"
echo ""
echo "Next steps:"
echo "1. Review manual edit instructions above"
echo "2. Run: flutter build apk --debug"
echo "3. Test keyboard functionality"
echo "4. Review PROJECT_CLEANUP_REPORT.md for additional optimizations"
echo ""

