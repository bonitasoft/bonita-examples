FROM openjdk:8-jdk-alpine

# /build folder is necessary when testing with H2 database (not for production):
RUN mkdir /build
VOLUME /build

# create a dedicated user to restrict system rights:
RUN addgroup -S bonita && adduser -S bonita -G bonita
USER bonita:bonita

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]