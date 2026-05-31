# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom first (layer cache — only re-downloads deps when pom changes)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S jullyscraft && adduser -S jullyscraft -G jullyscraft

# curl for health checks
RUN apk add --no-cache curl

# Copy JAR from build stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R jullyscraft:jullyscraft /app

# Create required directories
RUN mkdir -p /app/logs /app/uploads /app/config && \
    chown -R jullyscraft:jullyscraft /app/logs /app/uploads /app/config

USER jullyscraft

# ✅ Expose port — Render needs this to detect the port
EXPOSE 8080

# ✅ JVM tuned for 512MB free tier container
ENV JAVA_OPTS="-Xmx300m -Xms128m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# ✅ Uses JAVA_OPTS so memory limits apply
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]