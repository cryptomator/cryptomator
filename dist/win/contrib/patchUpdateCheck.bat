@echo off
:: Batch wrapper for PowerShell script to modify Cryptomator update check settings
:: This is executed as a Custom Action during MSI installation

:: Get the install directory from the Custom Action property
set "INSTALLDIR=%~dp1"
set "DISABLEUPDATECHECK=%~2"

:: Log for debugging
echo INSTALLDIR=%INSTALLDIR%
echo DISABLEUPDATECHECK=%DISABLEUPDATECHECK%

:: Execute the PowerShell script
:: DO NOT REMOVE WHITE SPACE AFTER %INSTALLDIR%.
:: The variable is expanded to a value ending with a slash "\", which would escape the double quote breaking the parameter parsing
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy RemoteSigned -File "%INSTALLDIR%patchUpdateCheck.ps1"^
 -InstallDir "%INSTALLDIR% "^
 -DisableUpdateCheck "%DISABLEUPDATECHECK%" 

:: Return the exit code from PowerShell
exit /b %ERRORLEVEL%