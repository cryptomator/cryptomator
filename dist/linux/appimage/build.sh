#!/bin/bash
set -e

cd $(dirname $0)
REVISION_NO=`git rev-list --count HEAD`

# check preconditions
if [ -z "${JAVA_HOME}" ]; then echo "JAVA_HOME not set. Run using JAVA_HOME=/path/to/jdk ./build.sh"; exit 1; fi
command -v mvn >/dev/null 2>&1 || { echo >&2 "mvn not found."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo >&2 "curl not found."; exit 1; }
command -v unzip >/dev/null 2>&1 || { echo >&2 "unzip not found."; exit 1; }

VERSION=$(mvn -f ../../../pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout)
SEMVER_STR=${VERSION}
CPU_ARCH=$(uname -p)

if [[ ! "${CPU_ARCH}" =~ x86_64|aarch64 ]]; then echo "Platform ${CPU_ARCH} not supported"; exit 1; fi

mvn -f ../../../pom.xml versions:set -DnewVersion=${SEMVER_STR}

# compile
mvn -B -f ../../../pom.xml clean package -Plinux -DskipTests -Djavafx.platform=linux
cp ../../../LICENSE.txt ../../../target
cp ../../../target/cryptomator-*.jar ../../../target/mods

JAVAFX_VERSION=24.0.1
JAVAFX_ARCH="x64"
JAVAFX_JMODS_SHA256='425fac742b9fbd095b2ce868cff82d1024620f747c94a7144d0a4879e756146c'
if [ "${CPU_ARCH}" = "aarch64" ]; then
    JAVAFX_ARCH="aarch64"
    JAVAFX_JMODS_SHA256='7e02edd0f4ee5527a27c94b0bbba66fcaaff41009119e45d0eca0f96ddfb6e7b'
fi

# download javaFX jmods
JAVAFX_JMODS_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_linux-${JAVAFX_ARCH}_bin-jmods.zip"


