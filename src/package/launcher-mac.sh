#!/bin/sh
cd $(dirname $0)
java \
	-cp "libs/*" \
	-Dcryptomator.settingsPath="~/Library/Application Support/Cryptomator/settings.json" \
	-Dcryptomator.ipcSocketPath="~/Library/Application Support/Cryptomator/ipc.socket" \
	-Dcryptomator.logDir="~/Library/Logs/Cryptomator" \
	-Dcryptomator.mountPointsDir="/Volumes" \
	-Xss20m \
	-Xmx512m \
	org.cryptomator.launcher.Cryptomator