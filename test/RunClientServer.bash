#!/bin/bash

javac server/ServerMain.java client/ClientMain.java || exit 1

java server.ServerMain &
SERVER_PID=$!

sleep 2

java client.ClientMain

kill $SERVER_PID 2>/dev/null
find server client -name "*.class" -delete