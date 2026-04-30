# Build stage — run from backend/ root: docker build -f borrow-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY borrow-service/pom.xml borrow-service/
RUN mvn -pl borrow-service dependency:go-offline -q
COPY borrow-service/src borrow-service/src
RUN mvn -pl borrow-service package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup -u 1000
USER 1000:1000
COPY --from=builder /build/borrow-service/target/borrowService.jar app.jar
EXPOSE 8030
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8030/actuator/health || exit 1
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
