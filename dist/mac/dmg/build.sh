#!/bin/bash

# parse options
usage() { echo "Usage: $0 [-s <codesign-identity>]" 1>&2; exit 1; }
while getopts ":s:" o; do
    case "${o}" in
        s)
            CODESIGN_IDENTITY=${OPTARG}
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
COPYRIGHT_YEARS="2016 - 2022"
PACKAGE_IDENTIFIER="org.cryptomator"
MAIN_JAR_GLOB="cryptomator-*.jar"
MODULE_AND_MAIN_CLASS="org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator"
REVISION_NO=`git rev-list --count HEAD`
VERSION_NO=`mvn -f../../../pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout | sed -rn 's/.*([0-9]+\.[0-9]+\.[0-9]+).*/\1/p'`

# check preconditions
if [ -z "${JAVA_HOME}" ]; then echo "JAVA_HOME not set. Run using JAVA_HOME=/path/to/jdk ./build.sh"; exit 1; fi
command -v mvn >/dev/null 2>&1 || { echo >&2 "mvn not found. Fix by 'brew install maven'."; exit 1; }
command -v create-dmg >/dev/null 2>&1 || { echo >&2 "create-dmg not found. Fix by 'brew install create-dmg'."; exit 1; }
if [ -n "${CODESIGN_IDENTITY}" ]; then
    command -v codesign >/dev/null 2>&1 || { echo >&2 "codesign not found. Fix by 'xcode-select --install'."; exit 1; }
    if [[ ! `security find-identity -v -p codesigning | grep -w "${CODESIGN_IDENTITY}"` ]]; then echo "Given codesign identity is invalid."; exit 1; fi
fi

# compile
mvn -B -f../../../pom.xml clean package -DskipTests -Pmac
cp ../../../target/${MAIN_JAR_GLOB} ../../../target/mods

# add runtime
${JAVA_HOME}/bin/jlink \
    --output runtime \
    --module-path "${JAVA_HOME}/jmods" \
    --add-modules java.base,java.desktop,java.instrument,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.crypto.ec,jdk.accessibility,jdk.management.jfr \
    --strip-native-commands \
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
    --module ${MODULE_AND_MAIN_CLASS} \
    --dest . \
    --name ${APP_NAME} \
    --vendor "${VENDOR}" \
    --copyright "(C) ${COPYRIGHT_YEARS} ${VENDOR}" \
    --app-version "${VERSION_NO}" \
    --java-options "--enable-preview" \
    --java-options "-Xss5m" \
    --java-options "-Xmx256m" \
    --java-options "-Dfile.encoding=\"utf-8\"" \
    --java-options "-Dapple.awt.enableTemplateImages=true" \
    --java-options "-Dsun.java2d.metal=true" \
    --java-options "-Dcryptomator.appVersion=\"${VERSION_NO}\"" \
    --java-options "-Dcryptomator.logDir=\"~/Library/Logs/${APP_NAME}\"" \
    --java-options "-Dcryptomator.pluginDir=\"~/Library/Application Support/${APP_NAME}/Plugins\"" \
    --java-options "-Dcryptomator.settingsPath=\"~/Library/Application Support/${APP_NAME}/settings.json\"" \
    --java-options "-Dcryptomator.ipcSocketPath=\"~/Library/Application Support/${APP_NAME}/ipc.socket\"" \
    --java-options "-Dcryptomator.p12Path=\"~/Library/Application Support/${APP_NAME}/key.p12\"" \
    --java-options "-Dcryptomator.integrationsMac.keychainServiceName=\"${APP_NAME}\"" \
    --java-options "-Dcryptomator.showTrayIcon=true" \
    --java-options "-Dcryptomator.buildNumber=\"dmg-${REVISION_NO}\"" \
    --mac-package-identifier ${PACKAGE_IDENTIFIER} \
    --resource-dir ../resources

# transform app dir
cp ../resources/${APP_NAME}-Vault.icns ${APP_NAME}.app/Contents/Resources/
sed -i '' "s|###BUNDLE_SHORT_VERSION_STRING###|${VERSION_NO}|g" ${APP_NAME}.app/Contents/Info.plist
sed -i '' "s|###BUNDLE_VERSION###|${REVISION_NO}|g" ${APP_NAME}.app/Contents/Info.plist

# generate license
mvn -B -f../../../pom.xml license:add-third-party \
    -Dlicense.thirdPartyFilename=license.rtf \
    -Dlicense.outputDirectory=dist/mac/dmg/resources \
    -Dlicense.fileTemplate=resources/licenseTemplate.ftl \
    -Dlicense.includedScopes=compile \
    -Dlicense.excludedGroups=^org\.cryptomator \
    -Dlicense.failOnMissing=true \
    -Dlicense.licenseMergesUrl=file://$(pwd)/../../../license/merges

# codesign
if [ -n "${CODESIGN_IDENTITY}" ]; then
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
    codesign --force --deep --entitlements ../${APP_NAME}.entitlements -o runtime -s ${CODESIGN_IDENTITY} ${APP_NAME}.app
fi

# prepare dmg contents
mkdir dmg
mv ${APP_NAME}.app dmg
cp resources/macFUSE.webloc dmg

# create dmg
create-dmg \
    --volname ${APP_NAME} \
    --volicon "resources/${APP_NAME}-Volume.icns" \
    --background "resources/${APP_NAME}-background.tiff" \
    --window-pos 400 100 \
    --window-size 640 694 \
    --icon-size 128 \
    --icon "${APP_NAME}.app" 128 245 \
    --hide-extension "${APP_NAME}.app" \
    --icon "macFUSE.webloc" 320 501 \
    --hide-extension "macFUSE.webloc" \
    --app-drop-link 512 245 \
    --eula "resources/license.rtf" \
    --icon ".background" 128 758 \
    --icon ".fseventsd" 320 758 \
    --icon ".VolumeIcon.icns" 512 758 \
    ${APP_NAME}-${VERSION_NO}.dmg dmg
