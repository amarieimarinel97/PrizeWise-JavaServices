#mvn clean package spring-boot:repackage

#E:\\Tools\\JDKs\\jdk-11.0.4\\bin\\java -jar target/crawler-1.0-SNAPSHOT.jar

FROM maven:3.6.3-jdk-11
EXPOSE 8080
COPY . /project
WORKDIR /target
CMD java -jar target/crawler-1.0-SNAPSHOT.jar

#docker build -t java:tag .
#docker run -it --publish 8080:8080 --name diploma-proj-java --rm java:tag

