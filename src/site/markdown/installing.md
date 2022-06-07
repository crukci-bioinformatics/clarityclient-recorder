## Installation

The record and playback wrapper can be used by including its JAR file and
its dependencies in your class path.

### With Maven

The client can be added as a dependency to your POM:

```XML
<dependency>
    <groupId>org.cruk.clarity</groupId>
    <artifactId>clarity-client</artifactId>
    <version>...</version>
</dependency>
<dependency>
    <groupId>org.cruk.clarity</groupId>
    <artifactId>clarity-client-recorder</artifactId>
    <version>...</version>
    <scope>test</scope>
</dependency>
```

(Obviously, update the version number as later versions appear.)

We here at CRUK-CI's bioinformatics group host a Maven repository for
our code. You can let Maven do the work and fetch the client from us if
you add our repository's details to your POM:

```XML
<repository>
    <id>crukci-bioinformatics</id>
    <url>https://content.cruk.cam.ac.uk/bioinformatics/maven</url>
    <releases>
        <enabled>true</enabled>
    </releases>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
</repository>
```

Including the recorder in your POM will also fetch a suitable version
of the Clarity client JAR, but you will almost certainly want to include
that JAR with _compile_ scope. This wrapper is designed for tests, hence
including both JARs in the example above with different scopes.

You will also need to provide a JAXB implementation for your tests.
This should be one of the EE8 implementations, namely a Glassfish
implementation version 2.x.x:

```XML
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>2.3.6</version>
    <scope>test</scope>
</dependency>
```

The `com.sun.xml.bind:jaxb-impl` artifacts available in Maven won't
work well with newer JREs.

### Without Maven

If you are not using Maven, you should also fetch this project's
dependencies and add them to your class path. The dependencies are given
in the [dependencies report](dependencies.html).

### Building from source

Assuming you have downloaded the source from GitHub, building the Java
API client is simple. You will need Maven version 3.5 or newer. Then building
is as simple as:

```
mvn install
```

You need to run this command from the project's top level directory. It will
build the client wrapper JAR file and install it in your local Maven cache.
