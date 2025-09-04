# PowerShell script to modify Cryptomator.cfg to set disableUpdateCheck property
# This script is executed as a Custom Action during MSI installation
# If the DisableUpdateCheck parameter is set to true, it disables the update check in Cryptomator by modifying the Cryptomator.cfg file.
# NOTE: This file must be located in the same directory as set in the MSI property INSTALLDIR

param(
    [Parameter(Mandatory)][string]$DisableUpdateCheck
)

try {
    # Log parameters for debugging (visible in MSI verbose logs)
    Write-Host "DisableUpdateCheck: $DisableUpdateCheck"

    # Parse DisableUpdateCheck value (handle various input formats)
    $shouldDisable = $false
    if ($DisableUpdateCheck) {
        $DisableUpdateCheck = $DisableUpdateCheck.Trim().ToLower()
        $shouldDisable = ($DisableUpdateCheck -eq 'true') -or ($DisableUpdateCheck -eq '1') -or ($DisableUpdateCheck -eq 'yes')
    }

    Write-Host "Setting cryptomator.disableUpdateCheck to: $shouldDisable"
    if (-not $shouldDisable) {
        Write-Host 'Disable-Update-Check property is by default "false". Skipping config modification.'
        exit 0
    }

    # Determine the .cfg file path
    $cfgDir = Join-Path $PSScriptRoot 'app'
    $cfgFiles = Get-ChildItem -Path $cfgDir -Filter '*.cfg' -File

    if ($cfgFiles.Count -eq 0) {
        Write-Error "No .cfg file found in directory: $cfgDir"
        exit 1
    }

    foreach ($file in $cfgFiles) {
        $cfgFile = $file.FullName
        Write-Host "Modifying configuration file: $cfgFile"
        # Read the current configuration
        $content = Get-Content $cfgFile -Raw -ErrorAction Stop

        # Add the new option based on the property value
        # Use regular expressions substitutions to replace the property
        $searchExpression = '(?<Prefix>java-options=-Dcryptomator\.disableUpdateCheck)=false'
        $replacementExpression = '${Prefix}=true'
        $content = $content -replace $searchExpression,$replacementExpression

        # Write the modified content back
        Set-Content -Path $cfgFile -Value $content -NoNewline
        Write-Host "Successfully updated $cfgFile"
    }

    exit 0
}
catch {
    Write-Error "Error modifying configuration file: $_"
    exit 1
}