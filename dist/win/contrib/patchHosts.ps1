#Requires -RunAsAdministrator

$sysdir = [Environment]::SystemDirectory
$hostsFile = "$sysdir\drivers\etc\hosts"
$aliasLine = '127.0.0.1 cryptomator-vault'

foreach ($line in Get-Content $hostsFile) {
	if ($line -eq $aliasLine){
		Write-Output 'No changes necessary'
        exit 0
    }
}

Add-Content -Path $hostsFile -Encoding ascii -Value "`r`n$aliasLine"
Write-Output 'Added alias to hosts file'
exit 0
