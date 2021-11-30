#Requires -RunAsAdministrator
. ./patchHosts.ps1

Add-AliasToHost
Write-Output 'Added alias to hosts file'


exit 0

