@echo off
:: Batch wrapper for PowerShell script to modify Cryptomator update check settings
:: This is executed as a Custom Action during MSI installation

:: Get the install directory from the Custom Action property
set "INSTALLDIR=%~dp1"
set "DISABLEUPDATECHECK=%~2"

:: Log for debugging
echo INSTALLDIR=%INSTALLDIR%
echo DISABLEUPDATECHECK=%DISABLEUPDATECHECK%

:: Normalize install directory (remove trailing slash if present)
if "%INSTALLDIR:~-1%"=="\" set "INSTALLDIR=%INSTALLDIR:~0,-1%"


:: Execute the PowerShell script
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy RemoteSigned -File "%INSTALLDIR%\patchUpdateCheck.ps1"^
 -InstallDir "%INSTALLDIR%"^
 -DisableUpdateCheck "%DISABLEUPDATECHECK%" 

:: Return the exit code from PowerShell
exit /b %ERRORLEVEL%