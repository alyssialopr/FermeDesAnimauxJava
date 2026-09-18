# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Etape 1 : compilation
# Les dependances sont resolues avant la copie des sources : modifier du code
# ne relance pas le telechargement de tout le repository Maven.
# ---------------------------------------------------------------------------
FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests clean package \
    && mv target/ferme-des-animaux-*.jar target/app.jar

# Decoupage du jar en couches Docker (dependances / code applicatif) : un
# changement de code ne reconstruit que la derniere couche.
RUN java -Djarmode=tools -jar target/app.jar extract --layers --launcher --destination extracted

# ---------------------------------------------------------------------------
# Etape 2 : execution
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S ferme && adduser -S -G ferme ferme \
    && apk add --no-cache curl

WORKDIR /app

COPY --from=build --chown=ferme:ferme /build/extracted/dependencies/ ./
COPY --from=build --chown=ferme:ferme /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=ferme:ferme /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=ferme:ferme /build/extracted/application/ ./

USER ferme

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness | grep -q '"status":"UP"'

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
