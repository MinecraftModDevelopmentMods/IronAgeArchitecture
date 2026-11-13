param(
    [ValidateSet('core','reload','furniture','disabled','legacy','legacy-daub','legacy-jetties','legacy-fascia','iron-age','late-medieval','mixed-eras','no-eras','invalid-era','windows-disabled','clay-disabled','clay-only','manual','hybrid','automatic','legacy-fireplaces','legacy-smart','cob-disabled','cob-only')][string]$Mode='core',
    [string]$RuntimeRoot='D:\IronAgeArchitecture\manual-testing\rounded-smart-cob-0.1.15',
    [string]$ServerTemplate='D:\IronAgeArchitecture\manual-testing\jetties-0.1.2-final\core',
    [string]$FurnitureJar='D:\IronAgeArchitecture\manual-testing\jetties-0.1.2-final\furniture\mods\iron-age-furniture-0.3.0.110021.jar',
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
    Copy-Item -LiteralPath $FurnitureJar -Destination (Join-Path $folder 'mods')
}
if($Mode -eq 'legacy-jetties') {
    if(Test-Path -LiteralPath (Join-Path $folder 'world')) {throw 'Legacy jetty probe requires a fresh disposable destination'}
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $staging=Join-Path $folder 'jetty-preview-source'
    [System.IO.Compression.ZipFile]::ExtractToDirectory('D:\IronAgeArchitecture\candidates\0.1.2.110021-jettied-storeys\Jettied Street Preview.zip',$staging)
    Copy-Item -LiteralPath (Join-Path $staging 'Jettied Street Preview') -Destination (Join-Path $folder 'world') -Recurse
}
if($Mode -eq 'legacy-fireplaces') {
    if(Test-Path -LiteralPath (Join-Path $folder 'world')) {throw 'Legacy fireplace probe requires a fresh destination'}
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $staging=Join-Path $folder 'fireplace-preview-source'
    [System.IO.Compression.ZipFile]::ExtractToDirectory('D:\IronAgeArchitecture\manual-testing\rye-fireplaces-0.1.13\Rye Fireplace Preview.zip',$staging)
    Copy-Item -LiteralPath (Join-Path $staging 'Rye Fireplace Preview') -Destination (Join-Path $folder 'world') -Recurse
}
if($Mode -eq 'legacy-smart') {
    if(Test-Path -LiteralPath (Join-Path $folder 'world')) {throw 'Legacy smart wall probe requires a fresh destination'}
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $staging=Join-Path $folder 'smart-preview-source'
    [System.IO.Compression.ZipFile]::ExtractToDirectory('D:\IronAgeArchitecture\manual-testing\smart-walls-0.1.14-surface-fix\Smart Walls Surface Preview.zip',$staging)
    Copy-Item -LiteralPath (Join-Path $staging 'Smart Walls Surface Preview') -Destination (Join-Path $folder 'world') -Recurse
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    # An existing alpha config without the new setting must retain every old identity.
    "building {`n S:mode=automatic`n}" | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
if($Mode -eq 'legacy-fascia') {
    if(Test-Path -LiteralPath (Join-Path $folder 'world')) {throw 'Legacy fascia probe requires a fresh disposable destination'}
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $staging=Join-Path $folder 'fascia-preview-source'
    [System.IO.Compression.ZipFile]::ExtractToDirectory('D:\IronAgeArchitecture\manual-testing\rye-roof-trim-0.1.10\Rye Roof Trim Preview.zip',$staging)
    Copy-Item -LiteralPath (Join-Path $staging 'Rye Roof Trim Preview') -Destination (Join-Path $folder 'world') -Recurse
}
$eraModes=@{'iron-age'=@('iron_age');'late-medieval'=@('late_medieval_tudor');'mixed-eras'=@('iron_age','late_medieval_tudor');'no-eras'=@();'invalid-era'=@('misspelled_era')}
if($eraModes.ContainsKey($Mode)) {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    ("recipes {`n S:survivalEras <`n"+($eraModes[$Mode] -join "`n")+"`n >`n}`n") | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
if($Mode -in @('manual','hybrid','automatic','legacy','legacy-daub','legacy-jetties','legacy-fascia','legacy-fireplaces')) {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    $wallMode=if($Mode -like 'legacy*') {'manual'} else {$Mode}
    $legacy=($Mode -like 'legacy*').ToString().ToLowerInvariant()
    ("building {`n S:mode="+$wallMode+"`n B:retainLegacyWallIds="+$legacy+"`n}") | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
if($Mode -eq 'windows-disabled') {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    "blocks {`n B:leadedWindows=false`n}" | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
if($Mode -eq 'clay-disabled') {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    "blocks {`n B:clayTiles=false`n}" | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
if($Mode -eq 'cob-disabled') {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    "blocks {`n B:smartCob=false`n}" | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
if($Mode -eq 'cob-only') {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    @'
blocks {
 B:cob=true
 B:smartCob=true
 B:timberFrames=false
 B:leadedWindows=false
 B:thatch=false
 B:slate=false
 B:clayTiles=false
 B:fireplaces=false
}
'@ | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
if($Mode -in @('disabled','clay-only')) {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    $clayTiles=($Mode -eq 'clay-only').ToString().ToLowerInvariant()
    @"
blocks {
    B:cob=false
    B:timberFrames=false
    B:thatch=false
    B:slate=false
    B:clayTiles=$clayTiles
    B:fireplaces=false
}
"@ | Set-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Encoding ascii
}
if($Mode -notin @('core','reload','manual','hybrid','automatic','legacy','legacy-daub','legacy-jetties','legacy-fascia','legacy-fireplaces','legacy-smart')) {
    New-Item -ItemType Directory -Force -Path (Join-Path $folder 'config') | Out-Null
    Add-Content -LiteralPath (Join-Path $folder 'config/ironagearchitecture.cfg') -Value "building {`n S:mode=automatic`n B:retainLegacyWallIds=false`n}" -Encoding ascii
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
if($Mode -eq 'legacy-jetties') {$args+='-Diaa.legacyJetties=true'}
if($Mode -eq 'legacy-fascia') {$args+='-Diaa.legacyFascia=true'}
if($Mode -eq 'legacy-fireplaces') {$args+='-Diaa.legacyFireplaces=true'}
if($Mode -eq 'legacy-smart') {$args+='-Diaa.legacySmart=true'}
if($Mode -eq 'core') {$args+=@('-Diaa.eraMatrix=true','-Diaa.recipeMatrix=true')}
if($Mode -eq 'disabled') {$args+='-Diaa.expectedVariants=0'}
if($Mode -eq 'clay-only') {$args+=@('-Diaa.expectedVariants=16','-Diaa.clayOnly=true','-Diaa.recipeMatrix=true')}
if($Mode -eq 'cob-only') {$args+=@('-Diaa.expectedVariants=8','-Diaa.cobOnly=true','-Diaa.recipeMatrix=true')}
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
