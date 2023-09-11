@echo off
:: Default values for Cryptomator builds
SET APPNAME="Cryptomator"
SET MAIN_JAR_GLOB="cryptomator-*"
SET UPGRADE_UUID="bda45523-42b1-4cae-9354-a45475ed4775"
SET VENDOR="Skymatic GmbH"
SET FIRST_COPYRIGHT_YEAR=2016
SET ABOUT_URL="https://cryptomator.org"
SET UPDATE_URL="https://cryptomator.org/downloads/"
SET HELP_URL="https://cryptomator.org/contact/"
SET MODULE_AND_MAIN_CLASS="org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator"
SET LOOPBACK_ALIAS="cryptomator-vault"

powershell -NoLogo -NoProfile -ExecutionPolicy Unrestricted -Command .\build.ps1^
 -AppName %APPNAME%^
 -MainJarGlob "%MAIN_JAR_GLOB%"^
 -ModuleAndMainClass "%MODULE_AND_MAIN_CLASS%"^
 -UpgradeUUID "%UPGRADE_UUID%"^
 -Vendor ""%VENDOR%""^
 -CopyrightStartYear %FIRST_COPYRIGHT_YEAR%^
 -AboutUrl "%ABOUT_URL%"^
 -HelpUrl "%HELP_URL%"^
 -UpdateUrl "%UPDATE_URL%"^
 -LoopbackAlias "%LOOPBACK_ALIAS%"^
 -Clean 1