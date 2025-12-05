#!/bin/bash
# Get the directory where this script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed."
    echo "Please install Java 17 or later to run Krill Browser."
    read -p "Press any key to exit..."
    exit 1
fi

# Run the browser
echo "🦐 Launching Krill Browser..."
java -jar KrillBrowser.jar
