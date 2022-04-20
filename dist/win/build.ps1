# check parameters
$clean = $args[0] -eq "fresh"

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

$vendor = "Skymatic GmbH"
$copyright = "(C) 2016 - 2022 Skymatic GmbH"

# compile
&mvn -B -f $buildDir/../../pom.xml clean package -DskipTests -Pwin
Copy-Item "$buildDir\..\..\target\cryptomator-*.jar" -Destination "$buildDir\..\..\target\mods"

# add runtime
$runtimeImagePath = '.\runtime'
if ($clean -and (Test-Path -Path $runtimeImagePath)) {
	Remove-Item -Path $runtimeImagePath -Force -Recurse
}

& "$Env:JAVA_HOME\bin\jlink" `
	--verbose `
	--output runtime `
	--module-path "$Env:JAVA_HOME/jmods" `
	--add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.crypto.ec,jdk.accessibility,jdk.management.jfr `
	--no-header-files `
	--no-man-pages `
	--strip-debug `
	--compress=1

$appPath = '.\Cryptomator'
if ($clean -and (Test-Path -Path $appPath)) {
	Remove-Item -Path $appPath -Force -Recurse
}

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
	--vendor $vendor `
	--copyright $copyright `
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

#Create RTF license for msi
&mvn -B -f $buildDir/../../pom.xml license:add-third-party `
 "-Dlicense.thirdPartyFilename=license.rtf" `
 "-Dlicense.fileTemplate=$buildDir\resources\licenseTemplate.ftl" `
 "-Dlicense.outputDirectory=$buildDir\resources\" `
 "-Dlicense.includeScopes=compile" `
 "-Dlicense.excludedGroups=^org\.cryptomator" `
 "-Dlicense.failOnMissing=true" `
 "-Dlicense.licenseMergesUrl=file:///$buildDir/../../license/merges"

# patch app dir
Copy-Item "contrib\*" -Destination "Cryptomator"
attrib -r "Cryptomator\Cryptomator.exe"

$aboutUrl="https://cryptomator.org"
$updateUrl="https://cryptomator.org/downloads/"
$helpUrl="https://cryptomator.org/contact/"

# create .msi
$Env:JP_WIXWIZARD_RESOURCES = "$buildDir\resources"
& "$Env:JAVA_HOME\bin\jpackage" `
	--verbose `
	--type msi `
	--win-upgrade-uuid bda45523-42b1-4cae-9354-a45475ed4775 `
	--app-image Cryptomator `
	--dest installer `
	--name Cryptomator `
	--vendor $vendor `
	--copyright $copyright `
	--app-version "$semVerNo" `
	--win-menu `
	--win-dir-chooser `
	--win-shortcut-prompt `
	--win-update-url $updateUrl `
	--win-menu-group Cryptomator `
	--resource-dir resources `
	--about-url $aboutUrl `
	--license-file resources/license.rtf `
	--file-associations resources/FAvaultFile.properties

#Create RTF license for bundle
&mvn -B -f $buildDir/../../pom.xml license:add-third-party `
 "-Dlicense.thirdPartyFilename=license.rtf" `
 "-Dlicense.fileTemplate=$buildDir\bundle\resources\licenseTemplate.ftl" `
 "-Dlicense.outputDirectory=$buildDir\bundle\resources\" `
 "-Dlicense.includeScopes=compile" `
 "-Dlicense.excludedGroups=^org\.cryptomator" `
 "-Dlicense.failOnMissing=true" `
 "-Dlicense.licenseMergesUrl=file:///$buildDir/../../license/merges"

# download Winfsp
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue' # disables Invoke-WebRequest's progress bar, which slows down downloads to a few bytes/s
$winfspMsiUrl = "https://github.com/winfsp/winfsp/releases/download/v1.10/winfsp-1.10.22006.msi"
Write-Output "Downloading ${winfspMsiUrl}..."
Invoke-WebRequest $winfspMsiUrl -OutFile ".\bundle\resources\winfsp.msi" # redirects are followed by default

# copy MSI to bundle resources
Copy-Item ".\installer\Cryptomator-*.msi" -Destination ".\bundle\resources\Cryptomator.msi"

# create bundle including winfsp
& "$env:WIX\bin\candle.exe" .\bundle\bundleWithWinfsp.wxs -ext WixBalExtension -out bundle\ `
	-dBundleVersion="$semVerNo.$revisionNo" `
	-dBundleVendor="$vendor" `
	-dBundleCopyright="$copyright" `
	-dAboutUrl="$aboutUrl" `
	-dHelpUrl="$helpUrl" `
	-dUpdateUrl="$updateUrl"
& "$env:WIX\bin\light.exe" -b . .\bundle\BundlewithWinfsp.wixobj -ext WixBalExtension -out installer\Cryptomator-Installer.exe