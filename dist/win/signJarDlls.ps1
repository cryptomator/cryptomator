<#
1. Select jar file
2. extract jar to own directory
3. Sign everything
4. Update dlls in the jar
#>

New-Item -Path ".\extract" -ItemType Directory
Get-ChildItem -Path "." -File *.jar | ForEach-Object {
    $jar = Copy-Item $_ -Destination ".\extract" -PassThru
    Set-Location -Path ".\extract"
    "Extracting jar $($jar.FullName)"
    jar --file=$($_.FullName) --extract
    Get-ChildItem -Path "." -Recurse -File "*.dll" | ForEach-Object {
        <# pipe into signtool, here we are just writing something into the file #>
        Set-Content -Path $_ -Value "Hello"
        jar --file=$($jar.FullName) --update $(Resolve-Path -Relative -Path $_)
    }
    Move-Item -Path $($jar.FullName) -Destination $_ -Force
    Remove-Item -Path ".\*" -Force -Recurse
    Set-Location -Path ".."
}
Remove-Item -Path ".\extract"