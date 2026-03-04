# ── Stage 1: Build ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build (frontend-maven-plugin downloads Node automatically)
COPY src ./src
COPY frontend ./frontend
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create data directory for SQLite database
RUN mkdir -p /app/data

COPY --from=build /app/target/utem-core-0.1.0.jar app.jar

# Store the SQLite DB in /app/data (mounted as a volume for persistence)
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/utem.db

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
