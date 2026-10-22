# TriVoKo backend image (Phase 7 load test; Phase 12 launch builds on it).
# Build the jar first:   cd backend && ./mvnw -q -DskipTests package
# Then:                  docker build -t trivoko-backend backend
# Settings (database, secrets, ports) come from environment variables - nothing secret is baked into the image.
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/trivoko-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# a small container still gets a sensible share of memory
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
