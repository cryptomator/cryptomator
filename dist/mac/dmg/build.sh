#!/bin/bash

# parse options
usage() { echo "Usage: $0 [-s <codesign-identity>] [-t <team-identifier>]" 1>&2; exit 1; }
while getopts ":s:t:" o; do
    case "${o}" in
        s)
            CODESIGN_IDENTITY=${OPTARG}
            ;;
        t)
            TEAM_IDENTIFIER=${OPTARG}
            ;;
        *)
            usage
            ;;
    esac
done
shift "$((OPTIND-1))"

# prepare working dir
cd $(dirname $0)
rm -rf runtime dmg *.app *.dmg

# set variables
APP_NAME="Cryptomator"
VENDOR="Skymatic GmbH"
COPYRIGHT_YEARS="2016 - 2025"
PACKAGE_IDENTIFIER="org.cryptomator"
MAIN_JAR_GLOB="cryptomator-*.jar"
MODULE_AND_MAIN_CLASS="org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator"
REVISION_NO=`git rev-list --count HEAD`
VERSION_NO=`mvn -f../../../pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout | sed -rn 's/.*([0-9]+\.[0-9]+\.[0-9]+).*/\1/p'`
FUSE_LIB="FUSE-T"

JAVAFX_VERSION=22.0.2
JAVAFX_ARCH="undefined"
JAVAFX_JMODS_SHA256="undefined"
if [ "$(machine)" = "arm64e" ]; then
    JAVAFX_ARCH="aarch64"
    JAVAFX_JMODS_SHA256="813c6748f7c99cb7a579d48b48a087b4682b1fad1fc1a4fe5f9b21cf872b15a7"
else
    JAVAFX_ARCH="x64"
    JAVAFX_JMODS_SHA256="115cb08bb59d880cfff6e51e0bf0dcc45785ed9d456b8b8425597b04da6ab3d4"
fi
JAVAFX_JMODS_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_osx-${JAVAFX_ARCH}_bin-jmods.zip"

# check preconditions
if [ -z "${JAVA_HOME}" ]; then echo "JAVA_HOME not set. Run using JAVA_HOME=/path/to/jdk ./build.sh"; exit 1; fi
command -v mvn >/dev/null 2>&1 || { echo >&2 "mvn not found. Fix by 'brew install maven'."; exit 1; }
command -v create-dmg >/dev/null 2>&1 || { echo >&2 "create-dmg not found. Fix by 'brew install create-dmg'."; exit 1; }
if [ -n "${CODESIGN_IDENTITY}" ]; then
    command -v codesign >/dev/null 2>&1 || { echo >&2 "codesign not found. Fix by 'xcode-select --install'."; exit 1; }
    if [[ ! `security find-identity -v -p codesigning | grep -w "${CODESIGN_IDENTITY}"` ]]; then echo "Given codesign identity is invalid."; exit 1; fi
fi

