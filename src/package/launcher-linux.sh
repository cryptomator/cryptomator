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
	-m org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator
