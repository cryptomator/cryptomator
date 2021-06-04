@echo off
java ^
	-cp "libs/*" ^
	-Dcryptomator.settingsPath="~/AppData/Roaming/Cryptomator/settings.json" ^
	-Dcryptomator.ipcPortPath="~/AppData/Roaming/Cryptomator/ipcPort.bin" ^
	-Dcryptomator.logDir="~/AppData/Roaming/Cryptomator" ^
	-Dcryptomator.mountPointsDir="~/Cryptomator" ^
	-Dcryptomator.keychainPath="~/AppData/Roaming/Cryptomator/keychain.json" ^
	-Xss20m ^
	-Xmx512m ^
	org.cryptomator.launcher.Cryptomator