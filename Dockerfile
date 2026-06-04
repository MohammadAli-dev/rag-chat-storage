FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/target/ragchat-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
