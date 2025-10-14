#!/bin/bash

# 🧪 SwipeAutocorrectEngine Fix Verification Script
# Tests the initialization and proper functioning of SwipeAutocorrectEngine

echo "🔧 SwipeAutocorrectEngine Fix Verification"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Clear old logs
echo -e "${BLUE}📋 Clearing old logs...${NC}"
adb logcat -c
echo ""

echo -e "${BLUE}🏗️  Building and installing app...${NC}"
echo -e "${YELLOW}(This may take a minute)${NC}"
echo ""

# Build in background
flutter run &
BUILD_PID=$!

# Wait for build
sleep 10

echo ""
echo -e "${GREEN}✅ App should be running now${NC}"
echo ""
echo -e "${BLUE}📊 Monitoring SwipeAutocorrectEngine logs...${NC}"
echo -e "${YELLOW}Looking for these key messages:${NC}"
echo "  ✅ SwipeAutocorrectEngine initialized and linked with UnifiedAutocorrectEngine"
echo "  ⚠️  SwipeAutocorrectEngine not initialized yet - ignoring swipe gesture"
echo "  ❌ Failed to initialize SwipeAutocorrectEngine"
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""

# Monitor logs with color highlighting
adb logcat | grep -E "(SwipeAutocorrectEngine|Enhanced swipe)" --color=always | while IFS= read -r line; do
    if [[ $line == *"✅"* ]]; then
        echo -e "${GREEN}$line${NC}"
    elif [[ $line == *"⚠️"* ]]; then
        echo -e "${YELLOW}$line${NC}"
    elif [[ $line == *"❌"* ]]; then
        echo -e "${RED}$line${NC}"
    else
        echo "$line"
    fi
done

# Note: Script runs until Ctrl+C

