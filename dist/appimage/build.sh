#!/bin/bash

BASEDIR=$(dirname $0)/../..
REVISION_NO=`git rev-list --count HEAD`

# check preconditions
if [ -z "${JAVA_HOME}" ]; then echo "JAVA_HOME not set. Run using JAVA_HOME=/path/to/jdk ./build.sh"; exit 1; fi
command -v mvn >/dev/null 2>&1 || { echo >&2 "mvn not found."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo >&2 "curl not found."; exit 1; }

# compile
mvn -B -f${BASEDIR}/pom.xml clean package -DskipTests -Plinux

# prepare AppDir
mkdir ${BASEDIR}/target/Cryptomator.AppDir
mv ${BASEDIR}/target/libs ${BASEDIR}/target/Cryptomator.AppDir
mv ${BASEDIR}/target/mods ${BASEDIR}/target/Cryptomator.AppDir
mv ${BASEDIR}/LICENSE.txt ${BASEDIR}/target/Cryptomator.AppDir
cd ${BASEDIR}/target/Cryptomator.AppDir

# add runtime
${JAVA_HOME}/bin/jlink
          --output runtime
          --module-path "${JAVA_HOME}/jmods"
          --add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.crypto.ec,jdk.accessibility
          --no-header-files
          --no-man-pages
          --strip-debug
          --compress=1

# extract jffi
JFFI_NATIVE_JAR=`ls libs | grep -e 'jffi-[1-9]\.[0-9]\{1,2\}.[0-9]\{1,2\}-native.jar'`
${JAVA_HOME}/bin/jar -xf libs/${JFFI_NATIVE_JAR} /jni/x86_64-Linux/
mv jni/x86_64-Linux/* libs/libjffi.so
rm -r jni/x86_64-Linux

# finalize AppDir
envsubst '${REVISION_NO}' < ${BASEDIR}/dist/appimage/resources/AppDir/bin/cryptomator.sh > Cryptomator.AppDir/bin/cryptomator.sh
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/org.cryptomator.Cryptomator.svg
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/Cryptomator.svg
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/.DirIcon
ln -s usr/share/applications/org.cryptomator.Cryptomator.desktop Cryptomator.AppDir/Cryptomator.desktop
ln -s bin/cryptomator.sh Cryptomator.AppDir/AppRun

# load AppImageTool
curl -L https://github.com/AppImage/AppImageKit/releases/download/13/appimagetool-x86_64.AppImage -o /tmp/appimagetool.AppImage
chmod +x /tmp/appimagetool.AppImage

# create AppImage
/tmp/appimagetool.AppImage \
    ${BASEDIR}/target/Cryptomator.AppDir \
    cryptomator-SNAPSHOT-x86_64.AppImage \
    -u 'gh-releases-zsync|cryptomator|cryptomator|latest|cryptomator-*-x86_64.AppImage.zsync'