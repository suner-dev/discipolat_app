FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY target/discipolat-backend-*.jar app.jar

RUN mkdir -p /app/keys
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
