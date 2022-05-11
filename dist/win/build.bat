@echo off
SET APPNAME="Cryptomator"
SET VENDOR="Skymatic GmbH"
SET FIRST_COPYRIGHT_YEAR=2016
SET ABOUT_URL="https://cryptomator.org"
SET UPDATE_URL="https://cryptomator.org/downloads/"
SET HELP_URL="https://cryptomator.org/contact/"
powershell -NoLogo -NoExit -ExecutionPolicy Unrestricted -Command .\build.ps1^
 -AppName %APPNAME%^
 -Vendor ""%VENDOR%""^
 -CopyrightStartYear %FIRST_COPYRIGHT_YEAR%^
 -AboutUrl "%ABOUT_URL%"^
 -HelpUrl "%HELP_URL%"^
 -UpdateUrl "%UPDATE_URL%"^
 -Clean 1