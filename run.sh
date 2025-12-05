#!/bin/bash
# Krill Browser Launch Script - PERFORMANCE OPTIMIZED
# Run this script to start Krill Browser

cd "$(dirname "$0")"

# Compile if needed
if [ ! -d "out" ] || [ "src/com/krillbrowser/KrillBrowser.java" -nt "out/com/krillbrowser/KrillBrowser.class" ]; then
    echo "🦐 Compiling Krill Browser..."
    javac --module-path javafx-sdk-21.0.5/lib --add-modules javafx.controls,javafx.web -d out src/module-info.java src/com/krillbrowser/*.java
fi

echo "🦐 Starting Krill Browser (Performance Mode)..."

# JVM Performance Flags:
# -Xms256m: Initial heap size (faster startup)
# -Xmx1g: Max heap size (prevents memory issues)
# -XX:+UseG1GC: Modern garbage collector (lower latency)
# -XX:+UseStringDeduplication: Reduce string memory usage
# -Dprism.order=sw: Software rendering (more compatible)
# -Dprism.vsync=false: Disable vsync for faster rendering

java \
    -Xms256m \
    -Xmx1g \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Dprism.vsync=false \
    -Djavafx.animation.pulse=60 \
    --module-path javafx-sdk-21.0.5/lib:out \
    --add-modules javafx.controls,javafx.web \
    -m KrillBrowser/com.krillbrowser.KrillBrowser
