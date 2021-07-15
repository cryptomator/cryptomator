#!/bin/sh
cd $(dirname $0)
java \
	-p "mods" \
	-cp "libs/*" \
	-Dcryptomator.settingsPath="~/Library/Application Support/Cryptomator/settings.json" \
	-Dcryptomator.ipcSocketPath="~/Library/Application Support/Cryptomator/ipc.socket" \
	-Dcryptomator.logDir="~/Library/Logs/Cryptomator" \
	-Dcryptomator.mountPointsDir="/Volumes" \
	-Xss20m \
	-Xmx512m \
	-m org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator