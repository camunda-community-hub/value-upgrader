
FROM eclipse-temurin:23-jre

EXPOSE 8080
COPY target/kiwi-value-assistant-*.jar /kiwi-value-assistant.jar
COPY pom.xml /pom.xml


WORKDIR  /


ENTRYPOINT ["java","-jar","/kiwi-value-assistant.jar"]