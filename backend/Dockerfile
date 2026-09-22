# Image 3 couches : (1) build Maven dans l'image, (2) runtime JRE seul,
# (3) config via env Render/compose. Reproductible partout (CI, Render,
# staging) : ne dépend plus d'un target/ pré-construit sur la machine hôte.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN (command -v mvn >/dev/null 2>&1 || (apt-get update -qq && apt-get install -y -qq maven >/dev/null 2>&1)) \
    && mvn -q -DskipTests package \
    && cp target/discipolat-backend-*.jar /build/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /build/app.jar app.jar

RUN mkdir -p /app/keys
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
