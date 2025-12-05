#!/bin/bash
# Krill Browser Launch Script
# Run this script to start Krill Browser

cd "$(dirname "$0")"

# Compile if needed
if [ ! -d "out" ] || [ "src/com/krillbrowser/KrillBrowser.java" -nt "out/com/krillbrowser/KrillBrowser.class" ]; then
    echo "🦐 Compiling Krill Browser..."
    javac --module-path javafx-sdk-21.0.5/lib --add-modules javafx.controls,javafx.web -d out src/module-info.java src/com/krillbrowser/*.java
fi

echo "🦐 Starting Krill Browser..."
java --module-path javafx-sdk-21.0.5/lib:out --add-modules javafx.controls,javafx.web -m KrillBrowser/com.krillbrowser.KrillBrowser
