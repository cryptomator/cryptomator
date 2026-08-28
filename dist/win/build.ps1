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
	[ValidateSet('All', 'AppImage', 'Msi')][string] $BuildStage = 'All',
	[string] $BundleUpgradeCode,
	[string] $BundleLaunchTarget,
	[bool] $clean = $false # if true, cleans up previous build artifacts
)

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

function Main {

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue' # disables Invoke-WebRequest's progress bar, which slows down downloads to a few bytes/s

# check preconditions
if ((Get-Command "git" -ErrorAction SilentlyContinue) -eq $null)
{
   Write-Error "Unable to find git.exe in your PATH (try: choco install git)"
   exit 1
}
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
if ($wixExtensions -notmatch 'WixToolset.BootstrapperApplications.wixext') {
    Write-Error 'Wix Bootstrapper extension missing. Please install it with: wix.exe extension add WixToolset.BootstrapperApplications.wixext/6.0.2 --global)'
    exit 1
}

$buildDir = Split-Path -Parent $PSCommandPath
$version = $(../../mvnw.cmd -f $buildDir/../../pom.xml help:evaluate -Dexpression="project.version" -q -DforceStdout)
$semVerNo = $version -replace '(\d+\.\d+\.\d+).*','$1'
$revisionNo = $(git rev-list --count HEAD)

Write-Host "`$version=$version"
Write-Host "`$semVerNo=$semVerNo"
Write-Host "`$revisionNo=$revisionNo"
Write-Host "`$buildDir=$buildDir"
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
        Write-Error "Unsupported architecture: $archName"
        exit 1
    }
}

if ([string]::IsNullOrWhiteSpace($BundleUpgradeCode)) {
	$BundleUpgradeCode = switch ($archName) {
		'ARM64' { '070b3234-eaf9-4294-ba31-78a0e2f0a6be' }
		default { '29eea626-2e5b-4449-b5f8-4602925ddf7b' }
	}
}
$parsedBundleUpgradeCode = [guid]::Empty
if (-not [guid]::TryParse($BundleUpgradeCode, [ref] $parsedBundleUpgradeCode) -or $parsedBundleUpgradeCode -eq [guid]::Empty) {
	Write-Error "BundleUpgradeCode must be a non-empty GUID. Received: '$BundleUpgradeCode'"
	exit 1
}
$BundleUpgradeCode = $parsedBundleUpgradeCode.ToString()
if (-not $PSBoundParameters.ContainsKey('BundleLaunchTarget') -and $archName -eq 'x64') {
	$BundleLaunchTarget = "[ProgramFiles64Folder]\$AppName\$AppName.exe"
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

$appPath = ".\$AppName"
if ($clean -and (Test-Path -Path $appPath)) {
	Remove-Item -Path $appPath -Force -Recurse
}


$javaOptions = @(
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
"--java-options", "-Dcryptomator.loopbackAlias=`"$LoopbackAlias`""
"--java-options", "-Dcryptomator.integrationsWin.autoStartShellLinkName=`"$AppName`""
"--java-options", "-Dcryptomator.integrationsWin.keychainPaths=`"@{appdata}/$AppName/keychain.json;@{userhome}/AppData/Roaming/$AppName/keychain.json`""
"--java-options", "-Dcryptomator.integrationsWin.windowsHelloKeychainPaths=`"@{appdata}/$AppName/windowsHelloKeychain.json`""
"--java-options", "-Dcryptomator.showTrayIcon=true"
"--java-options", "-Dcryptomator.buildNumber=`"msi-$revisionNo`""
"--java-options", "-Dcryptomator.disableUpdateCheck=false"
"--java-options", "-Dcryptomator.hub.enableTrustOnFirstUse=true"
)


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
	exit $LASTEXITCODE
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
if ($archName -eq 'ARM64') {
	# The checked-in JNA dispatcher is x64-only and must not enter the Arm64 payload.
	Get-ChildItem "contrib\*" -File |
		Where-Object Name -ne 'jnidispatch.dll' |
		Copy-Item -Destination "$AppName"
} else {
	Copy-Item "contrib\*" -Destination "$AppName"
}
attrib -r "$AppName\$AppName.exe"
attrib -r "$AppName\${AppName} (Debug).exe"

if ($BuildStage -eq 'AppImage') {
	Write-Host "BuildStage AppImage requested; skipping MSI and EXE bundle creation."
	return 0
}

# create .msi
$msiHelperBuildDir = ".\msi-helper-build"
$msiHelperOutputDir = ".\msi-helper-output"
Remove-Item -Path $msiHelperBuildDir, $msiHelperOutputDir, ".\msica.dll" -Force -Recurse -ErrorAction Ignore
Invoke-CommandWithExitCheck -Command `
    "$Env:JAVA_HOME\bin\jpackage" -Arguments @(
    "--type", "msi",
    "--win-upgrade-uuid", $UpgradeUUID,
    "--app-image", $AppName,
    "--dest", $msiHelperOutputDir,
    "--name", "CryptomatorHelper",
    "--vendor", $Vendor,
    "--copyright", $copyright,
    "--app-version", "1.0",
    "--temp", $msiHelperBuildDir
    )
$msiHelperDll = Get-ChildItem -Path $msiHelperBuildDir -Recurse -Filter "msica.dll" | Select-Object -First 1
if (-not $msiHelperDll) {
	Write-Error "Unable to find msica.dll in $msiHelperBuildDir"
	exit 1
}
Copy-Item -Path $msiHelperDll.FullName -Destination ".\msica.dll" -Force
$Env:JP_WIXWIZARD_RESOURCES = "$buildDir\resources\"
$Env:JP_WIXWIZARD_RESOURCES_PROPERTIES_FORMAT = "${Env:JP_WIXWIZARD_RESOURCES}".Replace('\', '\\');
$Env:JP_WIXHELPER_DIR = "$((Get-Location).Path)\"

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
Remove-Item -Path $msiHelperBuildDir, $msiHelperOutputDir, ".\msica.dll" -Force -Recurse -ErrorAction Ignore

if ($BuildStage -eq 'Msi') {
	Write-Host "BuildStage Msi requested; skipping EXE bundle creation."
	return 0
}

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
$bundleWixArgs = @(
	"build",
	"-define", "BundleName=$AppName",
	"-define", "BundleVersion=$semVerNo.$revisionNo",
	"-define", "BundleVendor=$Vendor",
	"-define", "BundleCopyright=$copyright",
	"-define", "AboutUrl=$AboutUrl",
	"-define", "HelpUrl=$HelpUrl",
	"-define", "UpdateUrl=$UpdateUrl",
	"-define", "BundleUpgradeCode=$BundleUpgradeCode"
)
if ($BundleLaunchTarget) {
	$bundleWixArgs += @("-define", "BundleLaunchTarget=$BundleLaunchTarget")
}
$bundleWixArgs += @(
	"-ext", "WixToolset.Util.wixext",
	"-ext", "WixToolset.BootstrapperApplications.wixext",
	".\bundle\bundleWithWinfsp.wxs",
	"-out", ".\installer\$AppName-Installer.exe"
)
Invoke-CommandWithExitCheck -Command "wix" -Arguments $bundleWixArgs

Write-Host "Created EXE installer .\installer\$AppName-Installer.exe"
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
}
Main
exit 0
