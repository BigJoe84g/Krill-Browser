#!/bin/bash
# Krill Browser Launch Script - PERFORMANCE OPTIMIZED
# Run this script to start Krill Browser

cd "$(dirname "$0")"

# Compile if needed
if [ ! -d "out" ] || [ "src/com/krillbrowser/KrillBrowser.java" -nt "out/com/krillbrowser/KrillBrowser.class" ]; then
    echo "🦐 Compiling Krill Browser..."
    javac --module-path javafx-sdk-21.0.5/lib --add-modules javafx.controls,javafx.web,javafx.media -d out src/module-info.java src/com/krillbrowser/*.java
fi

echo "🦐 Starting Krill Browser (Performance Mode)..."

# JVM Performance Flags for 2017 Intel Mac:
# -Xms512m: Higher initial heap for media playback
# -Xmx2g: Allow more memory for heavy sites like YouTube
# -XX:+UseG1GC: Low latency garbage collector
# -Dprism.order=es2: Force hardware acceleration (OpenGL/Metal)
# -Dsun.java2d.opengl=true: Enable OpenGL pipeline for UI
# -Dprism.vsync=false: Disable vsync for maximum FPS
# -Djavafx.animation.pulse=60: Limit animations to 60fps to save CPU

java \
    -Xms512m \
    -Xmx2g \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Dprism.order=es2 \
    -Dsun.java2d.opengl=true \
    -Dprism.vsync=false \
    -Djavafx.animation.pulse=60 \
    --module-path javafx-sdk-21.0.5/lib:out \
    --add-modules javafx.controls,javafx.web,javafx.media \
    -m KrillBrowser/com.krillbrowser.KrillBrowser
