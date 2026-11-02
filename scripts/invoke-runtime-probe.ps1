param(
    [ValidateSet('core','reload','furniture','disabled','legacy','legacy-daub')][string]$Mode='core',
    [string]$RuntimeRoot='D:\IronAgeArchitecture\manual-testing\jetties-0.1.2',
    [string]$ServerTemplate='D:\IronAgeFurniture\manual-testing\sylvester-iaf-0.3.0.110021-20260921',
    [string]$Java8='C:\Users\johnb\.gradle\jdks\temurin-8-amd64-windows.2\bin\java.exe'
)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$version=((Select-String -LiteralPath (Join-Path $repo 'gradle.properties') -Pattern '^mod_version=').Line -split '=',2)[1]
$runtimeJar='iron-age-architecture-'+$version+'.jar'
$folder=Join-Path $RuntimeRoot $(if($Mode -eq 'reload'){'core'}else{$Mode})
New-Item -ItemType Directory -Force -Path $folder,(Join-Path $folder 'mods') | Out-Null
$forge='forge-1.10.2-12.18.3.2511-universal.jar'
foreach($name in @($forge,'minecraft_server.1.10.2.jar','libraries')) {
    if(!(Test-Path -LiteralPath (Join-Path $folder $name))) {
        Copy-Item -LiteralPath (Join-Path $ServerTemplate $name) -Destination $folder -Recurse
    }
}
Copy-Item -LiteralPath (Join-Path $repo ('build/libs/'+$runtimeJar)),(Join-Path $repo 'build/probe/architecture-probe.jar') -Destination (Join-Path $folder 'mods')
if($Mode -eq 'legacy') {
    if(Test-Path -LiteralPath (Join-Path $folder 'world')) {throw 'Legacy probe requires a fresh disposable world destination'}
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $staging=Join-Path $folder 'initial-preview-source'
    [System.IO.Compression.ZipFile]::ExtractToDirectory('D:\IronAgeArchitecture\candidates\0.1.0.110021-initial-cottage-kit\Architecture Cottage Preview.zip',$staging)
    Copy-Item -LiteralPath (Join-Path $staging 'Architecture Cottage Preview') -Destination (Join-Path $folder 'world') -Recurse
}
if($Mode -eq 'legacy-daub') {
    if(Test-Path -LiteralPath (Join-Path $folder 'world')) {throw 'Legacy wall probe requires a fresh disposable destination'}
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $staging=Join-Path $folder 'wall-preview-source'
    [System.IO.Compression.ZipFile]::ExtractToDirectory('D:\IronAgeArchitecture\candidates\0.1.1.110021-wattle-and-daub\Wattle and Daub Preview.zip',$staging)
    Copy-Item -LiteralPath (Join-Path $staging 'Wattle and Daub Preview') -Destination (Join-Path $folder 'world') -Recurse
}
if($Mode -eq 'furniture') {
    Copy-Item -LiteralPath (Join-Path $ServerTemplate 'mods/iron-age-furniture-0.3.0.110021.jar') -Destination (Join-Path $folder 'mods')
}
if($Mode -eq 'disabled') {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    @'
blocks {
    B:cob=false
    B:timberFrames=false
    B:thatch=false
    B:slate=false
}
'@ | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
'eula=true' | Set-Content -LiteralPath (Join-Path $folder 'eula.txt') -Encoding ascii
@'
level-type=FLAT
generator-settings=3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;
level-name=world
level-seed=137
gamemode=1
generate-structures=false
spawn-npcs=false
spawn-animals=false
spawn-monsters=false
online-mode=false
server-ip=127.0.0.1
server-port=25671
max-tick-time=120000
'@ | Set-Content -LiteralPath (Join-Path $folder 'server.properties') -Encoding ascii
$receipt=Join-Path $folder ($Mode+'-receipt.json')
$args=@('-Xmx2G','-Diaa.probe=true',('-Diaa.receipt='+$receipt))
$args+=('-Diaa.runtimeSha256='+(Get-FileHash -LiteralPath (Join-Path $folder ('mods/'+$runtimeJar)) -Algorithm SHA256).Hash.ToLowerInvariant())
if($Mode -eq 'reload') {$args+='-Diaa.reload=true'}
if($Mode -eq 'legacy') {$args+='-Diaa.legacy=true'}
if($Mode -eq 'legacy-daub') {$args+='-Diaa.legacyDaub=true'}
if($Mode -eq 'disabled') {$args+='-Diaa.expectedVariants=0'}
$args+=@('-jar',$forge,'nogui')
Push-Location $folder
try {
    $started=Get-Date
    & $Java8 @args *> ($Mode+'-console.log')
    if($LASTEXITCODE -ne 0) {throw "Forge exited $LASTEXITCODE; inspect $folder\$Mode-console.log"}
    if(!(Test-Path -LiteralPath $receipt)) {throw "No probe receipt; inspect $folder\$Mode-console.log"}
    if((Get-Item -LiteralPath $receipt).LastWriteTime -lt $started) {throw 'Stale probe receipt'}
    Get-Content -LiteralPath $receipt
} finally {Pop-Location}
