#Requires -RunAsAdministrator
. ./patchHosts.ps1
. ./setFileSizeLimit.ps1

Add-AliasToHost
Write-Output 'Added alias to hosts file'

Set-WebDAVFileSizeLimit
Write-Output 'Set WebDAV file size limit'

exit 0