# download and check jmods
curl -L ${JAVAFX_JMODS_URL} -o openjfx-jmods.zip
echo "${JAVAFX_JMODS_SHA256}  openjfx-jmods.zip" | shasum -a256 --check
mkdir -p openjfx-jmods/
unzip -jo openjfx-jmods.zip \*/javafx.base.jmod \*/javafx.controls.jmod \*/javafx.fxml.jmod \*/javafx.graphics.jmod -d openjfx-jmods
JMOD_VERSION=$(jmod describe openjfx-jmods/javafx.base.jmod | head -1)
JMOD_VERSION=${JMOD_VERSION#*@}
JMOD_VERSION=${JMOD_VERSION%%.*}
POM_JFX_VERSION=$(mvn -f../../../pom.xml help:evaluate "-Dexpression=javafx.version" -q -DforceStdout)
POM_JFX_VERSION=${POM_JFX_VERSION#*@}
POM_JFX_VERSION=${POM_JFX_VERSION%%.*}

if [ "${POM_JFX_VERSION}" -ne "${JMOD_VERSION}" ]; then
    >&2 echo "Major JavaFX version in pom.xml (${POM_JFX_VERSION}) != jmod version (${JMOD_VERSION})"
    exit 1
fi

# compile
mvn -B -Djavafx.platform=mac -f../../../pom.xml clean package -DskipTests -Pmac
cp ../../../LICENSE.txt ../../../target
cp ../../../target/${MAIN_JAR_GLOB} ../../../target/mods

# add runtime
${JAVA_HOME}/bin/jlink \
    --output runtime \
    --module-path "${JAVA_HOME}/jmods:openjfx-jmods" \
    --add-modules java.base,java.desktop,java.instrument,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,javafx.base,javafx.graphics,javafx.controls,javafx.fxml,jdk.unsupported,jdk.security.auth,jdk.accessibility,jdk.management.jfr,java.compiler \
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
    --module ${MODULE_AND_MAIN_CLASS} \
    --dest . \
    --name ${APP_NAME} \
    --vendor "${VENDOR}" \
    --copyright "(C) ${COPYRIGHT_YEARS} ${VENDOR}" \
    --app-version "${VERSION_NO}" \
    --java-options "--enable-preview" \
    --java-options "--enable-native-access=org.cryptomator.jfuse.mac" \
    --java-options "-Xss5m" \
    --java-options "-Xmx256m" \
    --java-options "-Dfile.encoding=\"utf-8\"" \
    --java-options "-Djava.net.useSystemProxies=true" \
    --java-options "-Dapple.awt.enableTemplateImages=true" \
    --java-options "-Dsun.java2d.metal=true" \
    --java-options "-Dcryptomator.appVersion=\"${VERSION_NO}\"" \
    --java-options "-Dcryptomator.logDir=\"@{userhome}/Library/Logs/${APP_NAME}\"" \
    --java-options "-Dcryptomator.pluginDir=\"@{userhome}/Library/Application Support/${APP_NAME}/Plugins\"" \
    --java-options "-Dcryptomator.settingsPath=\"@{userhome}/Library/Application Support/${APP_NAME}/settings.json\"" \
    --java-options "-Dcryptomator.ipcSocketPath=\"@{userhome}/Library/Application Support/${APP_NAME}/ipc.socket\"" \
    --java-options "-Dcryptomator.p12Path=\"@{userhome}/Library/Application Support/${APP_NAME}/key.p12\"" \
    --java-options "-Dcryptomator.integrationsMac.keychainServiceName=\"${APP_NAME}\"" \
    --java-options "-Dcryptomator.mountPointsDir=\"@{userhome}/Library/Application Support${APP_NAME}/mnt\"" \
    --java-options "-Dcryptomator.showTrayIcon=true" \
    --java-options "-Dcryptomator.buildNumber=\"dmg-${REVISION_NO}\"" \
    --mac-package-identifier ${PACKAGE_IDENTIFIER} \
    --resource-dir ../resources

# transform app dir
cp ../resources/${APP_NAME}-Vault.icns ${APP_NAME}.app/Contents/Resources/
sed -i '' "s|###BUNDLE_SHORT_VERSION_STRING###|${VERSION_NO}|g" ${APP_NAME}.app/Contents/Info.plist
sed -i '' "s|###BUNDLE_VERSION###|${REVISION_NO}|g" ${APP_NAME}.app/Contents/Info.plist
cp ../embedded.provisionprofile ${APP_NAME}.app/Contents/

# generate license
mvn -B -Djavafx.platform=mac -f../../../pom.xml license:add-third-party \
    -Dlicense.thirdPartyFilename=license.rtf \
    -Dlicense.outputDirectory=dist/mac/dmg/resources \
    -Dlicense.fileTemplate=resources/licenseTemplate.ftl \
    -Dlicense.includedScopes=compile \
    -Dlicense.excludedGroups=^org\.cryptomator \
    -Dlicense.failOnMissing=true \
    -Dlicense.licenseMergesUrl=file://$(pwd)/../../../license/merges

# codesign
if [ -n "${CODESIGN_IDENTITY}" ] && [ -n "${TEAM_IDENTIFIER}" ]; then
    echo "Codesigning jdk files..."
    find ${APP_NAME}.app/Contents/runtime/Contents/Home/lib/ -name '*.dylib' -exec codesign --force -s ${CODESIGN_IDENTITY} {} \;
    find ${APP_NAME}.app/Contents/runtime/Contents/Home/lib/ -name 'jspawnhelper' -exec codesign --force -o runtime -s ${CODESIGN_IDENTITY} {} \;
    echo "Codesigning jar contents..."
    find ${APP_NAME}.app/Contents/runtime/Contents/MacOS -name '*.dylib' -exec codesign --force -s ${CODESIGN_IDENTITY} {} \;
    for JAR_PATH in `find ${APP_NAME}.app -name "*.jar"`; do
    if [[ `unzip -l ${JAR_PATH} | grep '.dylib\|.jnilib'` ]]; then
        JAR_FILENAME=$(basename ${JAR_PATH})
        OUTPUT_PATH=${JAR_PATH%.*}
        echo "Codesigning libs in ${JAR_FILENAME}..."
        unzip -q ${JAR_PATH} -d ${OUTPUT_PATH}
        find ${OUTPUT_PATH} -name '*.dylib' -exec codesign --force -s ${CODESIGN_IDENTITY} {} \;
        find ${OUTPUT_PATH} -name '*.jnilib' -exec codesign --force -s ${CODESIGN_IDENTITY} {} \;
        rm ${JAR_PATH}
        pushd ${OUTPUT_PATH} > /dev/null
        zip -qr ../${JAR_FILENAME} *
        popd > /dev/null
        rm -r ${OUTPUT_PATH}
    fi
    done
    echo "Codesigning ${APP_NAME}.app..."
    cp ../${APP_NAME}.entitlements .
    sed -i '' "s|###APP_IDENTIFIER_PREFIX###|${TEAM_IDENTIFIER}.|g" ${APP_NAME}.entitlements
    sed -i '' "s|###TEAM_IDENTIFIER###|${TEAM_IDENTIFIER}|g" ${APP_NAME}.entitlements
    codesign --force --deep --entitlements ${APP_NAME}.entitlements -o runtime -s ${CODESIGN_IDENTITY} ${APP_NAME}.app
fi

# prepare dmg contents
mkdir dmg
mv ${APP_NAME}.app dmg
cp resources/${FUSE_LIB}.webloc dmg

# create dmg
create-dmg \
    --volname ${APP_NAME} \
    --volicon "resources/${APP_NAME}-Volume.icns" \
    --background "resources/${APP_NAME}-${FUSE_LIB}-background.tiff" \
    --window-pos 400 100 \
    --window-size 640 694 \
    --icon-size 128 \
    --icon "${APP_NAME}.app" 128 245 \
    --hide-extension "${APP_NAME}.app" \
    --icon "${FUSE_LIB}.webloc" 320 501 \
    --hide-extension "${FUSE_LIB}.webloc" \
    --app-drop-link 512 245 \
    --eula "resources/license.rtf" \
    --icon ".background" 128 758 \
    --icon ".VolumeIcon.icns" 512 758 \
    ${APP_NAME}-${VERSION_NO}.dmg dmg
