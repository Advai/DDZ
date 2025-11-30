# Multi-stage Dockerfile for Dou Dizhu Game Server
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .

# Copy source code for all modules
COPY engine engine/
COPY server server/

# Make gradlew executable
RUN chmod +x gradlew

# Build the application (skip tests for faster builds)
RUN ./gradlew :server:bootJar -x test --no-daemon

# Stage 2: Create minimal runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S ddz && adduser -S ddz -G ddz

# Copy the built JAR from builder stage
COPY --from=builder /app/server/build/libs/doudizhu-server.jar app.jar

# Copy static web files
COPY web web/

# Change ownership to non-root user
RUN chown -R ddz:ddz /app

# Switch to non-root user
USER ddz

# Expose port 8080 for the application
EXPOSE 8080

# Health check configuration
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Optional: Add JVM options for production
# You can override these when running the container
CMD ["--server.port=8080"]
