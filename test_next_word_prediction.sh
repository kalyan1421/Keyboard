#!/bin/bash

# 🧪 Next-Word Prediction Testing Script
# Tests bigram and trigram predictions in real-time

echo "🎯 Next-Word Prediction Testing Suite"
echo "======================================"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

echo -e "${BLUE}📋 Step 1: Clearing old logs...${NC}"
adb logcat -c
echo ""

echo -e "${BLUE}📋 Step 2: Starting prediction monitor...${NC}"
echo -e "${YELLOW}Watch for these key indicators:${NC}"
echo "  📊 Bigram predictions for 'X': [...]"
echo "  🔺 Trigram boost: 'X Y Z' = N"
echo "  🔹 Bigram boost: 'X Y' = N"
echo "  ✅ Returning N next-word predictions"
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${PURPLE}📝 Test Instructions:${NC}"
echo "1. Type 'thank ' (with space) → should suggest: you, god, everyone"
echo "2. Type 'how ' (with space) → should suggest: are, much, many"
echo "3. Type 'good ' (with space) → should suggest: morning, night, luck"
echo "4. Type 'I am f' → should rank 'fine' higher (trigram context)"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop monitoring${NC}"
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""

# Monitor logs with highlighting
adb logcat | grep -E "(📊 Bigram|🔺 Trigram|🔹 Bigram|✅ Returning.*predictions|🔮 Next-word)" --color=always


