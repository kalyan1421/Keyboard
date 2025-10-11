#!/bin/bash

echo "🔍 Verifying Firebase Initialization Order in AppDelegate.swift"
echo "============================================================"
echo ""

# Extract the relevant section
APPDELEGATE="ios/Runner/AppDelegate.swift"

# Check if Firebase import exists
if grep -q "import Firebase" "$APPDELEGATE"; then
    echo "✅ Firebase import found"
else
    echo "❌ Firebase import missing"
    exit 1
fi

# Find the line numbers
FIREBASE_LINE=$(grep -n "FirebaseApp.configure()" "$APPDELEGATE" | head -1 | cut -d: -f1)
CONTROLLER_LINE=$(grep -n "let controller = window?.rootViewController as! FlutterViewController" "$APPDELEGATE" | head -1 | cut -d: -f1)
PLUGIN_LINE=$(grep -n "GeneratedPluginRegistrant.register(with: self)" "$APPDELEGATE" | head -1 | cut -d: -f1)

echo ""
echo "📍 Line Numbers:"
echo "   Firebase init:        Line $FIREBASE_LINE"
echo "   FlutterViewController: Line $CONTROLLER_LINE"
echo "   Plugin registration:   Line $PLUGIN_LINE"
echo ""

# Verify order
if [ "$FIREBASE_LINE" -lt "$CONTROLLER_LINE" ] && [ "$CONTROLLER_LINE" -lt "$PLUGIN_LINE" ]; then
    echo "✅ CORRECT ORDER:"
    echo "   1️⃣ FirebaseApp.configure() (Line $FIREBASE_LINE)"
    echo "   2️⃣ FlutterViewController access (Line $CONTROLLER_LINE)"
    echo "   3️⃣ Plugin registration (Line $PLUGIN_LINE)"
    echo ""
    echo "🎯 Initialization sequence is CORRECT!"
    echo "✅ This should prevent Dart VM crashes"
else
    echo "❌ WRONG ORDER detected!"
    exit 1
fi

echo ""
echo "============================================================"
echo "🚀 Ready to test. Run: flutter run"
