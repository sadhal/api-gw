FROM openjdk:11.0-slim
# Make port 8080 available to the world outside this container
EXPOSE 8080 80
VOLUME /tmp
# ARG JAR_FILE
# COPY ${JAR_FILE} app.jar
COPY build/libs/api-gw-0.0.2-SNAPSHOT.jar app.jar
RUN chmod ugo+x app.jar
USER 1001

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

##   gradle build -x test
##   podman build -t sadhal/api-gw:0.0.2 .
##   podman tag sadhal/api-gw:0.0.2 docker.io/sadhal/api-gw:0.0.2
##   podman push docker.io/sadhal/api-gw:0.0.2