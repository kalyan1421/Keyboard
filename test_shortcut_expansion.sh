#!/bin/bash

# 🎯 Quick Test Script for Shortcut Expansion Feature
# This script helps verify the instant shortcut expansion implementation

echo "🚀 Starting Shortcut Expansion Test..."
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}📋 Step 1: Building and installing app...${NC}"
flutter run -d $(flutter devices | grep -v "Chrome" | grep -v "web" | tail -n +2 | head -n 1 | awk '{print $1}') &
BUILD_PID=$!

echo ""
echo -e "${YELLOW}⏳ Waiting for build to complete (this may take a minute)...${NC}"
sleep 5

echo ""
echo -e "${BLUE}📋 Step 2: Starting logcat monitor...${NC}"
echo -e "${YELLOW}Watch for these key log messages:${NC}"
echo "  • 📢 DICTIONARY_CHANGED broadcast received!"
echo "  • ✅ Reloaded dictionary from Flutter SharedPreferences"
echo "  • ⚙️ Shortcut expanded: 'gm' → 'good morning'"
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""

# Monitor relevant logs
adb logcat -c  # Clear old logs
adb logcat | grep -E "(AIKeyboardService|DictionaryManager)" --color=always | grep -E "(DICTIONARY|Shortcut|expansion|Reloaded)"

# Note: Script will run until you press Ctrl+C

