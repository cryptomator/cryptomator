# check preconditions
if ((Get-Command "git" -ErrorAction SilentlyContinue) -eq $null) 
{ 
   Write-Host "Unable to find git.exe in your PATH (try: choco install git)"
   exit 1
}
if ((Get-Command "mvn" -ErrorAction SilentlyContinue) -eq $null) 
{ 
   Write-Host "Unable to find mvn.cmd in your PATH (try: choco install maven)"
   exit 1
}

$buildDir = Split-Path -Parent $PSCommandPath
$version = $(mvn -f $buildDir/../../pom.xml help:evaluate -Dexpression="project.version" -q -DforceStdout)
$semVerNo = $version -replace '(\d\.\d\.\d).*','$1'
$revisionNo = $(git rev-list --count HEAD)

Write-Output "`$version=$version"
Write-Output "`$semVerNo=$semVerNo"
Write-Output "`$revisionNo=$revisionNo"
Write-Output "`$buildDir=$buildDir"
Write-Output "`$Env:JAVA_HOME=$Env:JAVA_HOME"

# compile
&mvn -B -f $buildDir/../../pom.xml clean package -DskipTests -Pwin
Copy-Item "$buildDir\..\..\target\cryptomator-*.jar" -Destination "$buildDir\..\..\target\mods"

# add runtime
& "$Env:JAVA_HOME\bin\jlink" `
	--verbose `
	--output runtime `
	--module-path "$Env:JAVA_HOME/jmods" `
	--add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.crypto.ec,jdk.accessibility,jdk.management.jfr `
	--no-header-files `
	--no-man-pages `
	--strip-debug `
	--compress=1

# create app dir
& "$Env:JAVA_HOME\bin\jpackage" `
	--verbose `
	--type app-image `
	--runtime-image runtime `
	--input ../../target/libs `
	--module-path ../../target/mods `
	--module org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator `
	--dest . `
	--name Cryptomator `
	--vendor "Skymatic GmbH" `
	--copyright "(C) 2016 - 2022 Skymatic GmbH" `
	--java-options "-Xss5m" `
	--java-options "-Xmx256m" `
	--java-options "-Dcryptomator.appVersion=`"$semVerNo`"" `
	--app-version "$semVerNo.$revisionNo" `
	--java-options "-Dfile.encoding=`"utf-8`"" `
	--java-options "-Dcryptomator.logDir=`"~/AppData/Roaming/Cryptomator`"" `
	--java-options "-Dcryptomator.pluginDir=`"~/AppData/Roaming/Cryptomator/Plugins`"" `
	--java-options "-Dcryptomator.settingsPath=`"~/AppData/Roaming/Cryptomator/settings.json`"" `
	--java-options "-Dcryptomator.ipcSocketPath=`"~/AppData/Roaming/Cryptomator/ipc.socket`"" `
	--java-options "-Dcryptomator.keychainPath=`"~/AppData/Roaming/Cryptomator/keychain.json`"" `
	--java-options "-Dcryptomator.mountPointsDir=`"~/Cryptomator`"" `
	--java-options "-Dcryptomator.showTrayIcon=true" `
	--java-options "-Dcryptomator.buildNumber=`"msi-$revisionNo`"" `
	--resource-dir resources `
	--icon resources/Cryptomator.ico

# patch app dir
Copy-Item "contrib\*" -Destination "Cryptomator"
attrib -r "Cryptomator\Cryptomator.exe"

# create .msi bundle
$Env:JP_WIXWIZARD_RESOURCES = "$buildDir\resources"
& "$Env:JAVA_HOME\bin\jpackage" `
	--verbose `
	--type msi `
	--win-upgrade-uuid bda45523-42b1-4cae-9354-a45475ed4775 `
	--app-image Cryptomator `
	--dest installer `
	--name Cryptomator `
	--vendor "Skymatic GmbH" `
	--copyright "(C) 2016 - 2022 Skymatic GmbH" `
	--app-version "$semVerNo" `
	--win-menu `
	--win-dir-chooser `
	--win-shortcut-prompt `
	--win-update-url "https:\\cryptomator.org" `
	--win-menu-group Cryptomator `
	--resource-dir resources `
	--license-file resources/license.rtf `
	--file-associations resources/FAvaultFile.properties
