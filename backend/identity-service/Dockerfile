FROM openjdk:21
LABEL authors="simba"

WORKDIR /main

COPY target/identityService.jar ./myapp.jar

EXPOSE 8050

ENTRYPOINT ["java", "-jar", "myapp.jar"]