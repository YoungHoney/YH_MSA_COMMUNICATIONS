FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app
COPY . .

RUN gradle :rest-service:build --no-daemon --build-cache -x test


FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/rest-service/build/libs/rest-service.jar /app/rest-service.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/rest-service.jar"]