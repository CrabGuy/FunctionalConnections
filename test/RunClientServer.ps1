# 1. Compile the Java files
Write-Host "Compiling Java files..." -ForegroundColor Cyan
javac server/ServerMain.java client/ClientMain.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed!"
    exit
}

# 2. Start the Server in a new window and capture its process object
Write-Host "Starting TCP Server..." -ForegroundColor Green
$server_process = Start-Process powershell -ArgumentList "-Command", "java server.ServerMain" -PassThru

# 3. Give the server a moment to spin up and bind to the port
Start-Sleep -Seconds 2

# 4. Run the Client in the current window
Write-Host "Starting TCP Client..." -ForegroundColor Yellow
java client.ClientMain

# 5. Cleanup: Stop the server and delete .class files
Write-Host "`nCleaning up resources..." -ForegroundColor Cyan

# Terminate the server process
if ($server_process -and -not $server_process.HasExited) {
    Stop-Process -Id $server_process.Id -Force
    Write-Host "Server process stopped." -ForegroundColor DarkGreen
}

# Remove all .class files recursively in server and client folders
Get-ChildItem -Path ./server, ./client -Filter *.class -Recurse | Remove-Item -Force
Write-Host "Generated .class files cleaned up successfully." -ForegroundColor DarkGreen