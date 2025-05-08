Param(
	[Parameter(Mandatory, HelpMessage="Please provide a name for the app")][string] $AppName,
	[Parameter(Mandatory, HelpMessage="Please provide the glob pattern to identify the main jar")][string] $MainJarGlob,
	[Parameter(Mandatory, HelpMessage="Please provide the module- and main class path to start the app")][string] $ModuleAndMainClass,
	[Parameter(Mandatory, HelpMessage="Please provide the windows upgrade uuid for the installer")][string] $UpgradeUUID,
	[Parameter(Mandatory, HelpMessage="Please provide the name of the vendor")][string] $Vendor,
	[Parameter(Mandatory, HelpMessage="Please provide the starting year for the copyright notice")][int] $CopyrightStartYear,
	[Parameter(Mandatory, HelpMessage="Please provide a help url")][string] $HelpUrl,
	[Parameter(Mandatory, HelpMessage="Please provide an update url")][string] $UpdateUrl,
	[Parameter(Mandatory, HelpMessage="Please provide an about url")][string] $AboutUrl,
	[Parameter(Mandatory, HelpMessage="Please provide an alias for localhost")][string] $LoopbackAlias,
	[bool] $clean
)

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue' # disables Invoke-WebRequest's progress bar, which slows down downloads to a few bytes/s

# check preconditions
if ((Get-Command "git" -ErrorAction SilentlyContinue) -eq $null)
{
   Write-Error "Unable to find git.exe in your PATH (try: choco install git)"
   exit 1
}
if ((Get-Command "mvn" -ErrorAction SilentlyContinue) -eq $null)
{
   Write-Error "Unable to find mvn.cmd in your PATH (try: choco install maven)"
   exit 1
}
if ((Get-Command 'wix' -ErrorAction SilentlyContinue) -eq $null)
{
   Write-Error 'Unable to find wix in your PATH (try: dotnet tool install --global wix --version 6.0.0)'
   exit 1
}
$wixExtensions = & wix.exe extension list --global | Out-String
if ($wixExtensions -notmatch 'WixToolset.UI.wixext') {
    Write-Error 'UI wix extension missing. Please install it with: wix.exe extension add WixToolset.UI.wixext/6.0.0 --global)'
    exit 1
}
if ($wixExtensions -notmatch 'WixToolset.Util.wixext') {
    Write-Error 'Util wix extension missing. Please install it with: wix.exe extension add WixToolset.Util.wixext/6.0.0 --global)'
    exit 1
}
if ($wixExtensions -notmatch 'WixToolset.BootstrapperApplications.wixext') {
    Write-Error 'Util wix extension missing. Please install it with: wix.exe extension add WixToolset.BootstrapperApplications.wixext/6.0.0 --global)'
    exit 1
}

$buildDir = Split-Path -Parent $PSCommandPath
$version = $(mvn -f $buildDir/../../pom.xml help:evaluate -Dexpression="project.version" -q -DforceStdout)
$semVerNo = $version -replace '(\d+\.\d+\.\d+).*','$1'
$revisionNo = $(git rev-list --count HEAD)

Write-Output "`$version=$version"
Write-Output "`$semVerNo=$semVerNo"
Write-Output "`$revisionNo=$revisionNo"
Write-Output "`$buildDir=$buildDir"
Write-Output "`$Env:JAVA_HOME=$Env:JAVA_HOME"

$copyright = "(C) $CopyrightStartYear - $((Get-Date).Year) $Vendor"

# compile
&mvn -B -f $buildDir/../../pom.xml clean package -DskipTests -Pwin "-Djavafx.platform=win"
Copy-Item "$buildDir\..\..\target\$MainJarGlob.jar" -Destination "$buildDir\..\..\target\mods"

# add runtime
$runtimeImagePath = '.\runtime'
if ($clean -and (Test-Path -Path $runtimeImagePath)) {
	Remove-Item -Path $runtimeImagePath -Force -Recurse
}

