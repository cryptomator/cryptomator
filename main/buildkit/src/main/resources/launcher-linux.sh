#!/bin/sh
java \
	-cp "libs/*;linux-libs/*" \
	-Dcryptomator.settingsPath="~/.config/Cryptomator/settings.json" \
	-Dcryptomator.ipcPortPath="~/.config/Cryptomator/ipcPort.bin" \
	-Dcryptomator.logDir="~/.local/share/Cryptomator/logs" \
	-Dcryptomator.mountPointsDir="~/.local/share/Cryptomator/mnt" \
	-Djdk.gtk.version=2 \
	-Xss20m \
	-Xmx512m \
	org.cryptomator.launcher.Cryptomator