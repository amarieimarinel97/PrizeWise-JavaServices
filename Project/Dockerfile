FROM maven:3.6.3-jdk-11
EXPOSE 8080
COPY ./target/crawler-1.0.jar /project/crawler.jar
WORKDIR /project

CMD ls
CMD java -jar crawler.jar

# RUN mvn clean package spring-boot:repackage -DskipTests
#docker build -t java:tag .
#docker run -it --network diploma-proj-net -p 8080:8080 --name diploma-proj-java --rm java:tag
