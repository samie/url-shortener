# Stage 1: Build
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace

COPY pom.xml .
COPY urlshortener-core/pom.xml urlshortener-core/pom.xml
COPY urlshortener-server/pom.xml urlshortener-server/pom.xml
COPY urlshortener-client/pom.xml urlshortener-client/pom.xml
COPY urlshortener-ui/pom.xml urlshortener-ui/pom.xml

RUN mvn -pl urlshortener-core,urlshortener-server -am dependency:go-offline -q

COPY urlshortener-core/src urlshortener-core/src
COPY urlshortener-server/src urlshortener-server/src

RUN mvn -pl urlshortener-core,urlshortener-server -am -DskipTests clean package

# Stage 2: Runtime
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /workspace/urlshortener-server/target/urlshortener.jar app.jar

VOLUME /app/data

EXPOSE 8081 9090

# containers receive traffic over the pod/bridge network - bind all interfaces
# (the application defaults are localhost for plain `java -jar` runs)
ENV REDIRECT_SERVER_HOST=0.0.0.0 \
    ADMIN_SERVER_HOST=0.0.0.0

ENTRYPOINT ["java", "-jar", "app.jar"]
