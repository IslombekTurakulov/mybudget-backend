FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon
COPY src/main/resources/application.conf app/application.conf

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/MyBudget-backend-1.0.0.jar ./MyBudget-backend-1.0.0.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "MyBudget-backend-1.0.0.jar"]