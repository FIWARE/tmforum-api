# legacy-loader needs Micronaut 3 to load the 1.2.11 mapping library, so it is built from the last Micronaut 3 release.
# That release targets Java 17 and its lombok does not run on newer JDKs.
# LEGACY_LOADER_REPO and LEGACY_LOADER_REF come from data-migrator/pom.xml.
FROM maven:3.9-eclipse-temurin-17 AS legacy

ARG LEGACY_LOADER_REPO
ARG LEGACY_LOADER_REF

WORKDIR /build
RUN test -n "$LEGACY_LOADER_REPO" && test -n "$LEGACY_LOADER_REF" \
    && git clone --depth 1 --branch "$LEGACY_LOADER_REF" "$LEGACY_LOADER_REPO" . \
    && mvn -B -pl data-migrator/legacy-loader -am package -DskipTests -DskipITs -Dk3s.skip=true \
    && cp data-migrator/legacy-loader/target/legacy-loader-*.jar /legacy-loader.jar

FROM eclipse-temurin:25-jdk

WORKDIR /app

ENV READ_BROKER="http://localhost:8080"
ENV WRITE_BROKER="http://localhost:1026"

COPY --from=legacy /legacy-loader.jar ./legacy-loader.jar
COPY target/update-writer-${project.version}.jar ./update-writer.jar
COPY target/migrator-${project.version}.jar ./migrator.jar

# force shell usage, to have the env vars expanded
CMD ["sh", "-c", "java --add-opens java.base/java.lang=ALL-UNNAMED -jar ./migrator.jar -rB $READ_BROKER -wB $WRITE_BROKER -ll ./legacy-loader.jar -uw ./update-writer.jar" ]
