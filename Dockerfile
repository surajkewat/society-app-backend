# -----------------------------------------------------------------------------
# Stage 1: Build the Spring Boot application
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (better layer caching)
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Download dependencies (cached unless pom changes)
RUN ./mvnw dependency:go-offline -B

# Copy source and build (skip tests in image build for speed)
COPY src ./src
RUN ./mvnw package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2: Minimal image to run the application
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
WORKDIR /app

# Copy only the built jar from builder
COPY --from=builder /app/target/*.jar app.jar

USER app

EXPOSE 8080

# Override with env when running (e.g. SERVER_PORT, SPRING_DATASOURCE_URL)
ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]
