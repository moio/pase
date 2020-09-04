## Building
 - install JDK 11 (tested with openJDK)
 - install a recent Maven version (tested with 3.6.1)

To build, execute:
```
mvn clean install
```

Executable jar will be produced in `target/pase-<version>.jar`

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

