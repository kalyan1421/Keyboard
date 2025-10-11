#!/bin/bash

# AI Keyboard - Quick Install & Test Script
# This script installs the updated app with the new API key and prepares it for testing

echo "🚀 AI Keyboard - API Key Fix Installation"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Check if we're in the right directory
if [ ! -f "pubspec.yaml" ]; then
    echo -e "${RED}❌ Error: Not in project root directory${NC}"
    echo "Please run this script from /Users/kalyan/AI-keyboard/"
    exit 1
fi

echo -e "${GREEN}✅ Found project directory${NC}"
echo ""

# Step 2: Check if device is connected
echo -e "${BLUE}📱 Checking for connected devices...${NC}"
if command -v adb &> /dev/null; then
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l | tr -d ' ')
    if [ "$DEVICE_COUNT" -eq "0" ]; then
        echo -e "${YELLOW}⚠️  No device detected${NC}"
        echo "Please connect an Android device or start an emulator"
        echo ""
        echo "Options:"
        echo "1. Connect device via USB and enable USB debugging"
        echo "2. Start Android emulator"
        echo "3. Continue anyway (you can install manually later)"
        echo ""
        read -p "Continue? (y/n) " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo -e "${GREEN}✅ Found $DEVICE_COUNT Android device(s)${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  ADB not found - will build APK only${NC}"
fi
echo ""

# Step 3: Verify API key is updated
echo -e "${BLUE}🔑 Verifying API key configuration...${NC}"
if grep -q "sk-proj-qQDN3yb5C_sChh-" android/app/src/main/kotlin/com/example/ai_keyboard/OpenAIConfig.kt; then
    echo -e "${GREEN}✅ API key is correct (new key found)${NC}"
else
    echo -e "${RED}❌ Error: API key not updated!${NC}"
    echo "Expected to find: sk-proj-qQDN3yb5C_sChh-..."
    echo "Please check OpenAIConfig.kt"
    exit 1
fi
echo ""

# Step 4: Build the app
echo -e "${BLUE}🔨 Building APK with new API key...${NC}"
echo "This may take 30-60 seconds..."
echo ""

flutter build apk --debug

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Build successful!${NC}"
else
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi
echo ""

# Step 5: Install if device is available
if command -v adb &> /dev/null && [ "$DEVICE_COUNT" -gt "0" ]; then
    echo -e "${BLUE}📲 Installing app...${NC}"
    flutter install
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ App installed successfully!${NC}"
        echo ""
        
        # Step 6: Clear app data
        echo -e "${BLUE}🗑️  Clearing old app data (to use new API key)...${NC}"
        adb shell pm clear com.example.ai_keyboard
        echo -e "${GREEN}✅ App data cleared${NC}"
        echo ""
        
        # Step 7: Launch app
        echo -e "${BLUE}🚀 Launching app...${NC}"
        adb shell am start -n com.example.ai_keyboard/.MainActivity
        echo -e "${GREEN}✅ App launched${NC}"
        echo ""
    else
        echo -e "${RED}❌ Installation failed${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️  No device available for installation${NC}"
    echo ""
    echo "APK location: build/app/outputs/flutter-apk/app-debug.apk"
    echo ""
    echo "To install manually:"
    echo "1. Copy app-debug.apk to your device"
    echo "2. Open it on your device to install"
    echo "3. Go to Settings → Apps → AI Keyboard → Storage → Clear Data"
    echo ""
fi

# Summary
echo ""
echo "=========================================="
echo -e "${GREEN}✅ Installation Complete!${NC}"
echo "=========================================="
echo ""
echo "Next Steps:"
echo ""
echo "1️⃣  Enable the keyboard:"
echo "   Settings → System → Languages & Input → On-screen keyboard"
echo "   → Enable 'AI Keyboard'"
echo ""
echo "2️⃣  Grant Full Access (for AI features):"
echo "   Toggle AI Keyboard → Allow 'Full Access'"
echo ""
echo "3️⃣  Test AI Features:"
echo "   • Open any text app (Messages, Notes, etc.)"
echo "   • Type something"
echo "   • Try the AI buttons: ✅ Grammar, 🎭 Tone, 🤖 Assistant"
echo ""
echo "📊 Monitor API usage: https://platform.openai.com/usage"
echo ""
echo "📖 Documentation:"
echo "   • API_KEY_FIX_COMPLETE.md - Full details"
echo "   • QUICK_TEST_GUIDE.md - Testing instructions"
echo ""

# Check logs if ADB available
if command -v adb &> /dev/null && [ "$DEVICE_COUNT" -gt "0" ]; then
    echo "🔍 To monitor logs:"
    echo "   adb logcat | grep -E '(OpenAIConfig|AdvancedAIService)'"
    echo ""
fi

echo -e "${GREEN}Done! 🎉${NC}"
echo ""


