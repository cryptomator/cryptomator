@echo off
:: Builds only the per-user msi for corporate machines (no admin rights required to install).
:: Usage: build-corp.bat [clean]
call "%~dp0build.bat" corp %*
