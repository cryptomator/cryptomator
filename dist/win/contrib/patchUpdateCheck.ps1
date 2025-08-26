# PowerShell script to modify Cryptomator.cfg to set disableUpdateCheck property
# This script is executed as a Custom Action during MSI installation
# It reads the DISABLEUPDATECHECK property from the MSI and modifies the .cfg file accordingly

param(
    [string]$InstallDir = $env:INSTALLDIR,
    [string]$DisableUpdateCheck = $env:DISABLEUPDATECHECK
)

try {
    # Log parameters for debugging (visible in MSI verbose logs)
    Write-Host "InstallDir: $InstallDir"
    Write-Host "DisableUpdateCheck: $DisableUpdateCheck"
    
    # Determine the .cfg file path
    $cfgFile = Join-Path $InstallDir "app\Cryptomator.cfg"
    
    if (-not (Test-Path $cfgFile)) {
        Write-Error "Configuration file not found at: $cfgFile"
        exit 1
    }
    
    # Read the current configuration
    $content = Get-Content $cfgFile -Raw
    
    # Parse DisableUpdateCheck value (handle various input formats)
    $shouldDisable = $false
    if ($DisableUpdateCheck) {
        $DisableUpdateCheck = $DisableUpdateCheck.Trim().ToLower()
        $shouldDisable = $DisableUpdateCheck -eq "true" -or $DisableUpdateCheck -eq "1" -or $DisableUpdateCheck -eq "yes"
    }
    
    Write-Host "Setting cryptomator.disableUpdateCheck to: $shouldDisable"
    
    # Check if [JavaOptions] section exists
    if ($content -notmatch '\[JavaOptions\]') {
        Write-Error "[JavaOptions] section not found in configuration file"
        exit 1
    }
    
    # Remove any existing disableUpdateCheck option to avoid duplicates
    $content = $content -replace 'java-options=-Dcryptomator\.disableUpdateCheck=(true|false)\r?\n', ''
    
    # Add the new option based on the property value
    # We need to add it after [JavaOptions] but before [ArgOptions] or end of file
    if ($shouldDisable) {
        # Find the position to insert the new option
        if ($content -match '(\[JavaOptions\])([\s\S]*?)(\[ArgOptions\]|\z)') {
            $beforeSection = $matches[1]
            $javaOptionsContent = $matches[2]
            $afterSection = $matches[3]
            
            # Add our option at the end of JavaOptions section
            $newOption = "java-options=-Dcryptomator.disableUpdateCheck=true`r`n"
            $content = $beforeSection + $javaOptionsContent + $newOption + $afterSection
        }
    }
    # If shouldDisable is false, we don't need to add anything (default behavior)
    
    # Write the modified content back
    Set-Content -Path $cfgFile -Value $content -NoNewline
    
    Write-Host "Successfully updated $cfgFile"
    exit 0
}
catch {
    Write-Error "Error modifying configuration file: $_"
    exit 1
}