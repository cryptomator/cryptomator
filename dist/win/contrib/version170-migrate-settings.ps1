# This script migrates Cryptomator settings for all local users on Windows in case the users uses custom directories as mountpoint
# See also https://github.com/cryptomator/cryptomator/pull/2654.
#
# TODO: This script should be evaluated in a yearly interval if it is still needed and if not, should be removed
#
#Requires -RunAsAdministrator

#Get all active, local user profiles
$profileList = 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList'
Get-ChildItem $profileList | ForEach-Object {
    $profilePath =  $_.GetValue("ProfileImagePath") 
    $settingsPath = "$profilePath\AppData\Roaming\Cryptomator\settings.json"
    if(!(Test-Path -Path $settingsPath -PathType Leaf)) {
        #No settings file, nothing to do.
        return;
    }
    $settings = Get-Content -Path $settingsPath | ConvertFrom-Json
    if($settings.preferredVolumeImpl -ne "FUSE") {
        #Fuse not used, nothing to do
        return;
    }

    #check if customMountPoints are used
    $atLeastOneCustomPath = $false;
    foreach ($vault in $settings.directories){
        $atLeastOneCustomPath = $atLeastOneCustomPath -or ($vault.useCustomMountPath -eq "True")
    }

    #if so, use WinFsp Local Drive
    if( $atLeastOneCustomPath ) {
        Add-Member -Force -InputObject $settings -Name "mountService" -Value "org.cryptomator.frontend.fuse.mount.WinFspMountProvider" -MemberType NoteProperty
        $newSettings  = $settings | Select-Object * -ExcludeProperty "preferredVolumeImpl"
        ConvertTo-Json $newSettings | Set-Content -Path $settingsPath
    }
}
