#!/bin/bash

# Run Cryptomator with multi-identity feature
cd "$(dirname "$0")"

echo "Starting Cryptomator with multi-identity support..."
echo "==========================================="
echo ""

# Create directories if they don't exist
mkdir -p /tmp/cryptomator-ipc
mkdir -p /tmp/cryptomator-logs
mkdir -p /tmp/cryptomator-mounts

# Build with Maven (compiles and copies dependencies to mods/ and libs/)
echo "Building application..."
mvn prepare-package -Dmaven.test.skip=true -q

# Detect platform for JavaFX
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
if [[ "$OS" == "darwin" ]]; then
  JAVAFX_PLATFORM="mac"
elif [[ "$OS" == "linux" ]]; then
  JAVAFX_PLATFORM="linux"
else
  JAVAFX_PLATFORM="win"
fi

# Set up module path (modular JARs) and classpath (non-modular JARs)
MODULEPATH="target/classes"
CLASSPATH=""

# Add JavaFX modules to module path
JAVAFX_VERSION=25
JAVAFX_REPO="$HOME/.m2/repository/org/openjfx"
for module in base graphics controls fxml; do
  MODULEPATH="$MODULEPATH:$JAVAFX_REPO/javafx-$module/$JAVAFX_VERSION/javafx-$module-$JAVAFX_VERSION.jar"
  MODULEPATH="$MODULEPATH:$JAVAFX_REPO/javafx-$module/$JAVAFX_VERSION/javafx-$module-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
done

# Add modular dependencies
if [ -d "target/mods" ]; then
  for jar in target/mods/*.jar; do
    if [ -f "$jar" ]; then
      MODULEPATH="$MODULEPATH:$jar"
    fi
  done
fi

# Add non-modular dependencies to classpath
if [ -d "target/libs" ]; then
  for jar in target/libs/*.jar; do
    if [ -f "$jar" ]; then
      if [ -z "$CLASSPATH" ]; then
        CLASSPATH="$jar"
      else
        CLASSPATH="$CLASSPATH:$jar"
      fi
    fi
  done
fi

echo "Running with module path support..."

# Run with proper module support
java \
  --module-path "$MODULEPATH" \
  --class-path "$CLASSPATH" \
  --add-modules ALL-MODULE-PATH \
  -Dcryptomator.ipcSocketPath=/tmp/cryptomator-ipc/socket \
  -Dcryptomator.settingsPath=/tmp/cryptomator-settings.json \
  -Dcryptomator.logDir=/tmp/cryptomator-logs \
  -Dcryptomator.mountPointsDir=/tmp/cryptomator-mounts \
  -Xss20m \
  -Xmx512m \
  --enable-preview \
  --enable-native-access=org.cryptomator.jfuse.linux.amd64,org.cryptomator.jfuse.linux.aarch64,org.purejava.appindicator,javafx.graphics \
  --module org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator
