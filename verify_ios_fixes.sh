#!/bin/bash
# 
# iOS Fixes Verification Script
# AI Keyboard Project
# Date: October 8, 2025
#
# This script verifies all iOS fixes are correctly applied
#

echo "🎯 AI Keyboard - iOS Fixes Verification"
echo "========================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0

# Function to check and report
check() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ PASS${NC}: $1"
        PASS=$((PASS + 1))
    else
        echo -e "${RED}❌ FAIL${NC}: $1"
        FAIL=$((FAIL + 1))
    fi
}

echo "📋 Running Pre-Build Checks..."
echo ""

# Check 1: Firebase import
echo "🔍 Check 1: Firebase import in AppDelegate..."
grep -q "import Firebase" ios/Runner/AppDelegate.swift
check "Firebase import found"

# Check 2: Firebase initialization
echo "🔍 Check 2: Firebase initialization..."
grep -q "FirebaseApp.configure()" ios/Runner/AppDelegate.swift
check "Firebase initialization found"

# Check 3: App Group ID in AppDelegate (line 97)
echo "🔍 Check 3: App Group ID in AppDelegate line 97..."
grep -q 'suiteName: "group.com.example.aiKeyboard.shared"' ios/Runner/AppDelegate.swift
check "App Group ID correct in AppDelegate"

# Check 4: App Group ID in KeyboardExtensionManager (line 185)
echo "🔍 Check 4: App Group ID in KeyboardExtensionManager..."
grep -A 5 "func sendSettingsUpdate" ios/Runner/AppDelegate.swift | grep -q "group.com.example.aiKeyboard.shared"
check "App Group ID correct in KeyboardExtensionManager"

# Check 5: ShortcutsManager enabled
echo "🔍 Check 5: ShortcutsManager enabled..."
grep -A 2 "Setup shortcuts" ios/Runner/AppDelegate.swift | grep -v "//" | grep -q "ShortcutsManager.shared.setupKeyboardShortcuts()"
check "ShortcutsManager enabled"

# Check 6: Darwin notification posting
echo "🔍 Check 6: Darwin notification posting..."
grep -q "CFNotificationCenterPostNotification" ios/Runner/AppDelegate.swift
check "Darwin notification posting found"

# Check 7: Darwin notification observer
echo "🔍 Check 7: Darwin notification observer..."
grep -q "CFNotificationCenterAddObserver" ios/KeyboardExtension/KeyboardViewController.swift
check "Darwin notification observer found"

# Check 8: NumberLayoutManager exists
echo "🔍 Check 8: NumberLayoutManager.swift exists..."
test -f ios/KeyboardExtension/NumberLayoutManager.swift
check "NumberLayoutManager.swift exists"

# Check 9: numbersPressed() implementation
echo "🔍 Check 9: numbersPressed() implementation..."
grep -A 5 "func numbersPressed()" ios/KeyboardExtension/KeyboardViewController.swift | grep -q "switchToLayout"
check "numbersPressed() implemented"

# Check 10: Key preview methods
echo "🔍 Check 10: Key preview methods..."
grep -q "func showKeyPreview()" ios/KeyboardExtension/KeyButton.swift
check "Key preview methods found"

# Check 11: Row staggering
echo "🔍 Check 11: Row staggering logic..."
grep -q "func getRowStaggerOffset" ios/KeyboardExtension/LayoutManager.swift
check "Row staggering logic found"

# Check 12: Enhanced auto-capitalization
echo "🔍 Check 12: Enhanced auto-capitalization..."
grep -q "sentenceEnders" ios/KeyboardExtension/KeyboardViewController.swift
check "Enhanced auto-capitalization found"

# Check 13: Duplicate files removed
echo "🔍 Check 13: Duplicate config files removed..."
if [ ! -f "ios/Runner/GoogleService-Info 2.plist" ] && [ ! -f "ios/Runner/GoogleService-Info 3.plist" ]; then
    echo -e "${GREEN}✅ PASS${NC}: Duplicate files removed"
    PASS=$((PASS + 1))
else
    echo -e "${RED}❌ FAIL${NC}: Duplicate files still exist"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "========================================"
echo "📊 Results Summary"
echo "========================================"
echo -e "${GREEN}Passed: $PASS${NC}"
echo -e "${RED}Failed: $FAIL${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL CHECKS PASSED!${NC}"
    echo ""
    echo "✅ All iOS fixes are correctly applied"
    echo "📋 Next steps:"
    echo "   1. Run: flutter clean"
    echo "   2. Run: cd ios && pod install && cd .."
    echo "   3. Open: open ios/Runner.xcworkspace"
    echo "   4. Build in Xcode (Cmd+B)"
    echo "   5. Follow tests in IOS_FIX_VERIFICATION_PLAN.md"
    echo ""
    exit 0
else
    echo -e "${RED}⚠️  SOME CHECKS FAILED${NC}"
    echo ""
    echo "Please review the failed checks above and:"
    echo "   1. Verify files were saved correctly"
    echo "   2. Check git status for uncommitted changes"
    echo "   3. Review IOS_FIXES_COMPLETE_SUMMARY.md for details"
    echo ""
    exit 1
fi

