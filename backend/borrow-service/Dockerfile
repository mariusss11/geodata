FROM openjdk:21
LABEL authors="simba"

# create a user
RUN useradd -ms /bin/bash myuser
USER myuser

WORKDIR /main

COPY target/borrowService.jar ./myapp.jar

EXPOSE 8030

ENTRYPOINT ["java", "-jar", "myapp.jar"]