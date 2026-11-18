param(
    [string]$Runtime='D:\IronAgeArchitecture\manual-testing\smart-thatch-0.1.16\native-client-reviewed',
    [string]$PreviewName='Smart Thatch Preview',
    [switch]$SurfaceCheck,
    [switch]$ThatchInteraction,
    [switch]$Workbench,
    [string]$PlayerJar,
    [string]$CitySource,
    [int]$CityHouses=64,
    [switch]$ColdCity,
    [int]$CpuCount=4,
    [int]$CityViewDistance=6,
    [int]$CityTimeoutSeconds=300,
    [string]$ServerTemplate='D:\IronAgeArchitecture\manual-testing\jetties-0.1.2-final\core',
    [string]$GradleHome='D:\GradleCaches\minecraft-mods-shared',
    [string]$AssetsDirectory='',
    [string]$Java8='C:\Users\johnb\.gradle\jdks\temurin-8-amd64-windows.2\bin\java.exe'
)
$ErrorActionPreference='Stop'
if (Test-Path -LiteralPath (Join-Path $Runtime "saves/$PreviewName")) {
    throw 'Use a fresh runtime or preview name to keep the earlier save.'
}
$repo=Split-Path $PSScriptRoot -Parent
$version=((Select-String -LiteralPath (Join-Path $repo 'gradle.properties') -Pattern '^mod_version=').Line -split '=',2)[1]
$runtimeJar='iron-age-architecture-'+$version+'.jar'
$slime=Join-Path $GradleHome 'caches/minecraftforge/forgegradle/slime-launcher/cache/net/minecraftforge/forge/1.10.2-12.18.3.2511'
$vanilla=Join-Path $GradleHome 'caches/minecraftforge/forgegradle/mavenizer/caches/minecraft_tasks/1.10.2/client.jar'
$metadata=Get-Content -LiteralPath (Join-Path $slime 'metadata/minecraft/version.json') -Raw | ConvertFrom-Json
if(!$AssetsDirectory) {
    $AssetsDirectory=@((Join-Path $GradleHome 'caches/minecraft/assets'),
        (Join-Path $env:USERPROFILE '.gradle/caches/minecraft/assets'),
        (Join-Path $env:APPDATA '.minecraft/assets')) | Where-Object {Test-Path -LiteralPath (Join-Path $_ 'indexes/1.10.json')} | Select-Object -First 1
}
if(!$AssetsDirectory -or !(Test-Path -LiteralPath (Join-Path $AssetsDirectory 'indexes/1.10.json'))) {throw 'Minecraft 1.10 asset index is missing'}
if((Get-FileHash -LiteralPath (Join-Path $AssetsDirectory 'indexes/1.10.json') -Algorithm SHA1).Hash -ne $metadata.assetIndex.sha1) {throw 'Minecraft asset index checksum mismatch'}
New-Item -ItemType Directory -Force -Path $Runtime,(Join-Path $Runtime 'mods'),(Join-Path $Runtime 'libraries') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $Runtime 'config') | Out-Null
'building {
 S:mode=automatic
 B:retainLegacyWallIds=false
 B:retainLegacyThatchIds=true
}' | Set-Content -LiteralPath (Join-Path $Runtime 'config/ironagearchitecture.cfg') -Encoding ascii
if(!$PlayerJar){$PlayerJar=Join-Path $repo ('build/libs/'+$runtimeJar)}
Copy-Item -LiteralPath $PlayerJar,(Join-Path $repo 'build/probe/architecture-probe.jar') -Destination (Join-Path $Runtime 'mods')
if($CitySource){New-Item -ItemType Directory -Force -Path (Join-Path $Runtime 'saves') | Out-Null; Copy-Item -LiteralPath $CitySource -Destination (Join-Path $Runtime ('saves/'+$PreviewName)) -Recurse}
$forge=Join-Path $ServerTemplate 'forge-1.10.2-12.18.3.2511-universal.jar'
$classpath=[System.Collections.Generic.List[string]]::new()
$classpath.Add($vanilla);$classpath.Add($forge)
Get-ChildItem -LiteralPath (Join-Path $ServerTemplate 'libraries') -Recurse -Filter '*.jar' | ForEach-Object {$classpath.Add($_.FullName)}
foreach($library in $metadata.libraries) {
    $allowed=!$library.rules
    foreach($rule in $library.rules) {
        if(!$rule.os -or $rule.os.name -eq 'windows') {$allowed=$rule.action -eq 'allow'}
    }
    if(!$allowed -or !$library.downloads.artifact) {continue}
    $artifact=$library.downloads.artifact
    $filename=Split-Path $artifact.path -Leaf
    if($classpath | Where-Object {(Split-Path $_ -Leaf) -eq $filename}) {continue}
    $coordinate=$library.name.Split(':')
    $cache=Join-Path $GradleHome ('caches/modules-2/files-2.1/'+$coordinate[0]+'/'+$coordinate[1]+'/'+$coordinate[2])
    $found=if(Test-Path -LiteralPath $cache) {Get-ChildItem -LiteralPath $cache -Recurse -Filter $filename | Select-Object -First 1}
    if($found) {$candidate=$found.FullName}
    else {
        $candidate=Join-Path $Runtime ('libraries/'+$filename)
        if(!(Test-Path -LiteralPath $candidate)) {Invoke-WebRequest -Uri $artifact.url -OutFile $candidate}
    }
    if((Get-FileHash -LiteralPath $candidate -Algorithm SHA1).Hash -ne $artifact.sha1) {throw "Library checksum mismatch: $filename"}
    $classpath.Add($candidate)
}
@'
pauseOnLostFocus:false
showInventoryAchievementHint:false
renderDistance:8
guiScale:2
fancyGraphics:true
maxFps:120
enableVsync:false
'@ | Set-Content -LiteralPath (Join-Path $Runtime 'options.txt') -Encoding ascii
$digest=(Get-FileHash -LiteralPath (Join-Path $Runtime ('mods/'+$runtimeJar)) -Algorithm SHA256).Hash.ToLowerInvariant()
$args=@('-Xmx2G',('-Djava.library.path='+(Join-Path $slime 'natives')),'-Diaa.probe=true','-Diaa.clientProbe=true','-Diaa.keepRunning=true',('-Diaa.previewName='+$PreviewName),('-Diaa.runtimeSha256='+$digest),'-cp',($classpath -join ';'),
    'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLTweaker',
    '--username','ArchitecturePreview','--version','1.10.2','--gameDir',$Runtime,'--assetsDir',$AssetsDirectory,
    '--assetIndex','1.10','--uuid','00000000000000000000000000000001','--accessToken','0','--userType','legacy','--versionType','release','--width','1280','--height','800')
if($CitySource){$args=@('-Diaa.cityClient=true',('-Diaa.performanceHouses='+$CityHouses))+$args}
if($ColdCity){$args=@('-Diaa.cityCold=true',('-Diaa.cityTimeoutSeconds='+$CityTimeoutSeconds),('-XX:ActiveProcessorCount='+$CpuCount),('-Diaa.cityViewDistance='+$CityViewDistance))+$args}
if($SurfaceCheck) {$args=@('-Diaa.surfacePreview=true')+$args}
if($Workbench){$args=@('-Diaa.workbenchPreview=true')+$args}
if($ThatchInteraction){$args=@('-Diaa.thatchInteraction=true')+$args}
Push-Location $Runtime
try {
    if($ColdCity){
        $psi=[Diagnostics.ProcessStartInfo]::new($Java8);$psi.WorkingDirectory=$Runtime;$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true;$psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true;foreach($argument in $args){$psi.ArgumentList.Add($argument)}
        $game=[Diagnostics.Process]::Start($psi);$game.ProcessorAffinity=[IntPtr]0x55
        $output=[IO.File]::Open((Join-Path $Runtime 'native-client-console.log'),[IO.FileMode]::Create,[IO.FileAccess]::Write,[IO.FileShare]::ReadWrite);$errors=[IO.File]::Open((Join-Path $Runtime 'native-client-errors.log'),[IO.FileMode]::Create,[IO.FileAccess]::Write,[IO.FileShare]::ReadWrite)
        try{$outTask=$game.StandardOutput.BaseStream.CopyToAsync($output);$errTask=$game.StandardError.BaseStream.CopyToAsync($errors);[long]$peakMemory=0;while(!$game.WaitForExit(1000)){$game.Refresh();$peakMemory=[Math]::Max([long]$peakMemory,[long]$game.WorkingSet64)};$outTask.GetAwaiter().GetResult() | Out-Null;$errTask.GetAwaiter().GetResult() | Out-Null;$global:LASTEXITCODE=$game.ExitCode;[pscustomobject]@{CpuAffinityMask='0x55';Processors=$CpuCount;RequestedHeapMiB=2048;ViewDistance=$CityViewDistance;SampledPeakWorkingSetMiB=$peakMemory/1MB;ExitCode=$game.ExitCode} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Runtime 'native-envelope.json')}finally{$output.Dispose();$errors.Dispose();if(!$game.HasExited){$game.Kill()};$game.Dispose()}
    }else{& $Java8 @args *> 'native-client-console.log'}
    if($LASTEXITCODE -ne 0) {throw "Client exited $LASTEXITCODE"}
    if(!(Select-String -LiteralPath 'native-client-console.log' -SimpleMatch 'IAA_CLIENT_PROBE_PASS')) {throw 'Client probe did not finish'}
    if($Workbench){Get-Content -LiteralPath 'workbench-client.json'}elseif($ThatchInteraction){Get-Content -LiteralPath 'thatch-interaction.json'}elseif($CitySource){Get-Content -LiteralPath 'city-client.json'}else{Get-Content -LiteralPath 'architecture-probe.json'}
} finally {Pop-Location}
