FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /app

COPY . .

RUN gradle :grpc-service:build --no-daemon --build-cache -x test

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/grpc-service/build/libs/grpc-service.jar /app/grpc-service.jar

EXPOSE 9090


ENTRYPOINT ["java", "-jar", "/app/grpc-service.jar"]