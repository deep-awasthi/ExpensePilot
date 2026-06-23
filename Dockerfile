# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

# Copy Maven wrapper and project POMs first (layer-cache friendly)
COPY pom.xml                               ./pom.xml
COPY finance-tracker-domain/pom.xml        ./finance-tracker-domain/pom.xml
COPY finance-tracker-application/pom.xml   ./finance-tracker-application/pom.xml
COPY finance-tracker-infrastructure/pom.xml ./finance-tracker-infrastructure/pom.xml
COPY finance-tracker-events/pom.xml        ./finance-tracker-events/pom.xml
COPY finance-tracker-api/pom.xml           ./finance-tracker-api/pom.xml
COPY finance-tracker-tests/pom.xml         ./finance-tracker-tests/pom.xml

# Download dependencies (cached layer unless POM changes)
RUN mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || true

# Copy source
COPY finance-tracker-domain/src        ./finance-tracker-domain/src
COPY finance-tracker-application/src   ./finance-tracker-application/src
COPY finance-tracker-infrastructure/src ./finance-tracker-infrastructure/src
COPY finance-tracker-events/src        ./finance-tracker-events/src
COPY finance-tracker-api/src           ./finance-tracker-api/src

# Build (skip tests so integration tests don't run without a DB at build time)
RUN mvn clean package -pl finance-tracker-api -am -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="FlowBridge Team <dev@flowbridge.org>"
LABEL org.opencontainers.image.title="FlowBridge Finance Tracker"
LABEL org.opencontainers.image.description="Production-grade personal finance platform with FlowBridge event bus"
LABEL org.opencontainers.image.version="1.0.0-SNAPSHOT"

# Create non-root user for security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser
WORKDIR /app

# Copy runnable fat jar from build stage
COPY --from=build /workspace/finance-tracker-api/target/finance-tracker-api-*-exec.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

# API port and management/actuator port
EXPOSE 8080 8081

# JVM tuning: use container-aware memory limits, enable JIT for throughput
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
