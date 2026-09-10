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
	[ValidateSet('installer', 'portable', 'corp', 'all')][string] $Target = 'installer', # what to build: the msi/exe installer, the portable zip, the per-user corp msi or everything
	[string] $CorpUpgradeUUID = "d98d0145-341a-4684-bdb6-ee480294541a", # upgrade uuid of the per-user corp msi, must differ from the per-machine installer
	[bool] $clean = $false # if true, cleans up previous build artifacts
)

$buildInstaller = ($Target -eq 'installer') -or ($Target -eq 'all')
$buildPortable = ($Target -eq 'portable') -or ($Target -eq 'all')
$buildCorp = ($Target -eq 'corp') -or ($Target -eq 'all')

# ============================
# Function Definitions Section
# ============================

function Invoke-CommandWithExitCheck {
	param (
		[string]$Command,
		[string[]]$Arguments
	)

	& $Command @Arguments
	if ($LASTEXITCODE -ne 0) {
		Write-Error "Command '$Command' failed with exit code $LASTEXITCODE"
		exit $LASTEXITCODE
	}
}

# Java options shared by the per-machine installer and the per-user corp msi.
# The corp msi cannot patch the hosts file, hence it passes an empty LoopbackAliasName.
function New-InstallerJavaOptions {
	param (
		[string]$BuildNumber,
		[bool]$DisableUpdateCheck,
		[string]$LoopbackAliasName
	)
	$options = @(
	"--java-options", "--enable-native-access=javafx.graphics,org.cryptomator.jfuse.win,org.cryptomator.integrations.win"
	"--java-options", "-Xss5m"
	"--java-options", "-Xmx256m"
	"--java-options", "-Dcryptomator.appVersion=`"$semVerNo`""
	"--java-options", "-Dfile.encoding=`"utf-8`""
	"--java-options", "-Djava.net.useSystemProxies=true"
	"--java-options", "-Dcryptomator.logDir=`"@{localappdata}/$AppName`""
	"--java-options", "-XX:ErrorFile=`"C:/cryptomator/cryptomator_crash.log`""
	"--java-options", "-Dcryptomator.adminConfigPath=`"C:/ProgramData/$AppName/config.properties`""
	"--java-options", "-Dcryptomator.settingsPath=`"@{appdata}/$AppName/settings.json;@{userhome}/AppData/Roaming/$AppName/settings.json`""
	"--java-options", "-Dcryptomator.ipcSocketPath=`"@{localappdata}/$AppName/ipc.socket`""
	"--java-options", "-Dcryptomator.p12Path=`"@{appdata}/$AppName/key.p12;@{userhome}/AppData/Roaming/$AppName/key.p12`""
	"--java-options", "-Dcryptomator.mountPointsDir=`"@{userhome}/$AppName`""
	)
	if ($LoopbackAliasName) {
		$options += @("--java-options", "-Dcryptomator.loopbackAlias=`"$LoopbackAliasName`"")
	}
	$options += @(
	"--java-options", "-Dcryptomator.integrationsWin.autoStartShellLinkName=`"$AppName`""
	"--java-options", "-Dcryptomator.integrationsWin.keychainPaths=`"@{appdata}/$AppName/keychain.json;@{userhome}/AppData/Roaming/$AppName/keychain.json`""
	"--java-options", "-Dcryptomator.integrationsWin.windowsHelloKeychainPaths=`"@{appdata}/$AppName/windowsHelloKeychain.json`""
	"--java-options", "-Dcryptomator.showTrayIcon=true"
	"--java-options", "-Dcryptomator.buildNumber=`"$BuildNumber`""
	"--java-options", "-Dcryptomator.disableUpdateCheck=$($DisableUpdateCheck.ToString().ToLower())"
	"--java-options", "-Dcryptomator.hub.enableTrustOnFirstUse=true"
	)
	return $options
}

