# syntax=docker/dockerfile:1
#
# Discipolat — backend API image.
#
# MULTI-STAGE, et c'est délibéré : la version précédente faisait
# `COPY target/discipolat-backend-*.jar app.jar`, ce qui suppose qu'un JAR a
# déjà été compilé sur la machine. `target/` est gitignoré, donc sur un checkout
# propre — exactement ce que fait Render — ce COPY n'a aucune source et le build
# échoue. L'image ne pouvait donc être produite que par GitHub Actions, puis
# tirée depuis GHCR : le deployment dépendait de la CI.
#
# Ici l'image est auto-suffisante : Render compile le backend depuis les sources.
# La CI devient un garde-fou de non-régression, plus un prérequis de deployment.
#
# Build context attendu : ./backend (cf. render.yaml → dockerContext).

# ── Stage 1 : compilation ───────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Les dépendances d'abord, dans une couche à part : elle n'est invalidée que
# lorsque pom.xml change, pas à chaque modification de code. Le rebuild après
# un simple changement de service ne retélécharge donc rien.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ── Stage 2 : runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Utilisateur non privilégié : une web service exposée en Internet n'a aucune
# raison de tourner en root.
RUN useradd --system --uid 1001 --create-home --shell /usr/sbin/nologin app \
 && mkdir -p /app/keys \
 && chown -R app:app /app

COPY --from=build --chown=app:app /build/target/discipolat-backend-*.jar /app/app.jar

USER app

EXPOSE 8080

# MaxRAMPercentage plutôt qu'un -Xmx figé : le tier Free Render donne 512 MB, le
# Starter 2 GB. Le même Dockerfile s'adapte au plan sans être réécrit.
# ExitOnOutOfMemoryError : en cas de pression mémoire, le process meurt et Render
# le redémarre proprement, au lieu de laisser un JVM en sueur servir des réponses
# lentes. Le chemin de code le plus fragile (charge Hibernate + SSE) préfère un
# redémarrage à un degrade silencieux.
ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=70", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-XX:+UseContainerSupport", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "/app/app.jar"]
