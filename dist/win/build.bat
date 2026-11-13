@echo off
:: Thin wrapper around build.ps1, which defines the defaults for all parameters.
:: Usage: build.bat [clean]

SET CLEAN=
IF "%~1"=="clean" SET CLEAN=-Clean

pwsh -NoLogo -NoProfile -ExecutionPolicy Unrestricted -Command .\build.ps1 %CLEAN%
