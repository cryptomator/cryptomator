#Requires -RunAsAdministrator
Param(
	[Parameter(Mandatory, HelpMessage="Please provide an alias for 127.0.0.1")][string] $LoopbackAlias
)

# Adds an alias for 127.0.0.1 to the hosts file
function Add-AliasToHost {
    param (
        [string]$LoopbackAlias
    )
    $sysdir = [Environment]::SystemDirectory
    $hostsFile = "$sysdir\drivers\etc\hosts"
    $aliasLine = "127.0.0.1 $LoopbackAlias"

    foreach ($line in Get-Content $hostsFile) {
        if ($line -eq $aliasLine){
            return
        }
    }

    Add-Content -Path $hostsFile -Encoding ascii -Value "`r`n$aliasLine"
}


# Sets in the registry the webclient file size limit to the maximum value
function Set-WebDAVFileSizeLimit {
    # Set variables to indicate value and key to set
    $RegistryPath = 'HKLM:\SYSTEM\CurrentControlSet\Services\WebClient\Parameters'
    $Name         = 'FileSizeLimitInBytes'
    $Value        = '0xffffffff'

    # Create the key if it does not exist
    If (-NOT (Test-Path $RegistryPath)) {
        New-Item -Path $RegistryPath -Force | Out-Null
    }

    # Now set the value
    New-ItemProperty -Path $RegistryPath -Name $Name -Value $Value -PropertyType DWORD -Force | Out-Null
}


# Changes the network provider order such that the builtin Windows webclient is always first
function Edit-ProviderOrder {
    $RegistryPath    = 'HKLM:\SYSTEM\CurrentControlSet\Control\NetworkProvider\HwOrder'
    $Name            = 'ProviderOrder'
    $WebClientString = 'webclient'

    $CurrentOrder =  (Get-ItemProperty $RegistryPath $Name).$Name

    $OrderWithoutWebclientArray = $CurrentOrder -split ',' | Where-Object {$_ -ne $WebClientString}
    $WebClientArray = @($WebClientString)

    $UpdatedOrder = ($WebClientArray + $OrderWithoutWebclientArray) -join ","
    New-ItemProperty -Path $RegistryPath -Name $Name -Value $UpdatedOrder -PropertyType String -Force | Out-Null
}


Add-AliasToHost $LoopbackAlias
Write-Output 'Ensured alias exists in hosts file'

Set-WebDAVFileSizeLimit
Write-Output 'Set WebDAV file size limit'

Edit-ProviderOrder
Write-Output 'Ensured correct provider order'

exit 0
