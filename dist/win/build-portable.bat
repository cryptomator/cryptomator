@echo off
:: Builds only the portable zip of Cryptomator.
:: Usage: build-portable.bat [clean]
call "%~dp0build.bat" portable %*
