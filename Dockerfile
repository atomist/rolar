FROM adoptopenjdk/openjdk8-openj9

LABEL maintainer="Atomist <docker@atomist.com>"

# Update aptitude with new repo and install git
RUN apt-get clean && apt-get update && apt-get install -y \
  curl \
  dnsutils \
  openssl \
  jq \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

ENV DUMB_INIT_VERSION=1.2.2

RUN curl -s -f -L -O https://github.com/Yelp/dumb-init/releases/download/v$DUMB_INIT_VERSION/dumb-init_${DUMB_INIT_VERSION}_amd64.deb \
    && dpkg -i dumb-init_${DUMB_INIT_VERSION}_amd64.deb \
    && rm -f dumb-init_${DUMB_INIT_VERSION}_amd64.deb

RUN mkdir -p /opt/app

WORKDIR /opt/app

ENTRYPOINT ["dumb-init", "java"]

CMD ["-Xmx2048m", "-jar", "/opt/app/app.jar"]

EXPOSE 8080
EXPOSE 8081

COPY target/rolar.jar /opt/app/app.jar
