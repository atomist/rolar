FROM maven

MAINTAINER Clay McCoy <clay@atomist.com>

RUN mkdir -p /opt/app

WORKDIR /opt/app

EXPOSE 8080

#ENTRYPOINT ["/usr/bin/java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Xmx256m", "-Djava.security.egd=file:/dev/urandom"]

COPY . /opt/app/

RUN mvn package

CMD ["mvn", "spring-boot:run"]