function Main {

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue' # disables Invoke-WebRequest's progress bar, which slows down downloads to a few bytes/s

# check preconditions
if ((Get-Command "git" -ErrorAction SilentlyContinue) -eq $null)
{
   Write-Error "Unable to find git.exe in your PATH (try: choco install git)"
   exit 1
}
if ($buildInstaller -or $buildCorp) {
	# wix is only required for the msi/exe installer, not for the portable build
	if ((Get-Command 'wix' -ErrorAction SilentlyContinue) -eq $null)
	{
	   Write-Error 'Unable to find wix in your PATH (try: dotnet tool install --global wix --version 6.0.2)'
	   exit 1
	}
	$wixExtensions = & wix.exe extension list --global | Out-String
	if ($wixExtensions -notmatch 'WixToolset.UI.wixext') {
	    Write-Error 'Wix UI extension missing. Please install it with: wix.exe extension add WixToolset.UI.wixext/6.0.2 --global)'
	    exit 1
	}
	if ($wixExtensions -notmatch 'WixToolset.Util.wixext') {
	    Write-Error 'Wix Util extension missing. Please install it with: wix.exe extension add WixToolset.Util.wixext/6.0.2 --global)'
	    exit 1
	}
	if ($buildInstaller -and ($wixExtensions -notmatch 'WixToolset.BootstrapperApplications.wixext')) {
	    Write-Error 'Wix Bootstrapper extension missing. Please install it with: wix.exe extension add WixToolset.BootstrapperApplications.wixext/6.0.2 --global)'
	    exit 1
	}
}

$buildDir = Split-Path -Parent $PSCommandPath
$version = $(../../mvnw.cmd -f $buildDir/../../pom.xml help:evaluate -Dexpression="project.version" -q -DforceStdout)
$semVerNo = $version -replace '(\d+\.\d+\.\d+).*','$1'
$revisionNo = $(git rev-list --count HEAD)

Write-Host "`$version=$version"
Write-Host "`$semVerNo=$semVerNo"
Write-Host "`$revisionNo=$revisionNo"
Write-Host "`$buildDir=$buildDir"
Write-Host "`$Target=$Target"
Write-Host "`$Env:JAVA_HOME=$Env:JAVA_HOME"

$copyright = "(C) $CopyrightStartYear - $((Get-Date).Year) $Vendor"

# compile
Invoke-CommandWithExitCheck -Command `
    "../../mvnw.cmd" -Arguments @("-B", "-f", "$buildDir/../../pom.xml", "clean", "package", "-DskipTests", "-Pwin")
Copy-Item "$buildDir\..\..\target\$MainJarGlob.jar" -Destination "$buildDir\..\..\target\mods"

# add runtime
$runtimeImagePath = '.\runtime'
if ($clean -and (Test-Path -Path $runtimeImagePath)) {
	Remove-Item -Path $runtimeImagePath -Force -Recurse
}

## download jfx jmods for X64, while they are part of the Arm64 JDK
$archCode = (Get-CimInstance Win32_Processor).Architecture
$archName = switch ($archCode) {
    9  { "x64" }
    12 { "ARM64" }
    default { "WMI Win32_Processor.Architecture code ($archCode)" }
}

switch ($archName) {
    'ARM64' {
		$javafxBaseJmod = Join-Path $Env:JAVA_HOME "jmods\javafx.base.jmod"
		if (!(Test-Path $javafxBaseJmod)) {
			Write-Error "JavaFX module not found in JDK. Please ensure a JDK with JavaFX (including jmods) is installed."
			exit 1
		}

        $jmodPaths = "$Env:JAVA_HOME/jmods"
    }
    'x64' {
		$javaFxVersion='25.0.3'
		$javaFxJmodsUrl = "https://download2.gluonhq.com/openjfx/${javaFxVersion}/openjfx-${javaFxVersion}_windows-x64_bin-jmods.zip"
		$javaFxJmodsSHA256 = '0bf9b83260b85607a9ba200124debabd9cdb013cbc0d659e62a20192a7137907'
		$javaFxJmods = '.\resources\jfxJmods.zip'

		if( !(Test-Path -Path $javaFxJmods) ) {
			Write-Host "Downloading ${javaFxJmodsUrl}..."
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

		$jmodPaths="$buildDir/resources/javafx-jmods";
    }
    default {
        Write-Error "Unsupported architecture: $arch"
        exit 1
    }
}

## create custom runtime
### check for JEP 493
if ((& "$Env:JAVA_HOME\bin\jlink" --help | Select-String -Pattern "Linking from run-time image enabled" -SimpleMatch | Measure-Object).Count -eq 0 ) {
	$jmodPaths="$Env:JAVA_HOME/jmods;" + $jmodPaths;
}

### create runtime
Invoke-CommandWithExitCheck -Command `
    "$Env:JAVA_HOME\bin\jlink" -Arguments @(
    "--verbose",
    "--output", "runtime",
    "--module-path", $jmodPaths,
    "--add-modules", "java.base,java.desktop,java.instrument,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.accessibility,jdk.management.jfr,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.crypto.mscapi,java.compiler,javafx.base,javafx.graphics,javafx.controls,javafx.fxml",
    "--strip-native-commands",
    "--no-header-files",
    "--no-man-pages",
    "--strip-debug",
    "--compress", "zip-0" #do not compress and use msi compression
    )

if ($buildInstaller) {
$appPath = ".\$AppName"
if ($clean -and (Test-Path -Path $appPath)) {
	Remove-Item -Path $appPath -Force -Recurse
}


$javaOptions = New-InstallerJavaOptions -BuildNumber "msi-$revisionNo" -DisableUpdateCheck $false -LoopbackAliasName $LoopbackAlias


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
	--app-version "$semVerNo.$revisionNo" `
	--resource-dir resources `
	--icon resources/$AppName.ico `
	--add-launcher "${AppName} (Debug)=$buildDir\debug-launcher.properties" `
	@javaOptions

if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage Appimage failed with exit code $LASTEXITCODE"
	return 1;
}

#Create RTF license for msi
Invoke-CommandWithExitCheck -Command `
    "../../mvnw.cmd" -Arguments @("-B", "-f", "$buildDir/../../pom.xml", "license:add-third-party", `
    "-Dlicense.thirdPartyFilename=license.rtf", `
    "-Dlicense.fileTemplate=$buildDir\resources\licenseTemplate.ftl", `
    "-Dlicense.outputDirectory=$buildDir\resources\", `
    "-Dlicense.includedScopes=compile", `
    "-Dlicense.excludedGroups=^org\.cryptomator", `
    "-Dlicense.failOnMissing=true", `
    "-Dlicense.licenseMergesUrl=file:///$buildDir/../../license/merges")

# patch app dir
Copy-Item "contrib\*" -Destination "$AppName"
attrib -r "$AppName\$AppName.exe"
attrib -r "$AppName\${AppName} (Debug).exe"

# create .msi
$Env:JP_WIXWIZARD_RESOURCES = "$buildDir\resources\"
$Env:JP_WIXWIZARD_RESOURCES_PROPERTIES_FORMAT = "${Env:JP_WIXWIZARD_RESOURCES}".Replace('\', '\\');
$Env:JP_WIXHELPER_DIR = ""

Get-Content .\resources\FAvaultFile.template.properties ` # Similar to envsubst
    | ForEach-Object { $ExecutionContext.InvokeCommand.ExpandString($_) } `
    | Out-File -FilePath .\resources\FAvaultFile.properties

Invoke-CommandWithExitCheck -Command `
    "$Env:JAVA_HOME\bin\jpackage" -Arguments @(
    "--verbose",
    "--type", "msi",
    "--win-upgrade-uuid", $UpgradeUUID,
    "--app-image", $AppName,
    "--dest", "installer",
    "--name", $AppName,
    "--vendor", $Vendor,
    "--copyright", $copyright,
    "--app-version", "$semVerNo.$revisionNo",
    "--win-menu",
    "--win-dir-chooser",
    "--win-shortcut-prompt",
    "--win-menu-group", $AppName,
    "--resource-dir", "resources",
    "--license-file", "resources/license.rtf",
    "--win-update-url", $UpdateUrl,
    "--about-url", $AboutUrl,
    "--file-associations", "resources/FAvaultFile.properties"
    )

#Create RTF license for bundle
Invoke-CommandWithExitCheck -Command `
	"../../mvnw.cmd" -Arguments @("-B", "-f", "$buildDir/../../pom.xml", "license:add-third-party", `
	"-Dlicense.thirdPartyFilename=license.rtf", `
	"-Dlicense.fileTemplate=$buildDir\bundle\resources\licenseTemplate.ftl", `
	"-Dlicense.outputDirectory=$buildDir\bundle\resources\", `
	"-Dlicense.includedScopes=compile", `
	"-Dlicense.excludedGroups=^org\.cryptomator", `
	"-Dlicense.failOnMissing=true", `
	"-Dlicense.licenseMergesUrl=file:///$buildDir/../../license/merges")

# download Winfsp
$winfspMsiUrl= 'https://github.com/winfsp/winfsp/releases/download/v2.1/winfsp-2.1.25156.msi'
$winfspMsiHash = '073A70E00F77423E34BED98B86E600DEF93393BA5822204FAC57A29324DB9F7A'
Write-Host "Downloading ${winfspMsiUrl}..."
Invoke-WebRequest $winfspMsiUrl -OutFile ".\bundle\resources\winfsp.msi" # redirects are followed by default
$computedHash = $(Get-FileHash -Path '.\bundle\resources\winfsp.msi' -Algorithm SHA256).Hash
if (! $computedHash.Equals($winfspMsiHash)) {
	Write-Error -Category InvalidData -CategoryActivity "Data integrity check failed" -Message @"
	Downloaded Winfsp Installer does not match stored SHA256 checksum.
	Expected: $winfspMsiHash
	Actual:   $computedHash
"@
	exit 1
}

# download legacy-winfsp uninstaller
$winfspUninstaller= 'https://github.com/cryptomator/winfsp-uninstaller/releases/latest/download/winfsp-uninstaller.exe'
Write-Host "Downloading ${winfspUninstaller}..."
Invoke-WebRequest $winfspUninstaller -OutFile ".\bundle\resources\winfsp-uninstaller.exe" # redirects are followed by default

# copy MSI to bundle resources
Copy-Item ".\installer\$AppName-*.msi" -Destination ".\bundle\resources\$AppName.msi" -Force

# create bundle including winfsp
Invoke-CommandWithExitCheck -Command `
    "wix" -Arguments @(
    "build",
    "-define", "BundleName=$AppName",
    "-define", "BundleVersion=$semVerNo.$revisionNo",
    "-define", "BundleVendor=$Vendor",
    "-define", "BundleCopyright=$copyright",
    "-define", "AboutUrl=$AboutUrl",
    "-define", "HelpUrl=$HelpUrl",
    "-define", "UpdateUrl=$UpdateUrl",
    "-ext", "WixToolset.Util.wixext",
    "-ext", "WixToolset.BootstrapperApplications.wixext",
    ".\bundle\bundleWithWinfsp.wxs",
    "-out", ".\installer\$AppName-Installer.exe"
)

Write-Host "Created EXE installer .\installer\$AppName-Installer.exe"
}

