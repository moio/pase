## Building
 - install JDK 11 (tested with openJDK)
 - install a recent Maven version (tested with 3.6.1)
 - install a recent npm version (tested with 6.14.4)

To build, execute:
```
./build.sh
```

Executable jar will be produced in `target/pase-<version>.jar`

## Developing

Any Java IDE can be used, IntelliJ IDEA is recommended and configuration files are provided.

## Smoke testing

```
mvn clean install
java -jar target/pase-*.jar index src/test/resources/sources/ ./index
java -jar target/pase-0.1-SNAPSHOT.jar search ./index src/test/resources/patches/CVE-2017-5638.patch
```

## Running JUnit tests

To just run tests, execute:
```
mvn test
```

## Building and running as Docker containers

To ease experimentation with PaSe, use the bundled `Dockerfile` and `compose.yaml`
to build and run a pair of Docker containers (one for the source indexer, one for the web server).

Edit the paths to the directory containing the sources (`$SRCDIR`) and to where the index will be
written (`$INDEXDIR`) on the host in file `.env`, then run `docker compose up`.

This will build and run a pair of containers named `pase-indexer-1` and `pase-server-1`.
The indexer will scan and index the directory containing the sources, then exit.
The server will listen indefinitely for requests targeting the index at http://localhost:4567/.
