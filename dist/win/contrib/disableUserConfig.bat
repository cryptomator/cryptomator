@echo off
:: Batch wrapper for PowerShell script to disable user configuration in Cryptomator
:: This is executed as a Custom Action during MSI installation
:: This file must be located in the INSTALLDIR

:: Change to INSTALLDIR
cd %~dp0
:: Execute the PowerShell script
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy RemoteSigned -File ".\disableUserConfig.ps1"

:: Return the exit code from PowerShell
exit /b %ERRORLEVEL%