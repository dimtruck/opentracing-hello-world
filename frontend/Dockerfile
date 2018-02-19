FROM java:8 

COPY . /opt

WORKDIR /opt

RUN ./gradlew build

EXPOSE 8080

CMD java  -jar /opt/build/libs/frontend-0.1.0.jar

