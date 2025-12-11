#!/bin/bash

# MyBible Application Startup Script
# Starts Cloud SQL Proxy (if configured) and the JAC application

set -e

echo "=========================================="
echo "MyBible Application Startup"
echo "=========================================="

# Start Cloud SQL Proxy in background if CLOUD_SQL_CONNECTION is set
if [ -n "$CLOUD_SQL_CONNECTION" ]; then
    echo "[STARTUP] Starting Cloud SQL Proxy for: $CLOUD_SQL_CONNECTION"
    /usr/local/bin/cloud-sql-proxy "$CLOUD_SQL_CONNECTION" \
        --port=5432 \
        --address=0.0.0.0 \
        &
    PROXY_PID=$!
    echo "[STARTUP] Cloud SQL Proxy started (PID: $PROXY_PID)"

    # Wait for proxy to be ready
    echo "[STARTUP] Waiting for Cloud SQL Proxy to be ready..."
    sleep 5
fi

# Build classpath - include all JARs from lib subdirectories
CLASSPATH="/app/classes:/app/jacBuild24/classes:/app/jacBuild24/phase1Classes"

# Add all JAR files from lib and subdirectories
for jar in $(find /app/jacBuild24/lib -name "*.jar" 2>/dev/null); do
    CLASSPATH="$CLASSPATH:$jar"
done

export CLASSPATH

echo "[STARTUP] Classpath configured"
echo "[STARTUP] Starting MyBible server on port ${PORT:-8080}..."

# Start the JAC application using jac runner
cd /app
java -cp "$CLASSPATH" \
    com.esarks.jac.jac \
    -home /app \
    -work /app/classes \
    -scripts /app/app \
    -classpath "$CLASSPATH" \
    -script com.mybible.server.MyBibleRouter \
    -method execute

# If the app exits, also stop the proxy
if [ -n "$PROXY_PID" ]; then
    kill $PROXY_PID 2>/dev/null || true
fi