## download jfx jmods
$javaFxVersion='23.0.2'
$javaFxJmodsUrl = "https://download2.gluonhq.com/openjfx/${javaFxVersion}/openjfx-${javaFxVersion}_windows-x64_bin-jmods.zip"
$javaFxJmodsSHA256 = 'ee176dcee3bd78bde7910735bd67f67c792882f5b89626796ae06f7a1c0119d3'
$javaFxJmods = '.\resources\jfxJmods.zip'
if( !(Test-Path -Path $javaFxJmods) ) {
	Write-Output "Downloading ${javaFxJmodsUrl}..."
	Invoke-WebRequest $javaFxJmodsUrl -OutFile $javaFxJmods # redirects are followed by default
}

$jmodsChecksumActual = $(Get-FileHash -Path $javaFxJmods -Algorithm SHA256).Hash.ToLower()
if( $jmodsChecksumActual -ne $javaFxJmodsSHA256 ) {
	Write-Error "Checksum mismatch for jfxJmods.zip. Expected: $javaFxJmodsSHA256
, actual: $jmodsChecksumActual"
	exit 1;
}
Expand-Archive -Path $javaFxJmods -Force -DestinationPath ".\resources\"
Remove-Item -Recurse -Force -Path ".\resources\javafx-jmods" -ErrorAction Ignore
Move-Item -Force -Path ".\resources\javafx-jmods-*" -Destination ".\resources\javafx-jmods" -ErrorAction Stop

