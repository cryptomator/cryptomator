$settingsPath = 'C:\Users\Arbeit\AppData\Roaming\Cryptomator-Dev\settings1617.json'
$settings = Get-Content -Path $settingsPath | ConvertFrom-Json
$atLeastOneCustomPath = $false;
foreach ($vault in $settings.directories){
    $atLeastOneCustomPath = $atLeastOneCustomPath -or ($vault.useCustomMountPath -eq "True")
}

if( $atLeastOneCustomPath -and ($settings.preferredVolumeImpl -eq "FUSE")) {
    Add-Member -Force -InputObject $settings -Name "mountService" -Value "org.cryptomator.frontend.fuse.mount.WinFspMountProvider" -MemberType NoteProperty
    $newSettings  = $settings | Select-Object * -ExcludeProperty "preferredVolumeImpl"
    ConvertTo-Json $newSettings | Set-Content -Path $settingsPath
}
