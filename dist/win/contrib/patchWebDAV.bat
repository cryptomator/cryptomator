@echo off
:: Default values for Cryptomator builds
SET LOOPBACK_ALIAS="cryptomator-vault"

cd %~dp0
powershell -NoLogo -NonInteractive -ExecutionPolicy Unrestricted -Command .\patchWebDAV.ps1^
 -LoopbackAlias %LOOPBACK_ALIAS%