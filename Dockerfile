FROM maven:3-eclipse-temurin-11-alpine AS builder
WORKDIR /src
COPY . .
RUN apk add --no-cache npm
RUN npm install ./frontend
RUN ./build.sh

FROM eclipse-temurin:11-alpine
LABEL description="Pa(tch)Se(arch) is an experimental search engine for code allowing search by patch."
LABEL version="0.1"
WORKDIR /opt/pase
COPY --from=builder /src/target/pase-*.jar pase.jar
ENTRYPOINT ["java", "-jar", "pase.jar"]
