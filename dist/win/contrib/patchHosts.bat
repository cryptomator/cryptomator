@echo off
cd %~dp0
powershell -NoLogo -NonInteractive -ExecutionPolicy Unrestricted -Command .\patchHosts.ps1