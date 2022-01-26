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

# prepare working dir and variables
cd $(dirname $0)
rm -rf runtime dmg
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
    --java-options "-Dcryptomator.appVersion=\"${VERSION_NO}\"" \
    --app-version "${VERSION_NO}" \
    --java-options "-Dfile.encoding=\"utf-8\"" \
    --java-options "-Dapple.awt.enableTemplateImages=true" \
    --java-options "-Dcryptomator.logDir=\"~/Library/Logs/Cryptomator\"" \
    --java-options "-Dcryptomator.pluginDir=\"~/Library/Application Support/Cryptomator/Plugins\"" \
    --java-options "-Dcryptomator.settingsPath=\"~/Library/Application Support/Cryptomator/settings.json\"" \
    --java-options "-Dcryptomator.ipcSocketPath=\"~/Library/Application Support/Cryptomator/ipc.socket\"" \
    --java-options "-Dcryptomator.showTrayIcon=true" \
    --java-options "-Dcryptomator.buildNumber=\"dmg-${REVISION_NO}\"" \
    --mac-package-identifier org.cryptomator \
    --resource-dir ../resources

# transform app dir
cp ../resources/Cryptomator-Vault.icns Cryptomator.app/Contents/Resources/
sed -i '' "s|###BUNDLE_SHORT_VERSION_STRING###|${VERSION_NO}|g" Cryptomator.app/Contents/Info.plist
sed -i '' "s|###BUNDLE_VERSION###|${REVISION_NO}|g" Cryptomator.app/Contents/Info.plist

# codesign
if [ -n "${CODESIGN_IDENTITY}" ]; then
    find Cryptomator.app/Contents/runtime/Contents/MacOS -name '*.dylib' -exec codesign --force -s ${CODESIGN_IDENTITY} {} \;
    for JAR_PATH in `find Cryptomator.app -name "*.jar"`; do
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
    echo "Codesigning Cryptomator.app..."
    codesign --force --deep --entitlements ../Cryptomator.entitlements -o runtime -s ${CODESIGN_IDENTITY} Cryptomator.app
fi

# prepare dmg contents
mkdir dmg
mv Cryptomator.app dmg
cp resources/macFUSE.webloc dmg

# create dmg
create-dmg \
    --volname Cryptomator \
    --volicon "resources/Cryptomator-Volume.icns" \
    --background "resources/Cryptomator-background.tiff" \
    --window-pos 400 100 \
    --window-size 640 694 \
    --icon-size 128 \
    --icon "Cryptomator.app" 128 245 \
    --hide-extension "Cryptomator.app" \
    --icon "macFUSE.webloc" 320 501 \
    --hide-extension "macFUSE.webloc" \
    --app-drop-link 512 245 \
    --eula "resources/license.rtf" \
    --icon ".background" 128 758 \
    --icon ".fseventsd" 320 758 \
    --icon ".VolumeIcon.icns" 512 758 \
    Cryptomator-${VERSION_NO}.dmg dmg
