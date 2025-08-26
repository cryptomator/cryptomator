@echo off
:: Batch wrapper for PowerShell script to modify Cryptomator update check settings
:: This is executed as a Custom Action during MSI installation

:: Get the install directory from the Custom Action property
set "INSTALLDIR=%~1"
set "DISABLEUPDATECHECK=%~2"

:: Log for debugging
echo INSTALLDIR=%INSTALLDIR%
echo DISABLEUPDATECHECK=%DISABLEUPDATECHECK%

:: Execute the PowerShell script with bypass execution policy
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%INSTALLDIR%patchUpdateCheck.ps1" -InstallDir "%INSTALLDIR%" -DisableUpdateCheck "%DISABLEUPDATECHECK%"

:: Return the exit code from PowerShell
exit /b %ERRORLEVEL%