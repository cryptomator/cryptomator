#!/bin/sh
cd $(dirname $0)
java \
	-p "mods" \
	-cp "libs/*" \
	-Dcryptomator.settingsPath="~/.config/Cryptomator/settings.json" \
	-Dcryptomator.ipcSocketPath="~/.config/Cryptomator/ipc.socket" \
	-Dcryptomator.logDir="~/.local/share/Cryptomator/logs" \
	-Dcryptomator.mountPointsDir="~/.local/share/Cryptomator/mnt" \
	-Djdk.gtk.version=2 \
	-Xss2m \
	-Xmx512m \
	--enable-preview \
	--enable-native-access=org.cryptomator.jfuse.linux.amd64,org.cryptomator.jfuse.linux.aarch64 \
	-m org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator
