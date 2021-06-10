#!/bin/sh
cd $(dirname $0)
java \
	-p "libs" \
	-Dcryptomator.settingsPath="~/Library/Application Support/Cryptomator/settings.json" \
	-Dcryptomator.ipcPortPath="~/Library/Application Support/Cryptomator/ipcPort.bin" \
	-Dcryptomator.logDir="~/Library/Logs/Cryptomator" \
	-Dcryptomator.mountPointsDir="/Volumes" \
	-Xss20m \
	-Xmx512m \
	-m org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator