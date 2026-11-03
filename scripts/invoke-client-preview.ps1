param(
    [string]$Runtime='D:\IronAgeArchitecture\manual-testing\horizontal-rails-0.1.8\native-client',
    [string]$PreviewName='Horizontal Rails Preview',
    [string]$ServerTemplate='D:\IronAgeArchitecture\manual-testing\jetties-0.1.2-final\core',
    [string]$GradleHome='D:\GradleCaches\minecraft-mods-shared',
    [string]$AssetsDirectory='',
    [string]$Java8='C:\Users\johnb\.gradle\jdks\temurin-8-amd64-windows.2\bin\java.exe'
)
$ErrorActionPreference='Stop'
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
Copy-Item -LiteralPath (Join-Path $repo ('build/libs/'+$runtimeJar)),(Join-Path $repo 'build/probe/architecture-probe.jar') -Destination (Join-Path $Runtime 'mods')
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
renderDistance:8
guiScale:2
fancyGraphics:true
'@ | Set-Content -LiteralPath (Join-Path $Runtime 'options.txt') -Encoding ascii
$digest=(Get-FileHash -LiteralPath (Join-Path $Runtime ('mods/'+$runtimeJar)) -Algorithm SHA256).Hash.ToLowerInvariant()
$args=@('-Xmx2G',('-Djava.library.path='+(Join-Path $slime 'natives')),'-Diaa.probe=true','-Diaa.clientProbe=true','-Diaa.keepRunning=true',('-Diaa.previewName='+$PreviewName),('-Diaa.runtimeSha256='+$digest),'-cp',($classpath -join ';'),
    'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLTweaker',
    '--username','ArchitecturePreview','--version','1.10.2','--gameDir',$Runtime,'--assetsDir',$AssetsDirectory,
    '--assetIndex','1.10','--uuid','00000000000000000000000000000001','--accessToken','0','--userType','legacy','--versionType','release','--width','1280','--height','800')
Push-Location $Runtime
try {
    & $Java8 @args *> 'native-client-console.log'
    if($LASTEXITCODE -ne 0) {throw "Client exited $LASTEXITCODE"}
    if(!(Select-String -LiteralPath 'native-client-console.log' -SimpleMatch 'IAA_CLIENT_PROBE_PASS')) {throw 'Client probe did not finish'}
    Get-Content -LiteralPath 'architecture-probe.json'
} finally {Pop-Location}
