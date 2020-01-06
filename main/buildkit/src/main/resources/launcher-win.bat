@echo off
java ^
	-cp "libs/*;win-libs/*" ^
	-Dcryptomator.settingsPath="~/AppData/Roaming/Cryptomator/settings.json" ^
	-Dcryptomator.ipcPortPath="~/AppData/Roaming/Cryptomator/ipcPort.bin" ^
	-Dcryptomator.logDir="~/AppData/Roaming/Cryptomator" ^
	-Dcryptomator.keychainPath="~/AppData/Roaming/Cryptomator/keychain.json" ^
	-Xss20m ^
	-Xmx512m ^
	org.cryptomator.launcher.Cryptomator