FROM maven

LABEL maintainer="Clay McCoy <clay@atomist.com>"

RUN mkdir -p /opt/app

WORKDIR /opt/app

EXPOSE 8080

COPY . /opt/app/

RUN mvn package

COPY target/rolar.jar spring-boot.jar

ENTRYPOINT ["/usr/bin/java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Xmx256m", "-Djava.security.egd=file:/dev/urandom"]

CMD ["-jar", "spring-boot.jar"]
