# Stage 1: Build the application using Gradle with JDK 21
FROM gradle:8.7-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN chmod +x ./gradlew
RUN ./gradlew build -x test --no-daemon

# Stage 2: Create a lightweight runtime image with JRE 21
FROM eclipse-temurin:21-jre-jammy
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/spring-boot-application.jar

# Start the application
ENTRYPOINT ["java", "-jar", "/app/spring-boot-application.jar"]
