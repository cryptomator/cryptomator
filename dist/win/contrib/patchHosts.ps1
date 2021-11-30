# Adds for address 127.0.0.1 the 'cryptomator-vault' alias to the hosts file
function Add-AliasToHost {
    $sysdir = [Environment]::SystemDirectory
    $hostsFile = "$sysdir\drivers\etc\hosts"
    $aliasLine = '127.0.0.1 cryptomator-vault'

    foreach ($line in Get-Content $hostsFile) {
        if ($line -eq $aliasLine){
			return
        }
    }

    Add-Content -Path $hostsFile -Encoding ascii -Value "`r`n$aliasLine"
}