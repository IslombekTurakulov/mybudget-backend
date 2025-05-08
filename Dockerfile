FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
ARG IMAGE_TAG=latest
ENV IMAGE_TAG=$IMAGE_TAG
RUN gradle clean shadowJar --no-daemon --info

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/MyBudget-backend-${IMAGE_TAG}-all.jar ./app.jar
COPY --from=build /app/src/main/resources/application.conf ./application.conf
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]