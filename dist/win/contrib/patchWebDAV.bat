@echo off
:: Default values for Cryptomator builds
::REPLACE ME

cd %~dp0
powershell -NoLogo -NonInteractive -ExecutionPolicy Unrestricted -Command .\patchWebDAV.ps1^
 -LoopbackAlias %LOOPBACK_ALIAS%