#!/bin/bash
# Compile
echo "Compiling Client..."
mkdir -p frontend/bin
javac -d frontend/bin frontend/src/com/saksham/networkchat/*.java

# Run
echo "Starting Client..."
# Use absolute path for truststore
export TRUSTSTORE_PATH="$(pwd)/client.truststore"
java -Djavax.net.ssl.trustStore="$TRUSTSTORE_PATH" -Djavax.net.ssl.trustStorePassword=password123 -cp frontend/bin com.saksham.networkchat.Login
