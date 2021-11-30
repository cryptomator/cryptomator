# Changes the network provider order such that the builtin Windows webclient is always first
function Edit-ProviderOrder {
    $RegistryPath = 'HKLM:\SYSTEM\CurrentControlSet\Control\NetworkProvider\HwOrder'
    $Name = 'ProviderOrder'
    $WebClientString = 'webclient'

    $CurrentOrder =  (Get-ItemProperty $RegistryPath $Name).$Name

    $OrderWithoutWebclientArray = $CurrentOrder -split ',' | Where-Object {$_ -ne $WebClientString}
    $WebClientArray = @($WebClientString)

    $UpdatedOrder = ($WebClientArray + $OrderWithoutWebclientArray) -join ","
    New-ItemProperty -Path $RegistryPath -Name $Name -Value $UpdatedOrder -PropertyType String -Force | Out-Null
}