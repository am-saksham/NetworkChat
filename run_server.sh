#!/bin/bash
# Compile
echo "Compiling Server..."
mkdir -p backend/bin
javac -d backend/bin backend/src/com/saksham/networkchat/server/*.java

# Run
echo "Starting Server on port 443..."
# Use absolute path for keystore to avoid systemd CWD issues
export KEYSTORE_PATH="$(pwd)/server.keystore"
java -Djavax.net.ssl.keyStore="$KEYSTORE_PATH" -Djavax.net.ssl.keyStorePassword=password123 -cp backend/bin com.saksham.networkchat.server.ServerMain 443
