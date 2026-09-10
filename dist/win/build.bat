@echo off
:: Default values for Cryptomator builds
SET APPNAME="Cryptomator"
SET MAIN_JAR_GLOB="cryptomator-*"
SET UPGRADE_UUID="bda45523-42b1-4cae-9354-a45475ed4775"
SET CORP_UPGRADE_UUID="d98d0145-341a-4684-bdb6-ee480294541a"
SET VENDOR="Skymatic GmbH"
SET FIRST_COPYRIGHT_YEAR=2016
SET ABOUT_URL="https://cryptomator.org"
SET UPDATE_URL="https://cryptomator.org/downloads/"
SET HELP_URL="https://cryptomator.org/contact/"
SET MODULE_AND_MAIN_CLASS="org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator"
SET LOOPBACK_ALIAS="cryptomator-vault"

:: Usage: build.bat [clean] [installer|portable|corp|all]
::   clean      remove previous build artifacts before building
::   installer  build the msi and exe installer (default)
::   portable   build the portable zip
::   corp       build the per-user msi for corporate machines without admin rights
::   all        build installer, portable zip and corp msi
SET CLEAN=0
SET TARGET=installer
FOR %%A IN (%*) DO (
	IF /I "%%~A"=="clean" SET CLEAN=1
	IF /I "%%~A"=="installer" SET TARGET=installer
	IF /I "%%~A"=="portable" SET TARGET=portable
	IF /I "%%~A"=="corp" SET TARGET=corp
	IF /I "%%~A"=="all" SET TARGET=all
)

:: keep the window open on failure if started by double-click, so errors stay visible (never in CI)
SET PAUSE_AT_END=0
echo %CMDCMDLINE% | findstr /I /C:"/c" >nul 2>&1 && SET PAUSE_AT_END=1
IF DEFINED CI SET PAUSE_AT_END=0

:: preconditions: prefer PowerShell 7 (pwsh), fall back to the Windows PowerShell 5.1 that ships with Windows
SET PS=pwsh
where pwsh >nul 2>&1
IF ERRORLEVEL 1 (
	where powershell >nul 2>&1
	IF ERRORLEVEL 1 (
		echo ERROR: Neither pwsh.exe nor powershell.exe found in PATH.
		echo        Install PowerShell 7 with: winget install Microsoft.PowerShell
		SET EXITCODE=1
		GOTO :end
	)
	SET PS=powershell
	echo NOTE: PowerShell 7 ^(pwsh^) not found, using Windows PowerShell. Install PowerShell 7 with: winget install Microsoft.PowerShell
)
:: locate a JDK 26 if JAVA_HOME is not set: common vendor install dirs first (last match = newest), then java.exe from PATH
IF "%JAVA_HOME%"=="" (
	FOR /D %%D IN ("%ProgramFiles%\Eclipse Adoptium\jdk-26*" "%ProgramFiles%\Java\jdk-26*" "%ProgramFiles%\Zulu\zulu-26*" "%ProgramFiles%\Microsoft\jdk-26*" "%ProgramFiles%\Amazon Corretto\jdk26*" "%ProgramFiles%\BellSoft\LibericaJDK-26*" "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-26*") DO CALL :setjavahome "%%~D"
)
IF "%JAVA_HOME%"=="" (
	FOR /F "delims=" %%J IN ('where java 2^>nul') DO CALL :setjavahome "%%~dpJ.."
)
IF "%JAVA_HOME%"=="" (
	echo ERROR: No JDK 26 found. Install one, e.g. with: winget install EclipseAdoptium.Temurin.26.JDK
	echo        or point JAVA_HOME to an existing JDK 26, e.g. set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-26.0.1.8-hotspot
	SET EXITCODE=1
	GOTO :end
)
IF NOT EXIST "%JAVA_HOME%\bin\jpackage.exe" (
	echo ERROR: "%JAVA_HOME%\bin\jpackage.exe" not found. JAVA_HOME must point to a full JDK, not a JRE.
	SET EXITCODE=1
	GOTO :end
)
echo Using JDK at "%JAVA_HOME%"

:: build.ps1 expects to be run from dist\win
pushd "%~dp0"
echo Building target "%TARGET%" (clean=%CLEAN%) ...
%PS% -NoLogo -NoProfile -ExecutionPolicy Unrestricted -Command .\build.ps1^
 -AppName %APPNAME%^
 -MainJarGlob "%MAIN_JAR_GLOB%"^
 -ModuleAndMainClass "%MODULE_AND_MAIN_CLASS%"^
 -UpgradeUUID "%UPGRADE_UUID%"^
 -CorpUpgradeUUID "%CORP_UPGRADE_UUID%"^
 -Vendor ""%VENDOR%""^
 -CopyrightStartYear %FIRST_COPYRIGHT_YEAR%^
 -AboutUrl "%ABOUT_URL%"^
 -HelpUrl "%HELP_URL%"^
 -UpdateUrl "%UPDATE_URL%"^
 -LoopbackAlias "%LOOPBACK_ALIAS%"^
 -Target %TARGET%^
 -Clean %CLEAN%
SET EXITCODE=%ERRORLEVEL%
popd
IF NOT "%EXITCODE%"=="0" echo Build failed with exit code %EXITCODE%.

:end
IF "%PAUSE_AT_END%"=="1" IF NOT "%EXITCODE%"=="0" pause
EXIT /B %EXITCODE%

:: sets JAVA_HOME to the given directory if it contains a full JDK (normalizes trailing "\..")
:setjavahome
IF EXIST "%~f1\bin\jpackage.exe" SET "JAVA_HOME=%~f1"
EXIT /B 0
