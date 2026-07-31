#!/bin/bash

cd "$(dirname "$0")/.." || exit 1

mvn clean compile || exit 1

mvn exec:java -Dexec.mainClass="server.ServerMain" &
SERVER_PID=$!

sleep 2

mvn exec:java -Dexec.mainClass="client.ClientMain"

kill $SERVER_PID 2>/dev/null