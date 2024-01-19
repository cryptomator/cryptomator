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
MACHINE_TYPE=$(uname -m)

if [[ ! "${MACHINE_TYPE}" =~ x86_64|aarch64 ]]; then echo "Platform ${MACHINE_TYPE} not supported"; exit 1; fi

mvn -f ../../../pom.xml versions:set -DnewVersion=${SEMVER_STR}

# compile
mvn -B -f ../../../pom.xml clean package -Plinux -DskipTests
cp ../../../LICENSE.txt ../../../target
cp ../../../target/cryptomator-*.jar ../../../target/mods


# download javaFX jmods
OPENJFX_URL='https://download2.gluonhq.com/openjfx/21.0.1/openjfx-21.0.1_linux-x64_bin-jmods.zip'
OPENJFX_SHA='7baed11ca56d5fee85995fa6612d4299f1e8b7337287228f7f12fd50407c56f8'
OPENJFX_URL_aarch64='https://download2.gluonhq.com/openjfx/21.0.1/openjfx-21.0.1_linux-aarch64_bin-jmods.zip'
OPENJFX_SHA_aarch64='871e7b9d7af16aef2e55c1b7830d0e0b2503b13dd8641374ba7e55ecb81d2ef9'

if [[ "${MACHINE_TYPE}" = "aarch64" ]]; then
	OPENJFX_URL="${OPENJFX_URL_aarch64}";
	OPENJFX_SHA="${OPENJFX_SHA_aarch64}";
fi

curl -L ${OPENJFX_URL} -o openjfx-jmods.zip
echo "${OPENJFX_SHA}  openjfx-jmods.zip" | shasum -a256 --check
mkdir -p openjfx-jmods
unzip -j openjfx-jmods.zip \*/javafx.base.jmod \*/javafx.controls.jmod \*/javafx.fxml.jmod \*/javafx.graphics.jmod -d openjfx-jmods
JMOD_VERSION=$(jmod describe openjfx-jmods/javafx.base.jmod | head -1)
JMOD_VERSION=${JMOD_VERSION#*@}
JMOD_VERSION=${JMOD_VERSION%%.*}
POM_JFX_VERSION=$(mvn help:evaluate "-Dexpression=javafx.version" -q -DforceStdout)
POM_JFX_VERSION=${POM_JFX_VERSION#*@}
POM_JFX_VERSION=${POM_JFX_VERSION%%.*}
if [ $POM_JFX_VERSION -ne $JMOD_VERSION_AMD64 ]; then
	>&2 echo "Major JavaFX version in pom.xml (${POM_JFX_VERSION}) != amd64 jmod version (${JMOD_VERSION})"
	exit 1
fi


# add runtime
${JAVA_HOME}/bin/jlink \
    --verbose \
    --output runtime \
    --module-path "${JAVA_HOME}/jmods:openjfx-jmods" \
    --add-modules java.base,java.desktop,java.instrument,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,javafx.base,javafx.graphics,javafx.controls,javafx.fxml,jdk.unsupported,jdk.crypto.ec,jdk.security.auth,jdk.accessibility,jdk.management.jfr,jdk.net \
    --strip-native-commands \
    --no-header-files \
    --no-man-pages \
    --strip-debug \
    --compress zip-0

# create app dir
envsubst '${SEMVER_STR} ${REVISION_NUM}' < ../launcher-gtk2.properties > launcher-gtk2.properties
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
    --java-options "--enable-native-access=org.cryptomator.jfuse.linux.amd64,org.cryptomator.jfuse.linux.aarch64,org.purejava.appindicator" \
    --copyright "(C) 2016 - 2024 Skymatic GmbH" \
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
    --add-launcher cryptomator-gtk2=launcher-gtk2.properties \
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
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/Cryptomator.svg
ln -s usr/share/icons/hicolor/scalable/apps/org.cryptomator.Cryptomator.svg Cryptomator.AppDir/.DirIcon
ln -s usr/share/applications/org.cryptomator.Cryptomator.desktop Cryptomator.AppDir/Cryptomator.desktop
ln -s bin/cryptomator.sh Cryptomator.AppDir/AppRun

# load AppImageTool
curl -L https://github.com/AppImage/AppImageKit/releases/download/13/appimagetool-${MACHINE_TYPE}.AppImage -o /tmp/appimagetool.AppImage
chmod +x /tmp/appimagetool.AppImage

# create AppImage
/tmp/appimagetool.AppImage \
    Cryptomator.AppDir \
    cryptomator-${SEMVER_STR}-${MACHINE_TYPE}.AppImage  \
    -u 'gh-releases-zsync|cryptomator|cryptomator|latest|cryptomator-*-${MACHINE_TYPE}.AppImage.zsync'

echo ""
echo "Done. AppImage successfully created: cryptomator-${SEMVER_STR}-${MACHINE_TYPE}.AppImage"
echo ""
echo >&2 "To clean up, run: rm -rf Cryptomator.AppDir appdir runtime squashfs-root openjfx-jmods; rm launcher-gtk2.properties /tmp/appimagetool.AppImage openjfx-jmods.zip"
echo ""
