FROM openjdk:21
LABEL authors="simba"

WORKDIR /main

COPY target/mapService.jar ./myapp.jar

EXPOSE 8020

ENTRYPOINT ["java", "-jar", "myapp.jar"]