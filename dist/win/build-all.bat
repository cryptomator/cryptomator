@echo off
:: Builds the normal (msi/exe installer) version, the portable zip and the per-user corp msi of Cryptomator.
:: Usage: build-all.bat [clean]
call "%~dp0build.bat" all %*
