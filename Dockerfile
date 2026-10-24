# Build the React/Vite frontend first, then package its output into Spring Boot static resources.
FROM node:22-alpine AS frontend-build
WORKDIR /app/frontend

COPY frontend/package.json ./
RUN npm install --no-audit --no-fund

COPY frontend/ ./
RUN npm run build

# Compile and package the Spring Boot backend with the frontend in its classpath.
FROM eclipse-temurin:25-jdk AS backend-build
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY gradle.properties build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
# Vite's dist output becomes classpath:/static and is served by Spring Boot.
COPY --from=frontend-build /app/frontend/dist/ src/main/resources/static/
RUN ./gradlew bootJar --no-daemon

# Minimal runtime image; Node.js and build tooling are not included.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 erp

COPY --from=backend-build /app/build/libs/*.jar /app/app.jar
RUN chown -R erp:erp /app
USER erp

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
