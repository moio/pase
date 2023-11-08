FROM registry.suse.com/bci/openjdk-devel:11 AS builder
WORKDIR /src
COPY . .
RUN zypper -n refresh
RUN zypper -n install npm
RUN npm install ./frontend
RUN ./build.sh

FROM registry.suse.com/bci/openjdk:11
LABEL description="Pa(tch)Se(arch) is an experimental search engine for code allowing search by patch."
LABEL version="0.1"
WORKDIR /opt/pase
COPY --from=builder /src/target/pase-*.jar pase.jar
ENTRYPOINT ["java", "-jar", "pase.jar"]
