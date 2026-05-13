
FROM eclipse-temurin:23-jre

EXPOSE 8080
COPY target/value-upgrader-*.jar /value-upgrader.jar
COPY pom.xml /pom.xml


WORKDIR  /


ENTRYPOINT ["java","-jar","/value-upgrader.jar"]