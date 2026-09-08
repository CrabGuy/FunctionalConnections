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
$OutDir = Join-Path $ProjectRoot "out"
$ClassesDir = Join-Path $ProjectRoot "target\classes"

# Check that the required Gson JAR exists
$GsonJar = Join-Path $LibDir "gson-2.10.1.jar"
if (-not (Test-Path $GsonJar)) {
    Write-Error "ERROR: $GsonJar not found. Please place gson-2.10.1.jar in $LibDir"
    exit 1
}

# ----- Clean & prepare -----
Write-Host "Cleaning previous build..."
# Remove only compiled classes (temporary)
Remove-Item -Recurse -Force (Join-Path $ProjectRoot "target") -ErrorAction SilentlyContinue
# Create output directory if missing (preserves existing .gitkeep)
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
# Delete only JAR files inside OutDir (keeps .gitkeep and other files)
Get-ChildItem -Path $OutDir -Filter "*.jar" -File | Remove-Item -Force

# Prepare class output directory
New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null

# Locate source directory (maven style or flat)
$SrcDir = Join-Path $ProjectRoot "src\main\java"
if (-not (Test-Path $SrcDir -PathType Container)) {
    $SrcDir = Join-Path $ProjectRoot "src"
}
Write-Host "Using source directory: $SrcDir"

# ----- Compile -----
Write-Host "Compiling project..."
$SourceFiles = Get-ChildItem -Path $SrcDir -Filter "*.java" -Recurse -File | ForEach-Object { $_.FullName }
$SourceFiles | Set-Content -Path (Join-Path $ProjectRoot "sources.txt")

javac -d $ClassesDir -cp $GsonJar "@$(Join-Path $ProjectRoot 'sources.txt')"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed!"
    Remove-Item (Join-Path $ProjectRoot "sources.txt") -ErrorAction SilentlyContinue
    exit 1
}
Remove-Item (Join-Path $ProjectRoot "sources.txt") -ErrorAction SilentlyContinue
Write-Host "Compilation successful."

# ----- Function to package a single JAR -----
function Package-Jar {
    param(
        [string]$MainClass,
        [string]$JarName
    )
    Write-Host "Packaging $JarName..."
    $Staging = Join-Path $ProjectRoot "target\staging_$JarName"
    New-Item -ItemType Directory -Force -Path $Staging | Out-Null

    # Copy compiled classes
    Copy-Item -Path (Join-Path $ClassesDir "*") -Destination $Staging -Recurse -Force

    # Extract Gson classes into staging (create fat JAR)
    Push-Location $Staging
    jar xf $GsonJar
    Pop-Location

    # Create manifest
    $ManifestPath = Join-Path $ProjectRoot "target\manifest_$JarName.txt"
    "Main-Class: $MainClass" | Set-Content -Path $ManifestPath

    # Build JAR
    Push-Location $Staging
    jar cfm (Join-Path $OutDir $JarName) $ManifestPath -C $Staging "."
    Pop-Location

    # Clean staging
    Remove-Item -Recurse -Force $Staging -ErrorAction SilentlyContinue
    Remove-Item -Force $ManifestPath -ErrorAction SilentlyContinue
}

# ----- Package both applications -----
Package-Jar -MainClass $ClientMain -JarName "client.jar"
Package-Jar -MainClass $ServerMain -JarName "server.jar"

# ----- Clean temporary compilation directory -----
Remove-Item -Recurse -Force (Join-Path $ProjectRoot "target") -ErrorAction SilentlyContinue

Write-Host "Build complete. JARs are in $OutDir\:"
Get-ChildItem -Path $OutDir -Filter "*.jar" | Format-Table Name, Length