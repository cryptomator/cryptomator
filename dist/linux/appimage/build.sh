#!/bin/bash

cd $(dirname $0)
REVISION_NO=`git rev-list --count HEAD`

# check preconditions
if [ -z "${JAVA_HOME}" ]; then echo "JAVA_HOME not set. Run using JAVA_HOME=/path/to/jdk ./build.sh"; exit 1; fi
command -v mvn >/dev/null 2>&1 || { echo >&2 "mvn not found."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo >&2 "curl not found."; exit 1; }

VERSION=$(mvn -f ../../../pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout)

# compile
mvn -B -f ../../../pom.xml clean package -DskipTests -Plinux
cp ../../../target/cryptomator-*.jar ../../../target/mods

# add runtime
${JAVA_HOME}/bin/jlink \
    --output runtime \
    --module-path "${JAVA_HOME}/jmods" \
    --add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.crypto.ec,jdk.accessibility,jdk.management.jfr \
    --no-header-files \
    --no-man-pages \
    --strip-debug \
    --compress=1

# create app dir
${JAVA_HOME}/bin/jpackage \
    --verbose \
    --type app-image \
    --runtime-image runtime \
    --input ../../../target/libs \
    --module-path ../../../target/mods \
    --module org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator \
    --dest . \
    --name Cryptomator \
    --vendor "Skymatic GmbH" \
    --copyright "(C) 2016 - 2022 Skymatic GmbH" \
    --java-options "-Xss5m" \
    --java-options "-Xmx256m" \
    --app-version "${VERSION}.${REVISION_NO}" \
    --java-options "-Dfile.encoding=\"utf-8\"" \
    --java-options "-Dcryptomator.logDir=\"~/.local/share/Cryptomator/logs\"" \
    --java-options "-Dcryptomator.pluginDir=\"~/.local/share/Cryptomator/plugins\"" \
    --java-options "-Dcryptomator.settingsPath=\"~/.config/Cryptomator/settings.json:~/.Cryptomator/settings.json\"" \
    --java-options "-Dcryptomator.ipcSocketPath=\"~/.config/Cryptomator/ipc.socket\"" \
    --java-options "-Dcryptomator.mountPointsDir=\"~/.local/share/Cryptomator/mnt\"" \
    --java-options "-Dcryptomator.showTrayIcon=false" \
    --java-options "-Dcryptomator.buildNumber=\"appimage-${REVISION_NO}\"" \
    --resource-dir ../resources

# transform AppDir
mv Cryptomator Cryptomator.AppDir
cp -r resources/AppDir/* Cryptomator.AppDir/
chmod +x Cryptomator.AppDir/lib/runtime/bin/java
envsubst '${REVISION_NO}' < resources/AppDir/bin/cryptomator.sh > Cryptomator.AppDir/bin/cryptomator.sh
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/org.cryptomator.Cryptomator.svg
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/Cryptomator.svg
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/.DirIcon
ln -s usr/share/applications/org.cryptomator.Cryptomator.desktop Cryptomator.AppDir/Cryptomator.desktop
ln -s bin/cryptomator.sh Cryptomator.AppDir/AppRun

# extract jffi
JFFI_NATIVE_JAR=`ls Cryptomator.AppDir/lib/app | grep -e 'jffi-[1-9]\.[0-9]\{1,2\}.[0-9]\{1,2\}-native.jar'`
${JAVA_HOME}/bin/jar -xf Cryptomator.AppDir/lib/app/${JFFI_NATIVE_JAR} /jni/x86_64-Linux/
mv jni/x86_64-Linux/* Cryptomator.AppDir/lib/app/libjffi.so
rm -r jni/x86_64-Linux

# load AppImageTool
curl -L https://github.com/AppImage/AppImageKit/releases/download/13/appimagetool-x86_64.AppImage -o /tmp/appimagetool.AppImage
chmod +x /tmp/appimagetool.AppImage

# create AppImage
/tmp/appimagetool.AppImage \
    Cryptomator.AppDir \
    cryptomator-SNAPSHOT-x86_64.AppImage \
    -u 'gh-releases-zsync|cryptomator|cryptomator|latest|cryptomator-*-x86_64.AppImage.zsync'
