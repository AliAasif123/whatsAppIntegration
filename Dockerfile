FROM openjdk:11
ADD target/web-services.jar web-services.jar
ENTRYPOINT [ "java","-jar","/web-services.jar" ]