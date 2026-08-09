# Backend (Spring Boot + node registry), built into one shadow JAR.
# Needs JDK 25: CloudBootstrap.main() is package-private, which only the
# JDK 25 launcher accepts without --enable-preview (JEP 512).
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /src
COPY . .
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:25-jre-jammy
COPY --from=build /src/build/libs/*-all.jar /app/app.jar

# Everything lupo-cloud persists (tasks, templates, servers/, proxies/, backups,
# the admin secret) is written relative to the working directory — keep that
# separate from /app so a named volume on /app/data covers all of it.
WORKDIR /app/data

EXPOSE 8080
ENTRYPOINT ["java", "--sun-misc-unsafe-memory-access=allow", "-jar", "/app/app.jar"]
