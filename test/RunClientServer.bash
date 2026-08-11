#!/bin/bash

cd "$(dirname "$0")/.." || exit 1

mvn clean compile || exit 1

mvn exec:java -Dexec.mainClass="server.ServerMain" > server.log 2>&1 &
SERVER_PID=$!

trap 'kill $SERVER_PID 2>/dev/null' EXIT

while ! timeout 1 bash -c '</dev/tcp/localhost/8080' 2>/dev/null; do
    sleep 1
done

mvn exec:java -Dexec.mainClass="client.ClientMain"