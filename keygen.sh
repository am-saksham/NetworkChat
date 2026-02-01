#!/bin/bash
# Generate Server Keystore
echo "Generating Server Keystore..."
keytool -genkey -alias server -keyalg RSA -keysize 2048 -storetype PKCS12 \
    -keystore server.keystore -storepass password123 -keypass password123 \
    -dname "CN=52.66.246.194, OU=Dev, O=NetworkChat, L=City, ST=State, C=US"

# Export Server Public Certificate
echo "Exporting Server Certificate..."
keytool -export -alias server -keystore server.keystore -storepass password123 -file server.cer

# Generate Client Truststore (Import Server Cert)
echo "Generating Client Truststore..."
keytool -import -alias server -keystore client.truststore -storepass password123 -file server.cer -noprompt

# Cleanup
rm server.cer

echo "Keys generated successfully!"
echo "Server Keystore: server.keystore"
echo "Client Truststore: client.truststore"
