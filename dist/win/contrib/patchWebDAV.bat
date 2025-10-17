@echo off
:: Batch wrapper for PowerShell script to adjust Windows network settings for the Cryptomator WebDAVAdapter 
:: This is executed as a Custom Action during MSI installation
:: This file must be located in the INSTALLDIR

set "LOOPBACK_ALIAS=%1"

:: Log for debugging
echo LOOPBACK_ALIAS=%LOOPBACK_ALIAS%

:: Change to INSTALLDIR
cd %~dp0
:: Execute the PowerShell script
powershell -NoLogo -NoProfile -NonInteractive -ExecutionPolicy RemoteSigned -File .\patchWebDAV.ps1^
 -LoopbackAlias %LOOPBACK_ALIAS%

:: Return the exit code from PowerShell
exit /b %ERRORLEVEL%