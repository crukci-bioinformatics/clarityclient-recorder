# CRUK-CI Genologics API Java Client Recorder

The Cancer Research UK Cambridge Institute (CRUK-CI) Clarity Java Client
provides the Java or Groovy developer a means to work with
[Clarity's REST API](https://d10e8rzir0haj8.cloudfront.net/6.0/REST.html) using objects
rather than XML or DOM document trees. This repository provides a wrapper
around our [Clarity Java Client](https://github.com/crukci-bioinformatics/clarityclient)
that allows the developer to record entities returned from a real Clarity
server into XML files on disk that can then be used to provide unit tests
without requiring access to that server and provides fixed replies that
will not change as the server does.

## Building

Having got this check out of the code, run:

```
mvn install
```

This will build and install the project into your local Maven cache.
You'll need Maven 3.5 or newer.

Alternatively, you can add our Maven repository to your POM and let
Maven do the work. Add a &lt;repositories&gt; section containing:

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

## Usage

Add the JAR file file of the main Clarity client and the JAR of this project
to your POM (I'm assuming you're using Maven now):

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

_Fill in the <version> tag with the version of the API._
_For this branch, and code using EE8, the version should start "2.31.ee8"._

For details of using the API, please refer to the documentation at
<http://crukci-bioinformatics.github.io/clarityclient-recorder>
