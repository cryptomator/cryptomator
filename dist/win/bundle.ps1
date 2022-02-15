
## Dedup with build.ps1
$buildDir = Split-Path -Parent $PSCommandPath
$version = $(mvn -f $buildDir/../../pom.xml help:evaluate -Dexpression="project.version" -q -DforceStdout)
$semVerNo = $version -replace '(\d\.\d\.\d).*','$1'

### Creates the Wix Bootstrapper application

& "$env:WIX\bin\candle.exe" bundlewithWinfsp.wxs -ext WixBalExtension -out tmp\ -dAppVersion="$semVerNo"
& "$env:WIX\bin\light.exe" -b . .\tmp\BundlewithWinfsp.wixobj -ext WixBalExtension -out installer\CryptomatorBundle.exe

#Download WinFSP on Demand?