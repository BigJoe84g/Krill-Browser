#!/bin/bash
# Krill Browser v2.0 - Chromium Edition
# This script runs the JCEF-based browser with proper JVM flags

cd "$(dirname "$0")"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven first."
    echo "   Run: brew install maven"
    exit 1
fi

echo "🦐 Starting Krill Browser (Chromium Edition)..."

# Run with required JVM flags for JCEF on macOS
mvn exec:java \
    -Dexec.mainClass="com.krillbrowser.KrillBrowserChromium" \
    -Dexec.args="" \
    -Dexec.classpathScope="runtime" \
    -Djava.awt.headless=false \
    --add-opens java.desktop/sun.awt=ALL-UNNAMED \
    --add-opens java.desktop/java.awt=ALL-UNNAMED \
    --add-opens java.desktop/java.awt.peer=ALL-UNNAMED \
    2>&1 | grep -v "^\[WARNING\]" | grep -v "^Downloading"
