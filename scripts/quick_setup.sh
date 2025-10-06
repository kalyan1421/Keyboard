#!/bin/bash

# Quick setup script for Firestore word frequency
# This will install dependencies and upload sample data

set -e

echo "🚀 AI Keyboard - Firestore Word Frequency Quick Setup"
echo "======================================================"
echo ""

# Check if we're in the scripts directory
if [ ! -f "populate_word_frequency.js" ]; then
    echo "❌ Error: Please run this script from the scripts directory"
    echo "   cd scripts && ./quick_setup.sh"
    exit 1
fi

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Error: Node.js is not installed"
    echo "   Install from: https://nodejs.org/"
    exit 1
fi

# Install dependencies
echo "📦 Installing dependencies..."
npm install

# Check if service account key exists
if [ ! -f "serviceAccountKey.json" ]; then
    echo ""
    echo "⚠️  Service account key not found"
    echo ""
    echo "📝 Please download your Firebase service account key:"
    echo "   1. Go to: https://console.firebase.google.com/"
    echo "   2. Select your project (aikeyboard-18ed9)"
    echo "   3. Go to Project Settings → Service Accounts"
    echo "   4. Click 'Generate New Private Key'"
    echo "   5. Save the JSON file as 'serviceAccountKey.json' in this directory"
    echo ""
    echo "Then run this script again: ./quick_setup.sh"
    exit 1
fi

# Set environment variable
export GOOGLE_APPLICATION_CREDENTIALS="./serviceAccountKey.json"

# Run population script with sample data
echo ""
echo "📤 Uploading sample word frequency data..."
node populate_word_frequency.js

echo ""
echo "✅ Setup complete!"
echo ""
echo "🎯 Next steps:"
echo "   1. Rebuild and reinstall your keyboard app"
echo "   2. Check logs: adb logcat -s AutocorrectEngine"
echo "   3. Look for: '📊 Loaded X frequency entries for en from Firestore'"
echo ""
echo "📚 For production use, replace sample data with real corpus data"
echo "   See: README_FREQUENCY_SETUP.md"

