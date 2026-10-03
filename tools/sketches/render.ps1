param([Parameter(Mandatory)][string]$Stamp)
# Renders docs/sketches/sketches.html (one page per #hash) to PNG with headless Edge,
# then cuts off the empty space at the bottom (crop.py).
#   powershell -File tools\sketches\render.ps1 -Stamp 2026-10-03_0948
$edge = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"
if (-not (Test-Path $edge)) { $edge = "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe" }
$dir = Resolve-Path "$PSScriptRoot\..\..\docs\sketches"
$url = 'file:///' + ("$dir\sketches.html" -replace '\\', '/')
$i = 1
foreach ($p in 'home', 'search', 'product', 'cart', 'checkout', 'seller') {
  $out = "$dir\$i-$p" + "_$Stamp.png"
  & $edge --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --window-size=1200,860 "--screenshot=$out" "$url#$p" 2>$null | Out-Null
  $i++
}
Start-Sleep -Seconds 2
python "$PSScriptRoot\crop.py" $dir $Stamp
