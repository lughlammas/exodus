$ErrorActionPreference = 'Stop'
$apkRoot = (Get-ChildItem -Path (Join-Path $env:USERPROFILE 'Documents') -Directory | Where-Object { $_.Name -like 'APK*' } | Select-Object -First 1).FullName
if (-not $apkRoot) { throw 'APK* folder not found under Documents' }
$dest = Join-Path $apkRoot 'exodus'
Write-Host "Deploying to $dest"
if (Test-Path $dest) { Remove-Item -Recurse -Force $dest }
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Copy-Item -Recurse -Force (Join-Path $here 'exodus') $dest
Copy-Item -Force (Join-Path $here 'Exodus.apk') (Join-Path $env:USERPROFILE 'Desktop\Exodus.apk')
Write-Host "Source: $dest"
Get-Item (Join-Path $env:USERPROFILE 'Desktop\Exodus.apk') | Format-List FullName, Length, LastWriteTime
