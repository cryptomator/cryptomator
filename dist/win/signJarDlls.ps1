
$certificateSHA1 = 5FC94CE149E5B511E621F53A060AC67CBD446B3A
$description = Cryptomator
$timestampUrl = 'http://timestamp.digicert.com'
$folder = ".\appdir\Cryptomator"
$tmpDir = ".\extract"
$signtool = $(Get-ChildItem "C:/Program Files (x86)/Windows Kits/10/bin/" -Recurse -File signtool.exe | Where-Object { $_.Directory.ToString().EndsWith("x64")} | Select-Object -Last 1).FullName

# import certificate

# create directory to extract every jar to
New-Item -Path $tmpDir -ItemType Directory
# iterate over all jars
Get-ChildItem -Path $folder -Recurse -File *.jar | ForEach-Object {
    $jar = Copy-Item $_ -Destination $tmpDir -PassThru
    Set-Location -Path $tmpDir
    "Extracting jar $($jar.FullName)"
    jar --file=$($_.FullName) --extract
    Get-ChildItem -Path "." -Recurse -File "*.dll" | ForEach-Object {
        # sign
        & $signtool sign /sm /tr ${timestampUrl} /td SH256 /fd SHA256 /d $description /sha1 $certificateSHA1 $_.FullName
        # update jar with signed dll
        jar --file=$($jar.FullName) --update $(Resolve-Path -Relative -Path $_)
    }
    # replace old jar with its update
    Move-Item -Path $($jar.FullName) -Destination $_ -Force
    # clear extraction dir
    Remove-Item -Path ".\*" -Force -Recurse
    Set-Location -Path ".."
}
# clean up
Remove-Item -Path $tmpDir