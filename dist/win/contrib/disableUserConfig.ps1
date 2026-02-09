# PowerShell script to disable user configuration
# This script is executed as a Custom Action during MSI installation
# It deletes the file .package, effectively disabling user specific jpackage configuration.
# NOTE: This file must be located in the same directory as set in the MSI property INSTALLDIR

try {

    # Determine  file path
    $packageFile = Join-Path $PSScriptRoot 'app\.package'

    #check if file exists
    if (Test-Path -Path $packageFile) {
        Write-Host "Deleting file: $packageFile"
        Remove-Item -Path $packageFile -Force -ErrorAction Stop
    } else {
        Write-Host "File not found: $packageFile. Skipping deletion."
    }

    exit 0
}
catch {
    Write-Error "Error deleting package file: $_"
    exit 1
}