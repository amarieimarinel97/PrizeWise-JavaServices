#mvn clean package spring-boot:repackage
#E:\\Tools\\JDKs\\jdk-11.0.4\\bin\\java -jar target/crawler-1.0-SNAPSHOT.jar

FROM maven:3.6.3-jdk-11
EXPOSE 8080
COPY . /project
WORKDIR /project
RUN sed -i 's/localhost:5432/diploma-proj-postgres:5432/g' src/main/resources/application.properties
RUN sed -i 's/localhost:8081/diploma-proj-python:8081/g' src/main/resources/application.properties
RUN cat src/main/resources/application.properties

RUN mvn clean package spring-boot:repackage
CMD java -jar target/crawler-1.0-SNAPSHOT.jar

#docker build -t java:tag .
#docker run -it --network diploma-proj-net -p 8080:8080 --name diploma-proj-java --rm java:tag

