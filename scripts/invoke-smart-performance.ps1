param(
    [Parameter(Mandatory=$true)][string]$Runtime,
    [string]$PlayerJar,
    [int]$Houses=1,
    [int]$BlocksPerTick=4,
    [int]$ServerPort=25673,
    [string]$ReplayWorld
)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
if(Test-Path -LiteralPath $Runtime){throw 'Use a fresh performance runtime; earlier measurements are retained'}
if(!$PlayerJar){$PlayerJar=Join-Path $repo 'build/libs/iron-age-architecture-0.1.16.110021.jar'}
$template='D:/IronAgeArchitecture/manual-testing/jetties-0.1.2-final/core'
New-Item -ItemType Directory -Path $Runtime,(Join-Path $Runtime 'mods'),(Join-Path $Runtime 'config') | Out-Null
foreach($file in @('forge-1.10.2-12.18.3.2511-universal.jar','minecraft_server.1.10.2.jar','libraries')){Copy-Item -LiteralPath (Join-Path $template $file) -Destination $Runtime -Recurse}
Copy-Item -LiteralPath $PlayerJar,(Join-Path $repo 'build/probe/architecture-probe.jar') -Destination (Join-Path $Runtime 'mods')
"building {`n S:mode=automatic`n B:retainLegacyWallIds=false`n B:retainLegacyThatchIds=false`n}" | Set-Content -LiteralPath (Join-Path $Runtime 'config/ironagearchitecture.cfg') -Encoding ascii
if($ReplayWorld){Copy-Item -LiteralPath $ReplayWorld -Destination (Join-Path $Runtime 'world') -Recurse;Copy-Item -LiteralPath (Join-Path $repo 'run/config/ironagearchitecture.cfg') -Destination (Join-Path $Runtime 'config/ironagearchitecture.cfg') -Force}
'eula=true' | Set-Content -LiteralPath (Join-Path $Runtime 'eula.txt') -Encoding ascii
@"
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
server-port=$ServerPort
max-tick-time=120000
"@ | Set-Content -LiteralPath (Join-Path $Runtime 'server.properties') -Encoding ascii
$digest=(Get-FileHash -LiteralPath $PlayerJar).Hash.ToLowerInvariant()
$args=@('-Xms1G','-Xmx2G','-Diaa.probe=true','-Diaa.performance=true',('-Diaa.performanceHouses='+$Houses),('-Diaa.performanceRate='+$BlocksPerTick),('-Diaa.runtimeSha256='+$digest),('-Diaa.receipt='+(Join-Path $Runtime 'performance.json')),'-jar','forge-1.10.2-12.18.3.2511-universal.jar','nogui')
if($ReplayWorld){$args=@('-Diaa.replay=true',('-Diaa.replayLevel='+(Join-Path $Runtime 'world/level.dat')))+$args}
Push-Location $Runtime
try{& 'C:/Users/johnb/.gradle/jdks/temurin-8-amd64-windows.2/bin/java.exe' @args *> 'performance-console.log';if($LASTEXITCODE -ne 0){throw 'Performance runtime failed; inspect its preserved log'};Get-Content 'performance.json'}finally{Pop-Location}