if ($buildPortable) {
	# ===================================================================
	# Portable build: a self-contained app image that keeps all of its
	# data (settings, keychain, logs, ipc socket) in a "data" directory
	# next to the executable. $ROOTDIR is a jpackage launcher macro that
	# is expanded at runtime to the directory containing Cryptomator.exe,
	# so the folder can be moved freely (e.g. onto a USB stick).
	#
	# Compared to the installer this build deliberately does NOT
	#  - bundle or install WinFsp (requires an installer with admin rights);
	#    if WinFsp is not installed on the host, WebDAV is used instead
	#  - add the loopback alias to the hosts file (WebDAV uses localhost)
	#  - register a Windows autostart entry or file associations
	# ===================================================================
	$portableDir = ".\portable"
	$portableAppPath = "$portableDir\$AppName"
	$portableDataDir = "`$ROOTDIR/data"
	# the app dir is a pure build output and jpackage refuses to overwrite it, so always start from scratch
	if (Test-Path -Path $portableAppPath) {
		Remove-Item -Path $portableAppPath -Force -Recurse
	}

	$portableJavaOptions = @(
	"--java-options", "--enable-native-access=javafx.graphics,org.cryptomator.jfuse.win,org.cryptomator.integrations.win"
	"--java-options", "-Xss5m"
	"--java-options", "-Xmx256m"
	"--java-options", "-Dcryptomator.appVersion=`"$semVerNo`""
	"--java-options", "-Dfile.encoding=`"utf-8`""
	"--java-options", "-Djava.net.useSystemProxies=true"
	"--java-options", "-Dcryptomator.logDir=`"$portableDataDir/logs`""
	"--java-options", "-XX:ErrorFile=`"$portableDataDir/logs/cryptomator_crash.log`""
	"--java-options", "-Dcryptomator.adminConfigPath=`"$portableDataDir/config.properties`""
	"--java-options", "-Dcryptomator.settingsPath=`"$portableDataDir/settings.json`""
	"--java-options", "-Dcryptomator.ipcSocketPath=`"$portableDataDir/ipc.socket`""
	"--java-options", "-Dcryptomator.p12Path=`"$portableDataDir/key.p12`""
	"--java-options", "-Dcryptomator.mountPointsDir=`"@{userhome}/$AppName`""
	"--java-options", "-Dcryptomator.integrationsWin.keychainPaths=`"$portableDataDir/keychain.json`""
	"--java-options", "-Dcryptomator.integrationsWin.windowsHelloKeychainPaths=`"$portableDataDir/windowsHelloKeychain.json`""
	"--java-options", "-Dcryptomator.showTrayIcon=true"
	"--java-options", "-Dcryptomator.buildNumber=`"portable-$revisionNo`""
	"--java-options", "-Dcryptomator.disableUpdateCheck=false"
	"--java-options", "-Dcryptomator.hub.enableTrustOnFirstUse=true"
	)

	# create portable app dir
	& "$Env:JAVA_HOME\bin\jpackage" `
		--verbose `
		--type app-image `
		--runtime-image runtime `
		--input ../../target/libs `
		--module-path ../../target/mods `
		--module $ModuleAndMainClass `
		--dest $portableDir `
		--name $AppName `
		--vendor $Vendor `
		--copyright $copyright `
		--app-version "$semVerNo.$revisionNo" `
		--resource-dir resources `
		--icon resources/$AppName.ico `
		--add-launcher "${AppName} (Debug)=$buildDir\debug-launcher.properties" `
		@portableJavaOptions

	if ($LASTEXITCODE -ne 0) {
		Write-Error "jpackage portable Appimage failed with exit code $LASTEXITCODE"
		return 1;
	}

	# patch portable app dir
	Copy-Item "contrib\jnidispatch.dll" -Destination "$portableAppPath"
	Copy-Item "$buildDir\..\..\LICENSE.txt" -Destination "$portableAppPath"
	Copy-Item "$buildDir\portable\README-PORTABLE.txt" -Destination "$portableAppPath"
	New-Item -ItemType Directory -Path "$portableAppPath\data" -Force | Out-Null
	attrib -r "$portableAppPath\$AppName.exe"
	attrib -r "$portableAppPath\${AppName} (Debug).exe"

	# create portable zip
	$portableZip = "$portableDir\$AppName-$version-$($archName.ToLower())-portable.zip"
	Remove-Item -Path $portableZip -Force -ErrorAction Ignore
	Compress-Archive -Path $portableAppPath -DestinationPath $portableZip -CompressionLevel Optimal

	Write-Host "Created portable app dir $portableAppPath"
	Write-Host "Created portable ZIP $portableZip"
}

if ($buildCorp) {
	# ===================================================================
	# Corp build: a per-user msi for managed/corporate machines where the
	# user has no admin rights. It installs into %LOCALAPPDATA%, so it
	# needs no elevation, and deliberately does NOT
	#  - bundle WinFsp (IT deploys the WinFsp msi separately, otherwise
	#    vaults are mounted via WebDAV)
	#  - add the loopback alias to the hosts file
	#  - create the admin config dir in ProgramData; IT can still deploy
	#    C:\ProgramData\Cryptomator\config.properties via GPO/Intune to
	#    enforce settings (see corp\README.md)
	#  - check for updates (IT rolls out new versions)
	# ===================================================================
	$corpDir = ".\corp"
	$corpAppPath = "$corpDir\$AppName"
	$corpResources = "$corpDir\resources"
	$corpResourcesAbs = Join-Path $buildDir "corp\resources"
	if (Test-Path -Path $corpAppPath) {
		Remove-Item -Path $corpAppPath -Force -Recurse
	}

	$corpJavaOptions = New-InstallerJavaOptions -BuildNumber "corp-$revisionNo" -DisableUpdateCheck $true -LoopbackAliasName ""

	# create corp app dir
	& "$Env:JAVA_HOME\bin\jpackage" `
		--verbose `
		--type app-image `
		--runtime-image runtime `
		--input ../../target/libs `
		--module-path ../../target/mods `
		--module $ModuleAndMainClass `
		--dest $corpDir `
		--name $AppName `
		--vendor $Vendor `
		--copyright $copyright `
		--app-version "$semVerNo.$revisionNo" `
		--resource-dir resources `
		--icon resources/$AppName.ico `
		--add-launcher "${AppName} (Debug)=$buildDir\debug-launcher.properties" `
		@corpJavaOptions

	if ($LASTEXITCODE -ne 0) {
		Write-Error "jpackage corp Appimage failed with exit code $LASTEXITCODE"
		return 1;
	}

	# patch corp app dir
	Copy-Item "contrib\*" -Destination "$corpAppPath"
	attrib -r "$corpAppPath\$AppName.exe"
	attrib -r "$corpAppPath\${AppName} (Debug).exe"

	# assemble wix resources: the regular ones plus the corp overrides.wxi (no jmods dir)
	Remove-Item -Path $corpResources -Force -Recurse -ErrorAction Ignore
	New-Item -ItemType Directory -Path $corpResources -Force | Out-Null
	Get-ChildItem -Path ".\resources" -File | Copy-Item -Destination $corpResources
	Copy-Item "$corpDir\overrides.wxi" -Destination "$corpResources\overrides.wxi" -Force

	# create RTF license for corp msi
	Invoke-CommandWithExitCheck -Command `
	    "../../mvnw.cmd" -Arguments @("-B", "-f", "$buildDir/../../pom.xml", "license:add-third-party", `
	    "-Dlicense.thirdPartyFilename=license.rtf", `
	    "-Dlicense.fileTemplate=$buildDir\resources\licenseTemplate.ftl", `
	    "-Dlicense.outputDirectory=$corpResourcesAbs\", `
	    "-Dlicense.includedScopes=compile", `
	    "-Dlicense.excludedGroups=^org\.cryptomator", `
	    "-Dlicense.failOnMissing=true", `
	    "-Dlicense.licenseMergesUrl=file:///$buildDir/../../license/merges")

	# create per-user .msi
	$Env:JP_WIXWIZARD_RESOURCES = "$corpResourcesAbs\"
	$Env:JP_WIXWIZARD_RESOURCES_PROPERTIES_FORMAT = "${Env:JP_WIXWIZARD_RESOURCES}".Replace('\', '\\');
	$Env:JP_WIXHELPER_DIR = ""

	Get-Content "$corpResources\FAvaultFile.template.properties" `
	    | ForEach-Object { $ExecutionContext.InvokeCommand.ExpandString($_) } `
	    | Out-File -FilePath "$corpResources\FAvaultFile.properties"

	Remove-Item -Path "$corpDir\*.msi" -Force -ErrorAction Ignore
	Invoke-CommandWithExitCheck -Command `
	    "$Env:JAVA_HOME\bin\jpackage" -Arguments @(
	    "--verbose",
	    "--type", "msi",
	    "--win-per-user-install",
	    "--win-upgrade-uuid", $CorpUpgradeUUID,
	    "--app-image", $corpAppPath,
	    "--dest", $corpDir,
	    "--name", $AppName,
	    "--vendor", $Vendor,
	    "--copyright", $copyright,
	    "--app-version", "$semVerNo.$revisionNo",
	    "--win-menu",
	    "--win-shortcut-prompt",
	    "--win-menu-group", $AppName,
	    "--resource-dir", $corpResources,
	    "--license-file", "$corpResources\license.rtf",
	    "--about-url", $AboutUrl,
	    "--file-associations", "$corpResources\FAvaultFile.properties"
	    )

	$corpMsi = "$corpDir\$AppName-$version-$($archName.ToLower())-corp.msi"
	Get-ChildItem -Path "$corpDir\$AppName-*.msi" | Move-Item -Destination $corpMsi -Force
	Write-Host "Created per-user corp MSI $corpMsi"
}
return 0;
}

# ============================
# Script Execution Starts Here
# ============================
if ($clean) {
	Write-Host "Cleaning up previous build artifacts..."
	Remove-Item -Path ".\runtime" -Force -Recurse -ErrorAction Ignore -ProgressAction SilentlyContinue
	Remove-Item -Path ".\$AppName" -Force -Recurse -ErrorAction Ignore -ProgressAction SilentlyContinue
	Remove-Item -Path ".\installer" -Force -Recurse -ErrorAction Ignore -ProgressAction SilentlyContinue
	Remove-Item -Path ".\portable\$AppName" -Force -Recurse -ErrorAction Ignore -ProgressAction SilentlyContinue
	Remove-Item -Path ".\portable\*.zip" -Force -ErrorAction Ignore -ProgressAction SilentlyContinue
	Remove-Item -Path ".\corp\$AppName" -Force -Recurse -ErrorAction Ignore -ProgressAction SilentlyContinue
	Remove-Item -Path ".\corp\resources" -Force -Recurse -ErrorAction Ignore -ProgressAction SilentlyContinue
	Remove-Item -Path ".\corp\*.msi" -Force -ErrorAction Ignore -ProgressAction SilentlyContinue
}
return Main

