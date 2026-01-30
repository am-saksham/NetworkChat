#!/bin/bash
# Compile
echo "Compiling Client..."
mkdir -p frontend/bin
javac -d frontend/bin frontend/src/com/saksham/networkchat/*.java

# Run
echo "Starting Client..."
java -cp frontend/bin com.saksham.networkchat.Login
