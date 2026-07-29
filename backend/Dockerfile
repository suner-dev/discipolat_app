FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy pre-built JAR from local target directory
COPY target/*.jar app.jar

RUN mkdir -p /app/keys
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
