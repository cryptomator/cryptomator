#Requires -RunAsAdministrator
. ./patchHosts.ps1
. ./setFileSizeLimit.ps1
. ./patchProviderOrder.ps1

Add-AliasToHost
Write-Output 'Ensured alias exists in hosts file'

Set-WebDAVFileSizeLimit
Write-Output 'Set WebDAV file size limit'

Edit-ProviderOrder
Write-Output 'Ensured correct provider order'

exit 0

