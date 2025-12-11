# ============================================================================
# MyBible JAC Application - Docker Image
# ============================================================================
# Multi-stage build for optimized production image
#
# Build from jac2024 root:
#   docker build -f app/com/mybible/docker/Dockerfile -t mybible:1.0.0 .
#
# Run:
#   docker run -p 8080:8080 -e JWT_SECRET=your-secret mybible:1.0.0
# ============================================================================

# Stage 1: Build stage (copy and organize files)
FROM eclipse-temurin:24-jdk-alpine AS builder

WORKDIR /build

# Copy JAC runtime from jacBuild24
COPY jacBuild24/lib ./lib
COPY jacBuild24/classes ./jacBuild24-classes
COPY jacBuild24/phase1Classes ./phase1Classes
COPY jacBuild24/license.xml ./license.xml
COPY jacBuild24/conf ./conf

# Copy application compiled classes
COPY classes ./classes

# Copy application source/config (for Properties.xml, etc.)
COPY app/com/mybible ./app/com/mybible

# Copy JAC schema files (required for Property loading)
COPY app/com/esarks/arm/schemas ./app/com/esarks/arm/schemas

# Copy bin scripts (for config.properties reference)
COPY bin ./bin

# Copy config directory (Properties.xml, etc.)
COPY config ./config

# Stage 2: Runtime image
FROM eclipse-temurin:24-jdk-alpine

# Install runtime dependencies
RUN apk add --no-cache \
    bash \
    curl \
    ca-certificates \
    tzdata \
    netcat-openbsd \
    dos2unix

# Create non-root user for security
RUN addgroup -S jac -g 1000 && \
    adduser -S jac -u 1000 -G jac

# Set working directory
WORKDIR /opt/jac

# Copy from builder stage
COPY --from=builder --chown=jac:jac /build/lib ./lib
COPY --from=builder --chown=jac:jac /build/jacBuild24-classes ./jacBuild24-classes
COPY --from=builder --chown=jac:jac /build/phase1Classes ./phase1Classes
COPY --from=builder --chown=jac:jac /build/classes ./classes
COPY --from=builder --chown=jac:jac /build/license.xml ./license.xml
COPY --from=builder --chown=jac:jac /build/conf ./conf
COPY --from=builder --chown=jac:jac /build/app ./app
COPY --from=builder --chown=jac:jac /build/bin ./bin
COPY --from=builder --chown=jac:jac /build/config ./config

# Override Properties.xml with Docker-specific version
COPY --chown=jac:jac app/com/mybible/docker/Properties-docker.xml ./config/properties/Properties.xml

# Copy schema files to JAC_SCRIPTS location (required for Job.script to find properties.xml schema)
COPY --from=builder --chown=jac:jac /build/app/com/esarks/arm/schemas ./com/esarks/arm/schemas

# Copy entrypoint script and fix line endings (Windows -> Unix)
COPY --chown=jac:jac app/com/mybible/docker/docker-entrypoint.sh ./docker-entrypoint.sh
RUN dos2unix ./docker-entrypoint.sh && chmod +x ./docker-entrypoint.sh

# Create directories for runtime data
# Also create symlink for legacy path resolution
RUN mkdir -p /opt/jac/logs /opt/jac/data && \
    ln -s /opt/jac/app /app && \
    chown -R jac:jac /opt/jac

# Switch to non-root user
USER jac

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Environment variables with defaults
ENV JAC_HOME=/opt/jac \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport" \
    PORT=8080 \
    TZ=America/New_York

# Labels
LABEL maintainer="MyBible Team" \
      version="1.0.0" \
      description="MyBible Personal Bible Study Application"

ENTRYPOINT ["./docker-entrypoint.sh"]
