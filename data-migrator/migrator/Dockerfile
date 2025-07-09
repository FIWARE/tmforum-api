FROM eclipse-temurin:17-jdk

WORKDIR /app

ENV READ_BROKER="http://localhost:8080"
ENV WRITE_BROKER="http://localhost:1026"

COPY target/legacy-loader-${project.version}.jar ./legacy-loader.jar
COPY target/update-writer-${project.version}.jar ./update-writer.jar
COPY target/migrator-${project.version}.jar ./migrator.jar

# force shell usage, to have the env vars expanded
CMD ["sh", "-c", "java --add-opens java.base/java.lang=ALL-UNNAMED -jar ./migrator.jar -rB $READ_BROKER -wB $WRITE_BROKER -ll ./legacy-loader.jar -uw ./update-writer.jar" ]