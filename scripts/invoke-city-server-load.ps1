param([Parameter(Mandatory=$true)][string]$Runtime,[Parameter(Mandatory=$true)][string]$World,[int]$Houses=500,[int]$Port=25675,[switch]$RemoveProbeRecord)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
if(Test-Path -LiteralPath $Runtime){throw 'Use a fresh runtime'}
$template='D:/IronAgeArchitecture/manual-testing/jetties-0.1.2-final/core'
New-Item -ItemType Directory -Path $Runtime,"$Runtime/mods","$Runtime/config" | Out-Null
foreach($p in @('forge-1.10.2-12.18.3.2511-universal.jar','minecraft_server.1.10.2.jar','libraries')){Copy-Item -LiteralPath "$template/$p" -Destination $Runtime -Recurse}
Copy-Item -LiteralPath $World -Destination "$Runtime/world" -Recurse
$jar=Join-Path $repo 'build/libs/iron-age-architecture-0.1.16.110021.jar'
Copy-Item -LiteralPath $jar -Destination "$Runtime/mods"
'eula=true' | Set-Content "$Runtime/eula.txt" -Encoding ascii
"building {`n S:mode=automatic`n B:retainLegacyWallIds=false`n B:retainLegacyThatchIds=true`n}" | Set-Content "$Runtime/config/ironagearchitecture.cfg" -Encoding ascii
"level-name=world`nonline-mode=false`nserver-ip=127.0.0.1`nserver-port=$Port`nview-distance=6`nmax-tick-time=120000`n" | Set-Content "$Runtime/server.properties" -Encoding ascii
$java='C:/Users/johnb/.gradle/jdks/temurin-8-amd64-windows.2/bin/java.exe'
$rows=@()
foreach($pass in @('first-process','second-process')){
 $psi=[Diagnostics.ProcessStartInfo]::new($java);$psi.WorkingDirectory=$Runtime;$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true;$psi.RedirectStandardInput=$true;$psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true
 foreach($argument in @('-Xmx2G','-XX:ActiveProcessorCount=4','-jar','forge-1.10.2-12.18.3.2511-universal.jar','nogui')){$psi.ArgumentList.Add($argument)}
 if($RemoveProbeRecord -and $pass -eq 'first-process'){$psi.ArgumentList.Insert(0,'-Dfml.queryResult=confirm')}
 $watch=[Diagnostics.Stopwatch]::StartNew();$server=[Diagnostics.Process]::Start($psi);$server.ProcessorAffinity=[IntPtr]0x55;$err=$server.StandardError.ReadToEndAsync();$log=[IO.StreamWriter]::new("$Runtime/$pass.log");$log.AutoFlush=$true;$lineTask=$server.StandardOutput.ReadLineAsync();$ready=$false;$peakMemory=0;$readyMs=0;$worldSeconds=0;$stop=$false
 try{while($true){$server.Refresh();$peakMemory=[Math]::Max([long]$peakMemory,[long]$server.WorkingSet64);if($watch.Elapsed.TotalSeconds -gt 120){throw 'City server load timed out'};if(!$lineTask.Wait(200)){if($ready -and !$stop -and $watch.Elapsed.TotalMilliseconds -gt $readyMs+6000){$server.StandardInput.WriteLine('stop');$server.StandardInput.Flush();$stop=$true};continue};$line=$lineTask.Result;if($null -eq $line){break};$log.WriteLine($line);if(!$ready -and $line -match 'Done \(([0-9.]+)s\)'){$ready=$true;$readyMs=$watch.Elapsed.TotalMilliseconds;$worldSeconds=[double]$Matches[1]};$lineTask=$server.StandardOutput.ReadLineAsync()};$server.WaitForExit();$log.WriteLine($err.Result);if(!$ready -or $server.ExitCode -ne 0){throw 'City server load failed'};$rows+=[pscustomobject]@{Pass=$pass;ProcessToReadyMs=$readyMs;MinecraftWorldLoadSeconds=$worldSeconds;PeakWorkingSetMiB=$peakMemory/1MB;ExitCode=$server.ExitCode}}finally{if(!$server.HasExited){$server.Kill()};$log.Dispose();$server.Dispose()}
}
[pscustomobject]@{Passed=$true;Houses=$Houses;RuntimeSha256=(Get-FileHash -LiteralPath $jar).Hash.ToLowerInvariant();PlayerModOnly=$true;HeapLimitMiB=2048;ProcessorCount=4;AffinityMask='0x55';Runs=$rows} | ConvertTo-Json -Depth 5 | Tee-Object -FilePath "$Runtime/server-load.json"
