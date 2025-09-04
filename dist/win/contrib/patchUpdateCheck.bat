@echo off
:: Batch wrapper for PowerShell script to modify Cryptomator update check settings
:: This is executed as a Custom Action during MSI installation
:: This file must be located in the INSTALLDIR

set "DISABLEUPDATECHECK=%~1"

:: Log for debugging
echo DISABLEUPDATECHECK=%DISABLEUPDATECHECK%

:: Change to INSTALLDIR
cd %~dp0
:: Execute the PowerShell script
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy RemoteSigned -File ".\patchUpdateCheck.ps1"^
 -DisableUpdateCheck "%DISABLEUPDATECHECK%"

:: Return the exit code from PowerShell
exit /b %ERRORLEVEL%