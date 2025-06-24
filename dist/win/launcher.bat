@echo off
java ^
	-p "mods" ^
	-cp "libs/*" ^
	-Dcryptomator.settingsPath="~/AppData/Roaming/Cryptomator/settings.json" ^
	-Dcryptomator.ipcSocketPath="~/AppData/Roaming/Cryptomator/ipc.socket" ^
	-Dcryptomator.logDir="~/AppData/Roaming/Cryptomator" ^
	-Dcryptomator.mountPointsDir="~/Cryptomator" ^
	-Dcryptomator.integrationsWin.keychainPaths="~/AppData/Roaming/Cryptomator/keychain.json" ^
	-Dcryptomator.integrationsWin.windowsHelloKeychainPaths="~/AppData/Roaming/Cryptomator/windowsHelloKeychain.json" ^
	-Xss20m ^
	-Xmx512m ^
	--enable-preview `
	--enable-native-access=org.cryptomator.jfuse.win `
	-m org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator
