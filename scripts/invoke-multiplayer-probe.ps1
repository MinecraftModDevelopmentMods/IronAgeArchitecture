param(
 [Parameter(Mandatory=$true)][string]$Root,
 [Parameter(Mandatory=$true)][string]$SourceWorld,
 [ValidateSet(1,5,10)][int]$Clients=1,
 [ValidateSet('together','spread')][string]$Layout='together',
 [switch]$Prepare,
 [switch]$RoofJobs,
 [int]$Port=25676,
 [ValidateRange(0,16)][int]$NetworkThreads=0,
 [string]$PlayerJar,
 [switch]$Profile,
 [switch]$GcLog,
 [ValidateRange(0,10)][int]$Reconnects=0,
 [ValidateRange(2,16)][int]$ViewDistance=6,
 [string]$ProbeJar
)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
if(Test-Path -LiteralPath (Join-Path (Split-Path $Root -Parent) 'stop-matrix.flag')){throw 'Qualification stopped at a case boundary; see stop-matrix.flag'}
if(Test-Path -LiteralPath $Root){throw 'Use a fresh multiplayer runtime; failed cases are retained'}
$java='C:/Users/johnb/.gradle/jdks/temurin-8-amd64-windows.2/bin/java.exe'
$template='D:/IronAgeArchitecture/manual-testing/jetties-0.1.2-final/core'
$gradleHome='D:/GradleCaches/minecraft-mods-shared'
$slime=Join-Path $gradleHome 'caches/minecraftforge/forgegradle/slime-launcher/cache/net/minecraftforge/forge/1.10.2-12.18.3.2511'
if(!$PlayerJar){$PlayerJar=Join-Path $repo 'build/libs/iron-age-architecture-0.1.16.110021.jar'}
$playerJar=$PlayerJar
if(!$ProbeJar){$ProbeJar=Join-Path $repo 'build/probe/architecture-probe.jar'}
$probeJar=$ProbeJar
$digest=(Get-FileHash $playerJar).Hash.ToLowerInvariant()
$probeDigest=(Get-FileHash $probeJar).Hash.ToLowerInvariant()
$running=[Collections.Generic.List[object]]::new()
New-Item -ItemType Directory -Path $Root,"$Root/server","$Root/server/mods","$Root/server/config" | Out-Null
Copy-Item -LiteralPath $SourceWorld -Destination "$Root/server/world" -Recurse
foreach($file in @('forge-1.10.2-12.18.3.2511-universal.jar','minecraft_server.1.10.2.jar','libraries')){Copy-Item -LiteralPath "$template/$file" -Destination "$Root/server" -Recurse}
$config="building {`n S:mode=automatic`n B:retainLegacyWallIds=false`n B:retainLegacyThatchIds=true`n}"
$config | Set-Content "$Root/server/config/ironagearchitecture.cfg" -Encoding ascii
Copy-Item -LiteralPath $playerJar,$probeJar -Destination "$Root/server/mods"
'eula=true' | Set-Content "$Root/server/eula.txt" -Encoding ascii
"level-name=world`nonline-mode=false`nserver-ip=127.0.0.1`nserver-port=$Port`nview-distance=$ViewDistance`nallow-flight=true`nmax-players=20`nspawn-protection=0`ngamemode=1`ndifficulty=0`nmax-tick-time=60000`n" | Set-Content "$Root/server/server.properties" -Encoding ascii
function Start-TestJvm([string]$Folder,[string[]]$JvmArgs,[long]$Mask,[string]$Name){
 $psi=[Diagnostics.ProcessStartInfo]::new($java);$psi.WorkingDirectory=$Folder;$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true;$psi.WindowStyle=[Diagnostics.ProcessWindowStyle]::Hidden;$psi.RedirectStandardInput=$true;$psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true
 foreach($argument in $JvmArgs){$psi.ArgumentList.Add($argument)}
 $game=[Diagnostics.Process]::Start($psi);$game.ProcessorAffinity=[IntPtr]$Mask
 $out=[IO.File]::Open((Join-Path $Folder 'console.log'),[IO.FileMode]::Create,[IO.FileAccess]::Write,[IO.FileShare]::ReadWrite)
 $err=[IO.File]::Open((Join-Path $Folder 'errors.log'),[IO.FileMode]::Create,[IO.FileAccess]::Write,[IO.FileShare]::ReadWrite)
 $item=[pscustomobject]@{Name=$Name;Process=$game;Folder=$Folder;Output=$out;Errors=$err;OutTask=$game.StandardOutput.BaseStream.CopyToAsync($out);ErrTask=$game.StandardError.BaseStream.CopyToAsync($err);PeakWorkingSet=[long]0;Mask=$Mask}
 $running.Add($item);return $item
}
function Finish-TestJvm($Item){$Item.OutTask.GetAwaiter().GetResult() | Out-Null;$Item.ErrTask.GetAwaiter().GetResult() | Out-Null;$Item.Output.Flush();$Item.Errors.Flush()}
function Client-Environment {
 $metadata=Get-Content (Join-Path $slime 'metadata/minecraft/version.json') -Raw | ConvertFrom-Json
 $vanilla=Join-Path $gradleHome 'caches/minecraftforge/forgegradle/mavenizer/caches/minecraft_tasks/1.10.2/client.jar'
 $classpath=[Collections.Generic.List[string]]::new();$classpath.Add($vanilla);$classpath.Add((Join-Path $template 'forge-1.10.2-12.18.3.2511-universal.jar'))
 Get-ChildItem "$template/libraries" -Recurse -Filter '*.jar' | ForEach-Object {$classpath.Add($_.FullName)}
 foreach($library in $metadata.libraries){$allowed=!$library.rules;foreach($rule in $library.rules){if(!$rule.os -or $rule.os.name -eq 'windows'){$allowed=$rule.action -eq 'allow'}};if(!$allowed -or !$library.downloads.artifact){continue};$artifact=$library.downloads.artifact;$filename=Split-Path $artifact.path -Leaf;if($classpath | Where-Object {(Split-Path $_ -Leaf) -eq $filename}){continue};$coordinate=$library.name.Split(':');$cache=Join-Path $gradleHome ('caches/modules-2/files-2.1/'+$coordinate[0]+'/'+$coordinate[1]+'/'+$coordinate[2]);$found=Get-ChildItem $cache -Recurse -Filter $filename -ErrorAction SilentlyContinue | Select-Object -First 1;if(!$found){throw "Missing verified native client library: $filename"};if((Get-FileHash $found.FullName -Algorithm SHA1).Hash -ne $artifact.sha1){throw "Library checksum mismatch: $filename"};$classpath.Add($found.FullName)}
 $assets='C:/Users/johnb/.gradle/caches/minecraft/assets';if((Get-FileHash "$assets/indexes/1.10.json" -Algorithm SHA1).Hash -ne $metadata.assetIndex.sha1){throw 'Asset index mismatch'}
 return [pscustomobject]@{Classpath=$classpath -join ';';Assets=$assets}
}
$case=[ordered]@{Started=(Get-Date).ToUniversalTime().ToString('o');Players=$Clients;Layout=$Layout;Prepare=[bool]$Prepare;PlayerSha256=$digest;ProbeSha256=$probeDigest;ServerHeapMiB=2048;ServerProcessors=4;ServerCpuMask='0x55';ClientHeapMiB=1536;ClientProcessors=2;ClientCpuMask='0xffff00';ViewDistance=6;Transport='TCP loopback; separate native Forge processes';SourceWorld=$SourceWorld}
$case | ConvertTo-Json | Set-Content "$Root/case.json"
$case['ServerNetworkThreads']=if($NetworkThreads){$NetworkThreads}else{'Netty default'}
$case['RoofJobs']=[bool]$RoofJobs
$case['ViewDistance']=$ViewDistance;$case['ReconnectRounds']=$Reconnects;$case['FlightRecording']=[bool]$Profile;$case['GCLogging']=[bool]($Profile -or $GcLog)
$case | ConvertTo-Json | Set-Content "$Root/case.json"
$serverArgs=@('-Xmx2G','-XX:ActiveProcessorCount=4','-Djava.awt.headless=true','-Diaa.probe=true','-Diaa.mpServer=true',('-Diaa.mpRoot='+$Root),('-Diaa.runtimeSha256='+$digest),('-Diaa.mpClients='+$Clients),('-Diaa.mpSpread='+($Layout -eq 'spread').ToString().ToLowerInvariant()),'-jar','forge-1.10.2-12.18.3.2511-universal.jar','nogui')
if($RoofJobs){$serverArgs=@('-Diaa.mpRoofJobs=true')+$serverArgs}
if($Prepare){$serverArgs=@('-Diaa.mpPrepare=true')+$serverArgs}
if($NetworkThreads){$serverArgs=@("-Dio.netty.eventLoopThreads=$NetworkThreads")+$serverArgs}
$serverArgs=@("-Diaa.mpReconnects=$Reconnects")+$serverArgs
if($Profile){$serverArgs=@('-XX:+FlightRecorder',('-XX:StartFlightRecording=filename='+$Root+'/server/server.jfr,settings=profile,dumponexit=true'))+$serverArgs}
if($Profile -or $GcLog){$serverArgs=@('-XX:+PrintGCDetails','-XX:+PrintGCDateStamps','-XX:+PrintGCApplicationStoppedTime',('-Xloggc:'+$Root+'/server/gc.log'))+$serverArgs}
$watch=[Diagnostics.Stopwatch]::StartNew();$connected=$false;$launched=$false;$lastSample=-1;$success=$false
try{
 $server=Start-TestJvm "$Root/server" $serverArgs 0x55 'server'
 while(!$server.Process.HasExited){
  if($watch.Elapsed.TotalSeconds -gt 900){throw 'Multiplayer case exceeded 15 minutes'}
  if(!$Prepare -and !$launched -and (Test-Path "$Root/server-ready.json")){
   $environment=Client-Environment
   for($id=0;$id -lt $Clients;$id++){
    $folder=Join-Path $Root ('client-'+$id.ToString('00'));New-Item -ItemType Directory -Path $folder,"$folder/mods","$folder/config" | Out-Null;Copy-Item -LiteralPath $playerJar,$probeJar -Destination "$folder/mods";$config | Set-Content "$folder/config/ironagearchitecture.cfg" -Encoding ascii
    $fps=if($id -eq 0){60}else{10}
    "pauseOnLostFocus:false`nshowInventoryAchievementHint:false`nrenderDistance:6`nguiScale:1`nfancyGraphics:false`nclouds:false`nmaxFps:$fps`nenableVsync:false`nsoundCategory_master:0.0`n" | Set-Content "$folder/options.txt" -Encoding ascii
    $args=@('-Xmx1536M','-XX:ActiveProcessorCount=2',('-Djava.library.path='+(Join-Path $slime 'natives')),'-Diaa.probe=true','-Diaa.mpClient=true',('-Diaa.mpId='+$id),('-Diaa.mpPort='+$Port),('-Diaa.mpFps='+$fps),('-Diaa.mpViewDistance='+$ViewDistance),('-Diaa.mpRoot='+$Root),('-Diaa.mpClientDir='+$folder),('-Diaa.runtimeSha256='+$digest),'-cp',$environment.Classpath,'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLTweaker','--username',('IAAMP'+$id.ToString('00')),'--version','1.10.2','--gameDir',$folder,'--assetsDir',$environment.Assets,'--assetIndex','1.10','--uuid',([Guid]::NewGuid().ToString('N')),'--accessToken','0','--userType','legacy','--versionType','release','--width','640','--height','360')
    $null=Start-TestJvm $folder $args 0xffff00 ('client-'+$id.ToString('00'))
   }
   $launched=$true
  }
  if($launched -and !$connected){$all=$true;for($id=0;$id -lt $Clients;$id++){$all=$all -and (Test-Path (Join-Path $Root ('client-'+$id.ToString('00')+'/menu-ready.json')))};if($all){'connect' | Set-Content "$Root/connect.flag";$connected=$true;"Released $Clients simultaneous TCP connections"}}
  foreach($item in $running){if(!$item.Process.HasExited){$item.Process.Refresh();$item.PeakWorkingSet=[Math]::Max([long]$item.PeakWorkingSet,[long]$item.Process.WorkingSet64)}elseif($item.Name -ne 'server' -and !(Test-Path "$Root/server-result.json")){Finish-TestJvm $item;throw "Client exited early: $($item.Name)"}}
  if([int]$watch.Elapsed.TotalSeconds -ne $lastSample -and [int]$watch.Elapsed.TotalSeconds%3 -eq 0){$lastSample=[int]$watch.Elapsed.TotalSeconds;$free=(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1MB;[pscustomobject]@{Seconds=$watch.Elapsed.TotalSeconds;ServerWorkingSetMiB=if($server.Process.HasExited){0}else{$server.Process.WorkingSet64/1MB};ClientsWorkingSetMiB=($running | Where-Object {$_.Name -ne 'server' -and !$_.Process.HasExited} | ForEach-Object {$_.Process.WorkingSet64/1MB} | Measure-Object -Sum).Sum;FreeHostGiB=$free} | Export-Csv "$Root/process-samples.csv" -Append -NoTypeInformation;if($free -lt 3){throw 'Host free memory fell below the 3 GiB guard'}}
  Start-Sleep -Milliseconds 250
 }
 foreach($item in $running){if(!$item.Process.WaitForExit(20000)){throw "Process did not stop: $($item.Name)"};Finish-TestJvm $item;if($item.Process.ExitCode -ne 0){throw "Process failed: $($item.Name), code $($item.Process.ExitCode)"}}
 if($Prepare){$receipt=Get-Content "$Root/prepared.json" -Raw | ConvertFrom-Json;if(!$receipt.passed){throw 'Preparation did not finish'}}else{$receipt=Get-Content "$Root/server-result.json" -Raw | ConvertFrom-Json;if(!$receipt.passed){throw "Server workload failed: $($receipt.reason)"};for($id=0;$id -lt $Clients;$id++){$file=Join-Path $Root ('client-'+$id.ToString('00')+'/client-result.json');$client=Get-Content $file -Raw | ConvertFrom-Json;if(!$client.passed -or !$client.tcp -or $client.integratedServer -or $client.runtimeSha256 -ne $digest){throw "Client receipt failed: $id"}}}
 if(!$Prepare){
  $audit=& node (Join-Path $PSScriptRoot 'audit-multiplayer-world.cjs') $Root
  $auditCode=$LASTEXITCODE;$audit | Set-Content "$Root/world-audit.json"
  if($auditCode -ne 0){throw 'Saved-world material or preservation audit failed'}
  $baseline=& node (Join-Path $PSScriptRoot 'audit-simple-thatch.cjs') $SourceWorld
  if($LASTEXITCODE -ne 0){throw 'Source-world roof audit failed'}
  $baseline | Set-Content "$Root/source-roof-audit.json"
  $baseOwners=($baseline | ConvertFrom-Json).materialOwners
  $roof=& node (Join-Path $PSScriptRoot 'audit-simple-thatch.cjs') "$Root/server/world" ($baseOwners+25*$Clients)
  $roofCode=$LASTEXITCODE;$roof | Set-Content "$Root/roof-audit.json"
  if($roofCode -ne 0){throw 'Saved roof material or legacy helper cleanup failed'}
 }
 $success=$true
 [pscustomobject]@{Passed=$true;Players=$Clients;Layout=$Layout;Prepare=[bool]$Prepare;ElapsedSeconds=$watch.Elapsed.TotalSeconds;PlayerSha256=$digest;ProbeSha256=$probeDigest;Processes=@($running | ForEach-Object {[pscustomobject]@{Name=$_.Name;Pid=$_.Process.Id;PeakWorkingSetMiB=$_.PeakWorkingSet/1MB;ExitCode=$_.Process.ExitCode;Affinity=$_.Mask}})} | ConvertTo-Json -Depth 5 | Tee-Object -FilePath "$Root/process-receipt.json"
} catch {
 [pscustomobject]@{Passed=$false;Error=$_.Exception.Message;ElapsedSeconds=$watch.Elapsed.TotalSeconds;PlayerSha256=$digest;ProbeSha256=$probeDigest} | ConvertTo-Json | Set-Content "$Root/harness-failure.json"
 throw
} finally {
 if(!$success -and $server -and !$server.Process.HasExited){try{$server.Process.StandardInput.WriteLine('stop');$server.Process.StandardInput.Flush();$null=$server.Process.WaitForExit(10000)}catch{}}
 foreach($item in $running){if(!$item.Process.HasExited){$item.Process.Kill();$item.Process.WaitForExit()};try{Finish-TestJvm $item}catch{};$item.Output.Dispose();$item.Errors.Dispose();$item.Process.Dispose()}
}
