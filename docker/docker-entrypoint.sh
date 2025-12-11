#!/bin/bash
# ============================================================================
# MyBible Docker Entrypoint
# ============================================================================
# Starts the JAC application server with proper environment configuration
# Uses the Job system to properly initialize database connections
# ============================================================================

set -e

echo "=============================================="
echo " MyBible JAC Application Server"
echo " Version: 1.0.0"
echo "=============================================="
echo ""

# ============================================================================
# Wait for Dependencies
# ============================================================================

# Wait for PostgreSQL if configured
if [ -n "${DB_HOST}" ] && [ -n "${DB_PORT}" ]; then
    echo "[STARTUP] Waiting for database at ${DB_HOST}:${DB_PORT}..."
    attempt=1
    max_attempts=30
    while ! nc -z "${DB_HOST}" "${DB_PORT}" 2>/dev/null; do
        if [ ${attempt} -eq ${max_attempts} ]; then
            echo "[ERROR] Database not available after ${max_attempts} attempts"
            exit 1
        fi
        echo "  Attempt ${attempt}/${max_attempts}..."
        attempt=$((attempt + 1))
        sleep 2
    done
    echo "[STARTUP] Database is ready!"
fi

# ============================================================================
# Environment Configuration
# ============================================================================

# Set timezone if configured
if [ -n "${TZ}" ]; then
    export TZ
    echo "[CONFIG] Timezone: ${TZ}"
fi

# Validate required environment variables
if [ -z "${JWT_SECRET}" ]; then
    echo "[WARNING] JWT_SECRET not set - using default (NOT FOR PRODUCTION)"
fi

# ============================================================================
# Set JAC Environment Variables
# ============================================================================

export JAC_HOME="${JAC_HOME:-/opt/jac}"
export JAC_BASE="${JAC_HOME}"
export JAC_WORK="${JAC_HOME}/classes"
export JAC_SCRIPTS="${JAC_HOME}"
export JAVA_HOME="/opt/java/openjdk"

# ============================================================================
# Substitute secrets in Properties.xml
# ============================================================================

PROPS_FILE="${JAC_HOME}/config/properties/Properties.xml"
if [ -f "${PROPS_FILE}" ]; then
    echo "[CONFIG] Substituting secrets in Properties.xml"
    # Database connection settings
    [ -n "${DB_HOST}" ] && sed -i "s/__DB_HOST__/${DB_HOST}/g" "${PROPS_FILE}"
    [ -n "${DB_PORT}" ] && sed -i "s/__DB_PORT__/${DB_PORT}/g" "${PROPS_FILE}"
    [ -n "${DB_PASSWORD}" ] && sed -i "s/__DB_PASSWORD__/${DB_PASSWORD}/g" "${PROPS_FILE}"
    echo "[CONFIG] Database: ${DB_HOST}:${DB_PORT}"
fi

echo "[CONFIG] JAC_HOME: ${JAC_HOME}"
echo "[CONFIG] JAC_BASE: ${JAC_BASE}"
echo "[CONFIG] JAC_WORK: ${JAC_WORK}"
echo "[CONFIG] JAC_SCRIPTS: ${JAC_SCRIPTS}"

# ============================================================================
# Build Classpath
# ============================================================================

echo "[STARTUP] Building classpath..."

# Start with jacBuild classes and phase1Classes
CLASSPATH="${JAC_HOME}/jacBuild24-classes:${JAC_HOME}/phase1Classes"

# Add all JAR files from lib subdirectories
for dir in ${JAC_HOME}/lib/*/; do
    for jar in ${dir}*.jar; do
        [ -f "$jar" ] && CLASSPATH="${CLASSPATH}:${jar}"
    done
done

# Add loose JAR files in lib root
for jar in ${JAC_HOME}/lib/*.jar; do
    [ -f "$jar" ] && CLASSPATH="${CLASSPATH}:${jar}"
done

# Add application classes
CLASSPATH="${CLASSPATH}:${JAC_HOME}/classes"

export CLASSPATH

echo "[CONFIG] Classpath configured with $(echo $CLASSPATH | tr ':' '\n' | wc -l) entries"

# ============================================================================
# JVM Configuration
# ============================================================================

# Default JVM options optimized for containers
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseContainerSupport"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxRAMPercentage=75.0"

# Enable debug mode if requested
if [ "${DEBUG}" = "true" ]; then
    JAVA_OPTS="${JAVA_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    echo "[CONFIG] Debug mode enabled on port 5005"
fi

echo "[CONFIG] JVM Options: ${JAVA_OPTS}"

# ============================================================================
# Start Application using Job System
# ============================================================================

echo ""
echo "[STARTUP] Starting MyBible server via Job system..."
echo "  Port: ${PORT:-8080}"
echo "  JAC Home: ${JAC_HOME}"
echo ""

cd "${JAC_HOME}"

# Use the Job system to properly initialize database connections
# This runs com.esarks.jac.jrun.Job which reads Job.xml and Properties.xml
exec java ${JAVA_OPTS} \
    -cp "${CLASSPATH}" \
    com.esarks.jac.jac \
    -script "com.esarks.jac.jrun.Job" \
    -method "execute" \
    -argument "com.mybible.server.MyBibleRouter" \
    -argument "execute" \
    -home "${JAC_HOME}" \
    -work "${JAC_WORK}" \
    -scripts "${JAC_SCRIPTS}" \
    -debugInstance "docker" \
    -debugPath "${JAC_HOME}/logs" \
    -debug 4 \
    -debugClass 4
