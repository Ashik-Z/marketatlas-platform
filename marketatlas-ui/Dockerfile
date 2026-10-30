# Stage 1: Build the UI (Vaadin production profile)
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests -Pproduction package

# Stage 2: Run the UI
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/marketatlas-ui-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]