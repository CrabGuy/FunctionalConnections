#!/usr/bin/env pwsh
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Join-Path $scriptDir ".."
Set-Location $projectRoot

Write-Host "Compiling..."
mvn clean compile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Checking for existing process on port 8080..."
$existingPids = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
                Select-Object -ExpandProperty OwningProcess -Unique
if ($existingPids) {
    Write-Host "Found process(es) with PID(s): $($existingPids -join ', '). Stopping..."
    $existingPids | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
}

Write-Host "Starting server..."
$serverOut = Join-Path $projectRoot "server.log"
$serverErr = Join-Path $projectRoot "server.err"

$serverProcess = Start-Process -FilePath "mvn" `
    -ArgumentList @("exec:java", "-Dexec.mainClass=server.ServerMain") `
    -RedirectStandardOutput $serverOut `
    -RedirectStandardError $serverErr `
    -PassThru -WindowStyle Hidden

try {
    # Wait for server to be ready
    $timeout = 30
    $elapsed = 0
    $ready = $false
    while ($elapsed -lt $timeout) {
        if ($serverProcess.HasExited) {
            Write-Error "Server exited early. Check $serverOut and $serverErr"
            exit 1
        }
        if (Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 1
        $elapsed++
    }
    if (-not $ready) {
        Write-Error "Server did not start within $timeout seconds."
        exit 1
    }

    Write-Host "Running client..."
    & mvn exec:java "-Dexec.mainClass=client.ClientMain"
    if ($LASTEXITCODE -ne 0) {
        throw "Client failed with exit code $LASTEXITCODE"
    }
}
finally {
    if ($serverProcess -and -not $serverProcess.HasExited) {
        Write-Host "Stopping server process tree (PID $($serverProcess.Id))..."
        & taskkill /PID $serverProcess.Id /T /F 2>$null
    }
}