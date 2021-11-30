# taken from https://devblogs.microsoft.com/powershell-community/how-to-update-or-add-a-registry-key-value-with-powershell/
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