curl -L ${JAVAFX_JMODS_URL} -o openjfx-jmods.zip
echo "${JAVAFX_JMODS_SHA256}  openjfx-jmods.zip" | shasum -a256 --check
mkdir -p openjfx-jmods
unzip -o -j openjfx-jmods.zip \*/javafx.base.jmod \*/javafx.controls.jmod \*/javafx.fxml.jmod \*/javafx.graphics.jmod -d openjfx-jmods
JMOD_VERSION=$(jmod describe ./openjfx-jmods/javafx.base.jmod | head -1)
JMOD_VERSION=${JMOD_VERSION#*@}
JMOD_VERSION=${JMOD_VERSION%%.*}
POM_JFX_VERSION=$(mvn help:evaluate "-Dexpression=javafx.version" -q -DforceStdout -B -f ../../../pom.xml)
POM_JFX_VERSION=${POM_JFX_VERSION#*@}
POM_JFX_VERSION=${POM_JFX_VERSION%%.*}
if [ $POM_JFX_VERSION -ne $JMOD_VERSION ]; then
	>&2 echo "Major JavaFX version in pom.xml (${POM_JFX_VERSION}) != amd64 jmod version (${JMOD_VERSION})"
	exit 1
fi


# create runtime
## check for JEP 493
JMOD_PATHS="openjfx-jmods"
if ! ${JAVA_HOME}/bin/jlink --help | grep -q "Linking from run-time image enabled"; then
    JMOD_PATHS="${JAVA_HOME}/jmods:${JMOD_PATHS}"
fi
## create runtime image
${JAVA_HOME}/bin/jlink \
    --verbose \
    --output runtime \
    --module-path "${JMOD_PATHS}" \
    --add-modules java.base,java.desktop,java.instrument,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,javafx.base,javafx.graphics,javafx.controls,javafx.fxml,jdk.unsupported,jdk.security.auth,jdk.accessibility,jdk.management.jfr,jdk.net,java.compiler \
    --strip-native-commands \
    --no-header-files \
    --no-man-pages \
    --strip-debug \
    --compress zip-0

# create app dir
${JAVA_HOME}/bin/jpackage \
    --verbose \
    --type app-image \
    --runtime-image runtime \
    --input ../../../target/libs \
    --module-path ../../../target/mods \
    --module org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator \
    --dest appdir \
    --name Cryptomator \
    --vendor "Skymatic GmbH" \
    --java-options "--enable-preview" \
    --java-options "--enable-native-access=javafx.graphics,org.cryptomator.jfuse.linux.amd64,org.cryptomator.jfuse.linux.aarch64,org.purejava.appindicator" \
    --copyright "(C) 2016 - 2025 Skymatic GmbH" \
    --java-options "-Xss5m" \
    --java-options "-Xmx256m" \
    --app-version "${VERSION}.${REVISION_NO}" \
    --java-options "-Dfile.encoding=\"utf-8\"" \
    --java-options "-Djava.net.useSystemProxies=true" \
    --java-options "-Dcryptomator.logDir=\"@{userhome}/.local/share/Cryptomator/logs\"" \
    --java-options "-Dcryptomator.pluginDir=\"@{userhome}/.local/share/Cryptomator/plugins\"" \
    --java-options "-Dcryptomator.settingsPath=\"@{userhome}/.config/Cryptomator/settings.json:@{userhome}/.Cryptomator/settings.json\"" \
    --java-options "-Dcryptomator.p12Path=\"@{userhome}/.config/Cryptomator/key.p12\"" \
    --java-options "-Dcryptomator.ipcSocketPath=\"@{userhome}/.config/Cryptomator/ipc.socket\"" \
    --java-options "-Dcryptomator.mountPointsDir=\"@{userhome}/.local/share/Cryptomator/mnt\"" \
    --java-options "-Dcryptomator.showTrayIcon=true" \
    --java-options "-Dcryptomator.integrationsLinux.trayIconsDir=\"@{appdir}/usr/share/icons/hicolor/symbolic/apps\"" \
    --java-options "-Dcryptomator.buildNumber=\"appimage-${REVISION_NO}\"" \
    --java-options "-Dcryptomator.networking.truststore.p12Path=\"/etc/cryptomator/certs.p12\"" \
    --resource-dir ../resources

# transform AppDir
mv appdir/Cryptomator Cryptomator.AppDir
cp -r resources/AppDir/* Cryptomator.AppDir/
envsubst '${REVISION_NO}' < resources/AppDir/bin/cryptomator.sh > Cryptomator.AppDir/bin/cryptomator.sh
cp ../common/org.cryptomator.Cryptomator256.png Cryptomator.AppDir/usr/share/icons/hicolor/256x256/apps/org.cryptomator.Cryptomator.png
cp ../common/org.cryptomator.Cryptomator512.png Cryptomator.AppDir/usr/share/icons/hicolor/512x512/apps/org.cryptomator.Cryptomator.png
cp ../common/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg
cp ../common/org.cryptomator.Cryptomator.tray.svg Cryptomator.AppDir/usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.tray.svg
cp ../common/org.cryptomator.Cryptomator.tray-unlocked.svg Cryptomator.AppDir/usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.tray-unlocked.svg
cp ../common/org.cryptomator.Cryptomator.tray.svg Cryptomator.AppDir/usr/share/icons/hicolor/symbolic/apps/org.cryptomator.Cryptomator.tray-symbolic.svg
cp ../common/org.cryptomator.Cryptomator.tray-unlocked.svg Cryptomator.AppDir/usr/share/icons/hicolor/symbolic/apps/org.cryptomator.Cryptomator.tray-unlocked-symbolic.svg
cp ../common/org.cryptomator.Cryptomator.desktop Cryptomator.AppDir/usr/share/applications/org.cryptomator.Cryptomator.desktop
cp ../common/org.cryptomator.Cryptomator.metainfo.xml Cryptomator.AppDir/usr/share/metainfo/org.cryptomator.Cryptomator.metainfo.xml
cp ../common/application-vnd.cryptomator.vault.xml Cryptomator.AppDir/usr/share/mime/packages/application-vnd.cryptomator.vault.xml
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/org.cryptomator.Cryptomator.svg
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/.DirIcon
ln -s usr/share/applications/org.cryptomator.Cryptomator.desktop Cryptomator.AppDir/org.cryptomator.Cryptomator.desktop
ln -s org.cryptomator.Cryptomator.metainfo.xml Cryptomator.AppDir/usr/share/metainfo/org.cryptomator.Cryptomator.appdata.xml
ln -s bin/cryptomator.sh Cryptomator.AppDir/AppRun

# load AppImageTool
curl -L https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${CPU_ARCH}.AppImage -o /tmp/appimagetool.AppImage
chmod +x /tmp/appimagetool.AppImage

# create AppImage
/tmp/appimagetool.AppImage \
    Cryptomator.AppDir \
    cryptomator-${SEMVER_STR}-${CPU_ARCH}.AppImage  \
    -u 'gh-releases-zsync|cryptomator|cryptomator|latest|cryptomator-*-${CPU_ARCH}.AppImage.zsync'

echo ""
echo "Done. AppImage successfully created: cryptomator-${SEMVER_STR}-${CPU_ARCH}.AppImage"
echo ""
echo >&2 "To clean up, run: rm -rf Cryptomator.AppDir appdir runtime squashfs-root openjfx-jmods; rm /tmp/appimagetool.AppImage openjfx-jmods.zip"
echo ""
