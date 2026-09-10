@echo off
:: Builds the normal (msi/exe installer) version and the portable zip of Cryptomator.
:: Usage: build-all.bat [clean]
call "%~dp0build.bat" all %*
