# PowerShell script to modify Cryptomator.cfg to set disableUpdateCheck property
# This script is executed as a Custom Action during MSI installation
# It reads the DISABLEUPDATECHECK property from the MSI and modifies the .cfg file accordingly
# NOTE: This script only modifies the config, if the config already contains the cryptomator.disableUpdateCheck property!

param(
    [Parameter(Mandatory)][string]$InstallDir,
    [Parameter(Mandatory)][string]$DisableUpdateCheck
)

try {
    # Log parameters for debugging (visible in MSI verbose logs)
    Write-Host "InstallDir: $InstallDir"
    Write-Host "DisableUpdateCheck: $DisableUpdateCheck"
    
    # Parse DisableUpdateCheck value (handle various input formats)
    $shouldDisable = $false
    if ($DisableUpdateCheck) {
        $DisableUpdateCheck = $DisableUpdateCheck.Trim().ToLower()
        $shouldDisable = ($DisableUpdateCheck -eq 'true') -or ($DisableUpdateCheck -eq '1') -or ($DisableUpdateCheck -eq 'yes')
    }

    Write-Host "Setting cryptomator.disableUpdateCheck to: $shouldDisable"
    if (-not $shouldDisable) {
        Write-Host "Disable-Update-Check property is set to 'false'. Skipping config modification, nothing to do."
        exit 0
    }

    # Determine the .cfg file path
    $cfgFile = Join-Path $($InstallDir.Trim()) "app\Cryptomator.cfg"

    if (-not (Test-Path $cfgFile)) {
        Write-Error "Configuration file not found at: $cfgFile"
        exit 1
    }
    
    # Read the current configuration
    $content = Get-Content $cfgFile -Raw
       
    # Add the new option based on the property value
    # Use regular expressions substitutions to replace the property
    $searchExpression = '(?<Prefix>java-options=-Dcryptomator\.disableUpdateCheck)=false'
    $replacementExpression = '${Prefix}=true'
    $content = $content -replace $searchExpression,$replacementExpression

    # Write the modified content back
    Set-Content -Path $cfgFile -Value $content -NoNewline
    Write-Host "Successfully updated $cfgFile"
    exit 0
}
catch {
    Write-Error "Error modifying configuration file: $_"
    exit 1
}