## create custom runtime
### check for JEP 493
$jmodPaths="$buildDir/resources/javafx-jmods";
if ((& "$Env:JAVA_HOME\bin\jlink" --help | Select-String -Pattern "Linking from run-time image enabled" -SimpleMatch | Measure-Object).Count -gt 0 ) {
	$jmodPaths="$Env:JAVA_HOME/jmods;" + $jmodPaths;
}
### create runtime
& "$Env:JAVA_HOME\bin\jlink" `
	--verbose `
	--output runtime `
	--module-path $jmodPaths `
	--add-modules java.base,java.desktop,java.instrument,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.accessibility,jdk.management.jfr,jdk.crypto.mscapi,java.compiler,javafx.base,javafx.graphics,javafx.controls,javafx.fxml `
	--strip-native-commands `
	--no-header-files `
	--no-man-pages `
	--strip-debug `
	--compress "zip-0" #do not compress and use msi compression

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
	--module $ModuleAndMainClass `
	--dest . `
	--name $AppName `
	--vendor $Vendor `
	--copyright $copyright `
	--java-options "--enable-preview" `
	--java-options "--enable-native-access=org.cryptomator.jfuse.win,org.cryptomator.integrations.win" `
	--java-options "-Xss5m" `
	--java-options "-Xmx256m" `
	--java-options "-Dcryptomator.appVersion=`"$semVerNo`"" `
	--app-version "$semVerNo.$revisionNo" `
	--java-options "-Dfile.encoding=`"utf-8`"" `
	--java-options "-Djava.net.useSystemProxies=true" `
	--java-options "-Dcryptomator.logDir=`"@{localappdata}/$AppName`"" `
	--java-options "-Dcryptomator.pluginDir=`"@{appdata}/$AppName/Plugins`"" `
	--java-options "-Dcryptomator.settingsPath=`"@{appdata}/$AppName/settings.json;@{userhome}/AppData/Roaming/$AppName/settings.json`"" `
	--java-options "-Dcryptomator.ipcSocketPath=`"@{localappdata}/$AppName/ipc.socket`"" `
	--java-options "-Dcryptomator.p12Path=`"@{appdata}/$AppName/key.p12;@{userhome}/AppData/Roaming/$AppName/key.p12`"" `
	--java-options "-Dcryptomator.mountPointsDir=`"@{userhome}/$AppName`"" `
	--java-options "-Dcryptomator.loopbackAlias=`"$LoopbackAlias`"" `
	--java-options "-Dcryptomator.integrationsWin.autoStartShellLinkName=`"$AppName`"" `
	--java-options "-Dcryptomator.integrationsWin.keychainPaths=`"@{appdata}/$AppName/keychain.json;@{userhome}/AppData/Roaming/$AppName/keychain.json`"" `
	--java-options "-Dcryptomator.showTrayIcon=true" `
	--java-options "-Dcryptomator.buildNumber=`"msi-$revisionNo`"" `
	--resource-dir resources `
	--icon resources/$AppName.ico

#Create RTF license for msi
&mvn -B -f $buildDir/../../pom.xml license:add-third-party "-Djavafx.platform=win" `
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
# patch batch script to set hostfile
$webDAVPatcher = "$AppName\patchWebDAV.bat"
try {
	(Get-Content $webDAVPatcher ) -replace '::REPLACE ME', "SET LOOPBACK_ALIAS=`"$LoopbackAlias`"" | Set-Content $webDAVPatcher
} catch {
   Write-Host "Failed to set LOOPBACK_ALIAS for patchWebDAV.bat"
   exit 1
}

# create .msi
$Env:JP_WIXWIZARD_RESOURCES = "$buildDir\resources"
$Env:JP_WIXHELPER_DIR = "."
& "$Env:JAVA_HOME\bin\jpackage" `
	--verbose `
	--type msi `
	--win-upgrade-uuid $UpgradeUUID `
	--app-image $AppName `
	--dest installer `
	--name $AppName `
	--vendor $Vendor `
	--copyright $copyright `
	--app-version "$semVerNo.$revisionNo" `
	--win-menu `
	--win-dir-chooser `
	--win-shortcut-prompt `
	--win-menu-group $AppName `
	--resource-dir resources `
	--license-file resources/license.rtf `
	--win-update-url $UpdateUrl `
	--about-url $AboutUrl `
	--file-associations resources/FAvaultFile.properties

if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage MSI failed with exit code $LASTEXITCODE"
	return 1;
}


#Create RTF license for bundle
&mvn -B -f $buildDir/../../pom.xml license:add-third-party "-Djavafx.platform=win" `
 "-Dlicense.thirdPartyFilename=license.rtf" `
 "-Dlicense.fileTemplate=$buildDir\bundle\resources\licenseTemplate.ftl" `
 "-Dlicense.outputDirectory=$buildDir\bundle\resources\" `
 "-Dlicense.includedScopes=compile" `
 "-Dlicense.excludedGroups=^org\.cryptomator" `
 "-Dlicense.failOnMissing=true" `
 "-Dlicense.licenseMergesUrl=file:///$buildDir/../../license/merges"

# download Winfsp
$winfspMsiUrl= 'https://github.com/winfsp/winfsp/releases/download/v2.0/winfsp-2.0.23075.msi'
Write-Output "Downloading ${winfspMsiUrl}..."
Invoke-WebRequest $winfspMsiUrl -OutFile ".\bundle\resources\winfsp.msi" # redirects are followed by default

# download legacy-winfsp uninstaller
$winfspUninstaller= 'https://github.com/cryptomator/winfsp-uninstaller/releases/latest/download/winfsp-uninstaller.exe'
Write-Output "Downloading ${winfspUninstaller}..."
Invoke-WebRequest $winfspUninstaller -OutFile ".\bundle\resources\winfsp-uninstaller.exe" # redirects are followed by default

# copy MSI to bundle resources
Copy-Item ".\installer\$AppName-*.msi" -Destination ".\bundle\resources\$AppName.msi" -Force

# create bundle including winfsp
& wix build `
	-define BundleName="$AppName" `
	-define BundleVersion="$semVerNo.$revisionNo" `
	-define BundleVendor="$Vendor" `
	-define BundleCopyright="$copyright" `
	-define AboutUrl="$AboutUrl" `
	-define HelpUrl="$HelpUrl" `
	-define UpdateUrl="$UpdateUrl" `
	-ext "WixToolset.Util.wixext" `
	-ext "WixToolset.BootstrapperApplications.wixext" `
    .\bundle\bundleWithWinfsp.wxs `
    -out "installer\$AppName-Installer.exe"