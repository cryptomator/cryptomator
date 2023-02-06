@echo off
:: see comments in file ./version170-migrate-settings.ps1

cd %~dp0
powershell -NoLogo -NonInteractive -ExecutionPolicy Unrestricted -Command .\version170-migrate-settings.ps1