# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Cache dependencies layer separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the fat jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Render injects PORT — app.yml reads it via ${PORT:8081}
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]