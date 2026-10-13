# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S skillnapse && adduser -S skillnapse -G skillnapse

WORKDIR /app

COPY --from=build --chown=skillnapse:skillnapse /build/target/*.jar app.jar

USER skillnapse

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
