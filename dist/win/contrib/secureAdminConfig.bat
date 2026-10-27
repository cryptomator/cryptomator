@echo off
:: Restricts the machine-wide admin config directory and its config.properties to SYSTEM and Administrators, with read-only access for Users
:: This is executed as a deferred Custom Action (running as SYSTEM) during MSI installation
:: Windows Installer neither resets owner nor DACL of a pre-existing directory and skips a pre-existing config.properties (NeverOverwrite),
:: so both might still be controlled by a standard user who created them before the installation.
:: Usage: secureAdminConfig.bat "C:\ProgramData\Cryptomator\"

setlocal
set "CONFIG_DIR=%~1"
:: MSI directory properties end with a backslash
if "%CONFIG_DIR:~-1%"=="\" set "CONFIG_DIR=%CONFIG_DIR:~0,-1%"
set "CONFIG_FILE=%CONFIG_DIR%\config.properties"

:: Log for debugging
echo CONFIG_DIR=%CONFIG_DIR%

:: Refuse to modify the target of a planted junction or symlink
call :failIfReparsePoint "%CONFIG_DIR%" || exit /b 1
call :failIfReparsePoint "%CONFIG_FILE%" || exit /b 1

:: Directory: Administrators as owner, drop all explicit and inherited ACEs, then grant SYSTEM and Administrators full control and Users read/execute
icacls "%CONFIG_DIR%" /setowner *S-1-5-32-544 /L /Q || exit /b 1
icacls "%CONFIG_DIR%" /reset /L /Q || exit /b 1
icacls "%CONFIG_DIR%" /inheritance:r /grant:r *S-1-5-18:(OI)(CI)F *S-1-5-32-544:(OI)(CI)F *S-1-5-32-545:(OI)(CI)RX /L /Q || exit /b 1

if not exist "%CONFIG_FILE%" exit /b 0

:: File: Administrators as owner, drop all explicit ACEs, so that only the ACEs inherited from the directory remain
icacls "%CONFIG_FILE%" /setowner *S-1-5-32-544 /L /Q || exit /b 1
icacls "%CONFIG_FILE%" /reset /L /Q || exit /b 1

exit /b 0

:failIfReparsePoint
:: %~a1 contains an "l" if the path is a reparse point (junction, symlink, ...) and is empty if the path does not exist
set "ATTRIBUTES=%~a1"
if not "%ATTRIBUTES%"=="%ATTRIBUTES:l=%" echo %~1 is a reparse point. Aborting.& exit /b 1
exit /b 0
