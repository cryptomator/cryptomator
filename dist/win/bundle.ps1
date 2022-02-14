### Creates the Wix Bootstrapper application

& "$env:WIX\bin\candle.exe" bundlewithWinfsp.wxs -ext WixBalExtension -out tmp\
& "$env:WIX\bin\light.exe" -b . .\tmp\BundlewithWinfsp.wixobj -ext WixBalExtension -out installer\CryptomatorBundle.exe

#Download WinFSP on Demand?