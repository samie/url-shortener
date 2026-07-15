# Stage 1: Download Jetty
FROM eclipse-temurin:26-jre AS jetty-download

ARG JETTY_VERSION=12.0.36

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN wget -q https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/${JETTY_VERSION}/jetty-home-${JETTY_VERSION}.tar.gz \
         -O /tmp/jetty.tar.gz \
    && mkdir -p /opt/jetty-home \
    && tar xfz /tmp/jetty.tar.gz -C /opt/jetty-home --strip-components=1 \
    && rm /tmp/jetty.tar.gz

# Stage 2: Build WAR
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace

COPY pom.xml .
COPY urlshortener-core/pom.xml urlshortener-core/pom.xml
COPY urlshortener-client/pom.xml urlshortener-client/pom.xml
COPY urlshortener-ui/pom.xml urlshortener-ui/pom.xml
COPY urlshortener-server/pom.xml urlshortener-server/pom.xml

RUN mvn -pl urlshortener-ui -am dependency:go-offline -q

COPY urlshortener-core/src urlshortener-core/src
COPY urlshortener-client/src urlshortener-client/src
COPY urlshortener-ui/src urlshortener-ui/src

RUN mvn -pl urlshortener-ui -am -Dmaven.test.skip=true clean package

# Stage 3: Runtime
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=jetty-download /opt/jetty-home /opt/jetty-home

ENV JETTY_HOME=/opt/jetty-home
ENV JETTY_BASE=/opt/jetty-base

# http-forwarded lets Jetty honor X-Forwarded-* from the ingress (TLS is
# terminated at nginx/Traefik). Without it request.isSecure() is false behind
# the proxy, so Vaadin negotiates its wss:// push connection as insecure and
# the WebSocket handshake breaks (browser: ERR_UNKNOWN_URL_SCHEME).
RUN mkdir -p ${JETTY_BASE} \
    && cd ${JETTY_BASE} \
    && java -jar ${JETTY_HOME}/start.jar \
        --add-modules=http,http-forwarded,ee10-deploy,ee10-annotations,ee10-webapp

# deploy at /management so the context path matches the ingress prefix (no rewriting)
COPY --from=build /workspace/urlshortener-ui/target/ROOT.war ${JETTY_BASE}/webapps/management.war

EXPOSE 8080

WORKDIR ${JETTY_BASE}

CMD ["java", "-Djetty.http.host=0.0.0.0", "-Djetty.http.port=8080", "-jar", "/opt/jetty-home/start.jar"]
