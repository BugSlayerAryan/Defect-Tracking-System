# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

ENV TOMCAT_VERSION=10.1.28
ENV CATALINA_HOME=/opt/tomcat

RUN apt-get update && \
    apt-get install -y --no-install-recommends wget ca-certificates && \
    rm -rf /var/lib/apt/lists/*

RUN wget -q https://archive.apache.org/dist/tomcat/tomcat-10/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz && \
    tar -xzf apache-tomcat-${TOMCAT_VERSION}.tar.gz -C /opt && \
    mv /opt/apache-tomcat-${TOMCAT_VERSION} ${CATALINA_HOME} && \
    rm apache-tomcat-${TOMCAT_VERSION}.tar.gz && \
    rm -rf ${CATALINA_HOME}/webapps/ROOT ${CATALINA_HOME}/webapps/docs ${CATALINA_HOME}/webapps/examples

COPY --from=builder /app/target/defect-tracking-system.war ${CATALINA_HOME}/webapps/ROOT.war

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

CMD ["/opt/tomcat/bin/catalina.sh", "run"]
