FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon
WORKDIR /app
COPY src/main/resources/application.conf /app/application.conf
COPY src/main/resources/hikari.properties /app/hikari.properties

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"] 