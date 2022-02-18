@echo off
cd %~dp0
powershell -NoLogo -NonInteractive -ExecutionPolicy Unrestricted -Command .\patchWebDAV.ps1