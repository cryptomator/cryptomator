Param(
	[Parameter(Mandatory, HelpMessage="Please provide a name for the app")][string] $AppName,
	[Parameter(Mandatory, HelpMessage="Please provide the name of the vendor")][string] $Vendor,
	[Parameter(Mandatory, HelpMessage="Please provide the starting year for the copyright notice")][int] $CopyrightStartYear,
	[Parameter(Mandatory, HelpMessage="Please provide a help url")][string] $HelpUrl,
	[Parameter(Mandatory, HelpMessage="Please provide an update url")][string] $UpdateUrl,
	[Parameter(Mandatory, HelpMessage="Please provide an about url")][string] $AboutUrl,
	[bool] $clean
)

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

$copyright = "(C) $CopyrightStartYear - $((Get-Date).Year) $Vendor"

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
	--strip-native-commands `
	--no-header-files `
	--no-man-pages `
	--strip-debug `
	--compress=1

$appPath = ".\$AppName"
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
	--name $AppName `
	--vendor $Vendor `
	--copyright $copyright `
	--java-options "-Xss5m" `
	--java-options "-Xmx256m" `
	--java-options "-Dcryptomator.appVersion=`"$semVerNo`"" `
	--app-version "$semVerNo.$revisionNo" `
	--java-options "-Dfile.encoding=`"utf-8`"" `
	--java-options "-Dcryptomator.logDir=`"~/AppData/Roaming/$AppName`"" `
	--java-options "-Dcryptomator.pluginDir=`"~/AppData/Roaming/$AppName/Plugins`"" `
	--java-options "-Dcryptomator.settingsPath=`"~/AppData/Roaming/$AppName/settings.json`"" `
	--java-options "-Dcryptomator.ipcSocketPath=`"~/AppData/Roaming/$AppName/ipc.socket`"" `
	--java-options "-Dcryptomator.keychainPath=`"~/AppData/Roaming/$AppName/keychain.json`"" `
	--java-options "-Dcryptomator.mountPointsDir=`"~/$AppName`"" `
	--java-options "-Dcryptomator.integrationsWin.autoStartShellLinkName=`"$AppName`"" `
	--java-options "-Dcryptomator.showTrayIcon=true" `
	--java-options "-Dcryptomator.buildNumber=`"msi-$revisionNo`"" `
	--resource-dir resources `
	--icon resources/$AppName.ico

#Create RTF license for msi
&mvn -B -f $buildDir/../../pom.xml license:add-third-party `
 "-Dlicense.thirdPartyFilename=license.rtf" `
 "-Dlicense.fileTemplate=$buildDir\resources\licenseTemplate.ftl" `
 "-Dlicense.outputDirectory=$buildDir\resources\" `
 "-Dlicense.includedScopes=compile" `
 "-Dlicense.excludedGroups=^org\.cryptomator" `
 "-Dlicense.failOnMissing=true" `
 "-Dlicense.licenseMergesUrl=file:///$buildDir/../../license/merges"

# patch app dir
Copy-Item "contrib\*" -Destination "$AppName"
attrib -r "$AppName\$AppName.exe"

# create .msi
$Env:JP_WIXWIZARD_RESOURCES = "$buildDir\resources"
& "$Env:JAVA_HOME\bin\jpackage" `
	--verbose `
	--type msi `
	--win-upgrade-uuid bda45523-42b1-4cae-9354-a45475ed4775 `
	--app-image $AppName `
	--dest installer `
	--name $AppName `
	--vendor $Vendor `
	--copyright $copyright `
	--app-version "$semVerNo" `
	--win-menu `
	--win-dir-chooser `
	--win-shortcut-prompt `
	--win-menu-group $AppName `
	--resource-dir resources `
	--license-file resources/license.rtf `
	--win-update-url $UpdateUrl `
	--about-url $AboutUrl `
	--file-associations resources/FAvaultFile.properties

#Create RTF license for bundle
&mvn -B -f $buildDir/../../pom.xml license:add-third-party `
 "-Dlicense.thirdPartyFilename=license.rtf" `
 "-Dlicense.fileTemplate=$buildDir\bundle\resources\licenseTemplate.ftl" `
 "-Dlicense.outputDirectory=$buildDir\bundle\resources\" `
 "-Dlicense.includedScopes=compile" `
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
Copy-Item ".\installer\$AppName-*.msi" -Destination ".\bundle\resources\$AppName.msi"

# create bundle including winfsp
& "$env:WIX\bin\candle.exe" .\bundle\bundleWithWinfsp.wxs -ext WixBalExtension -out bundle\ `
	-dBundleVersion="$semVerNo.$revisionNo" `
	-dBundleVendor="$Vendor" `
	-dBundleCopyright="$copyright" `
	-dAboutUrl="$AboutUrl" `
	-dHelpUrl="$HelpUrl" `
	-dUpdateUrl="$UpdateUrl"
& "$env:WIX\bin\light.exe" -b . .\bundle\BundlewithWinfsp.wixobj -ext WixBalExtension -out installer\$AppName-Installer.exe