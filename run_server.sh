#!/bin/bash
# Compile
echo "Compiling Server..."
mkdir -p backend/bin
javac -d backend/bin backend/src/com/saksham/networkchat/server/*.java

# Run
echo "Starting Server on port 8192..."
java -cp backend/bin com.saksham.networkchat.server.ServerMain 8192
