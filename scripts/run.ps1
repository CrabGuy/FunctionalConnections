#Requires -Version 5.1
$ErrorActionPreference = "Stop"

# ----- Determine project root -----
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..")
Set-Location $ProjectRoot

Write-Host "Project root: $ProjectRoot"

# ----- Configuration -----
$ServerMain = "server.app.ServerMain"
$ClientMain = "client.app.ClientMain"
$LibDir = Join-Path $ProjectRoot "lib"
$OutDir = Join-Path $ProjectRoot "target\classes"
$ServerPort = 8080
$ServerLog = Join-Path $ProjectRoot "server.log"

# Check that the required Gson JAR exists
$GsonJar = Join-Path $LibDir "gson-2.10.1.jar"
if (-not (Test-Path $GsonJar)) {
    Write-Error "ERROR: $GsonJar not found. Please place gson-2.10.1.jar in $LibDir"
    exit 1
}

# ----- Clean & prepare -----
Write-Host "Cleaning previous build..."
Remove-Item -Recurse -Force (Join-Path $ProjectRoot "target") -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# Locate source directory (maven style or flat)
$SrcDir = Join-Path $ProjectRoot "src\main\java"
if (-not (Test-Path $SrcDir -PathType Container)) {
    $SrcDir = Join-Path $ProjectRoot "src"
}
Write-Host "Using source directory: $SrcDir"

# ----- Compile -----
Write-Host "Compiling project..."
$SourceFiles = Get-ChildItem -Path $SrcDir -Filter "*.java" -Recurse -File | ForEach-Object { $_.FullName }
$SourcesListPath = Join-Path $ProjectRoot "sources.txt"
$SourceFiles | Set-Content -Path $SourcesListPath

javac -d $OutDir -cp $GsonJar "@$SourcesListPath"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed!"
    Remove-Item $SourcesListPath -ErrorAction SilentlyContinue
    exit 1
}
Remove-Item $SourcesListPath -ErrorAction SilentlyContinue
Write-Host "Compilation successful."

# ----- Runtime classpath -----
$CpRuntime = "$OutDir;$GsonJar"

# ----- Start server in background -----
Write-Host "Starting server (logging to $ServerLog)..."
$ServerProcess = Start-Process -FilePath "java" `
    -ArgumentList @("-cp", "`"$CpRuntime`"", $ServerMain) `
    -RedirectStandardOutput $ServerLog `
    -RedirectStandardError "$ServerLog.err" `
    -PassThru -NoNewWindow

# ----- Cleanup (runs on normal exit or Ctrl+C via finally) -----
function Cleanup {
    Write-Host "Cleaning up..."
    if ($ServerProcess -and -not $ServerProcess.HasExited) {
        Stop-Process -Id $ServerProcess.Id -Force -ErrorAction SilentlyContinue
        $ServerProcess.WaitForExit(5000) | Out-Null
    }
    Write-Host "Removing compiled classes..."
    Remove-Item -Recurse -Force (Join-Path $ProjectRoot "target") -ErrorAction SilentlyContinue
    Write-Host "Done."
}

try {
    # ----- Wait for server to be ready (port check) -----
    Write-Host "Waiting for server on port $ServerPort..."
    $ready = $false
    while (-not $ready) {
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $tcpClient.Connect("localhost", $ServerPort)
            $ready = $tcpClient.Connected
            $tcpClient.Close()
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    Write-Host "Server is ready!"

    # ----- Run client -----
    Write-Host "Starting client..."
    java -cp $CpRuntime $ClientMain

    Write-Host "Client exited. Shutting down..."
}
finally {
    Cleanup
}