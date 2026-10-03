<#
.SYNOPSIS
    Runs the in-game behaviour check on every loader x Minecraft line, and prints one line each.

.DESCRIPTION
    A green check here is the only cheap evidence a mixin still applies: Mixin resolves its targets
    at runtime, so compiling proves nothing. NeoForge's runServer ignores stdin, which is why the
    test halts the server itself.

    A world never loads in an older Minecraft, so each line gets its own level-name.

.EXAMPLE
    .\selftest.ps1                       # every loader, every line of MATRIX
    .\selftest.ps1 -Loaders fabric       # one loader
#>
[CmdletBinding()]
param(
    [string[]]$Loaders  = @('fabric', 'neoforge'),
    [string[]]$Versions,
    [string]  $ModId,
    [int]     $TimeoutSeconds = 420
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

if (-not $ModId) {
    $ModId = (Select-String -Path (Join-Path $root 'gradle.properties') -Pattern '^mod_id=(.+)$').Matches.Groups[1].Value
}
if (-not $Versions) {
    $Versions = Select-String -Path (Join-Path $root 'build.gradle') -Pattern '^\s*"([0-9][^"]*)"\s*:\s*\[' -AllMatches |
        ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value }
}

$envName = ($ModId.ToUpper() -replace '[^A-Z0-9]', '') + '_SELFTEST'
Write-Host "Self-test: $($Loaders -join ', ') x $($Versions -join ', ')  (env $envName)" -ForegroundColor Cyan

$failed = $false
foreach ($version in $Versions) {
    foreach ($loader in $Loaders) {
        $runDir = Join-Path $root "$loader\run"
        $props  = Join-Path $runDir 'server.properties'
        if (Test-Path $props) {
            (Get-Content $props) -replace '^level-name=.*', "level-name=world-$version" | Set-Content $props -Encoding utf8
        }

        $log = Join-Path $env:TEMP "selftest-$ModId-$loader-$version.log"
        [Environment]::SetEnvironmentVariable($envName, 'true')
        # The server stops itself once the check has run. If a mixin never applied, nothing stops it
        # and the run would hang forever - hence the kill switch. NeoForge's runServer also ignores
        # stdin, so a piped "stop" is not an option.
        $proc = Start-Process -FilePath (Join-Path $root 'gradlew.bat') `
            -ArgumentList ":${loader}:runServer", "-Pminecraft_version=$version", "--console=plain", "-q" `
            -WorkingDirectory $root -NoNewWindow -PassThru `
            -RedirectStandardOutput $log -RedirectStandardError "$log.err"
        if (-not $proc.WaitForExit($TimeoutSeconds * 1000)) {
            Write-Host "  $loader $version : TIMEOUT after ${TimeoutSeconds}s - the server never halted, so the check never ran" -ForegroundColor Red
            try { $proc.Kill($true) } catch { }
            [Environment]::SetEnvironmentVariable($envName, $null)
            $failed = $true
            continue
        }
        [Environment]::SetEnvironmentVariable($envName, $null)

        $result = Select-String -Path $log -Pattern '\[SELFTEST\] (RESULT|.*FAILED)' | ForEach-Object { $_.Line }
        if (-not $result) {
            Write-Host "  $loader $version : NO RESULT - read $log" -ForegroundColor Red
            $failed = $true
        } elseif ($result -match 'PASS') {
            Write-Host "  $loader $version : PASS" -ForegroundColor Green
        } else {
            Write-Host "  $loader $version : $result" -ForegroundColor Red
            $failed = $true
        }
    }
}

if ($failed) { exit 1 }
