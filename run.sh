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
# Build the project (skip tests for speed)
echo "📦 Building Krill Browser..."
mvn clean package -DskipTests -q

# Run the jar with module access flags
# These flags are CRITICAL for JCEF on macOS/Java 17+
echo "🚀 Launching..."
java \
    --add-opens java.desktop/sun.awt=ALL-UNNAMED \
    --add-opens java.desktop/java.awt=ALL-UNNAMED \
    --add-opens java.desktop/java.awt.peer=ALL-UNNAMED \
    --add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED \
    --add-opens java.desktop/sun.lwawt=ALL-UNNAMED \
    --add-opens java.base/java.nio=ALL-UNNAMED \
    -jar target/krill-browser-2.0.0.jar
