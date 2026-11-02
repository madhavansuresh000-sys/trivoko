# TriVoKo backend as a Docker image (Phase 7 load test, Phase 12 launch on Render).
# Stage 1 builds the JAR with the Maven wrapper; stage 2 only holds Java + the JAR (smaller, no source code).
# Build:  docker build -t trivoko-backend backend
# Settings (profile, database, secrets) come from environment variables - nothing secret is baked into the image.
# The profile is NOT fixed here: Render sets SPRING_PROFILES_ACTIVE=prod (render.yaml), the load test uses dev.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# dependencies first: this layer is cached as long as pom.xml does not change
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
# the tests already ran in GitHub Actions (they need Docker for MySQL, which a build machine does not have)
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
# run as a normal user, not root: a bug in the app cannot change the container's system files
RUN useradd --system --no-create-home trivoko
COPY --from=build /app/target/trivoko-backend-0.0.1-SNAPSHOT.jar app.jar
USER trivoko
# Render free = 512 MB for everything. 75% for Java, the simple one-thread garbage collector (least memory),
# smaller thread stacks (each Tomcat thread reserves one). The search index goes to /tmp: rebuilt on every start.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k" \
    SEARCH_INDEX_DIR=/tmp/trivoko-search-index
EXPOSE 8080
# Lucene (search) uses Java's native-memory API for fast file access; this allows it without a warning
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/app.jar"]
