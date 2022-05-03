## Setting up the Record and Playback Wrappers

The documentation for the core Clarity Java Client can be found at
https://crukci-bioinformatics.github.io/clarityclient

### Spring Configuration

The main Spring configuration file for the Clarity client is:

```
/org/cruk/clarity/api/clarity-client-context.xml
```

You will need to add one of the wrappers to your application context path
to activate the wrapper. For recording messages from a server, also include:

```
/org/cruk/clarity/api/clarity-record-context.xml
```

To use prerecorded messages, include:

```
/org/cruk/clarity/api/clarity-playback-context.xml
```

Add the appropriate file to your Spring application context path. If you
wish to use the caching feature too, you'll also need to add:

```
/org/cruk/clarity/api/clarity-cache-context.xml
```

`clarity-client-context.xml` and `clarity-cache-context.xml`
are provided in the main client's JAR file.
`clarity-record-context.xml` and `clarity-playback-context.xml` are
provides in the recorder's JAR file.

It is recommended to use the Spring Test module (`spring-test`). Our tests
are written for JUnit 5, and for these there are some convenience Spring
configuration classes provided to help with this. A test class needs to be annotated
to use the Spring test system and initialised with a recording or playback configuration:

```Java
@SpringJUnitConfig(locations = {
    "classpath:/org/cruk/clarity/api/clarity-client-context.xml",
    "classpath:/org/cruk/clarity/api/clarity-playback-context.xml"
})
```

Replace `clarity-playback-context.xml` with `clarity-record-context.xml`
for recording. Typically one may use the recording class to get the actual results from
a real Clarity server, then switch to the playback configuration to run the tests
from the recorded files. Don't forget to check the recorded files into source control.

Test classes annotated as above become Spring beans themselves, and as such can
have dependencies injected. For example:

```Java
@Autowired
ClarityAPI api;
```

### Setting the Message Directory

The record and playback wrappers around the Clarity client need no additional
configuration beyond what is supplied in the Spring files. The one property that
the user might want to change is the directory the server's messages are written
to, or the pretend server reads from.

By default, both the recording and playback aspects write and read into a
directory `serverexchanges` in the current working directory. This can be
configured by setting the `messageDirectory` property in Spring configuration,
or injecting the aspect into the unit test class code and setting the property:

```Java
@SpringJUnitConfig(locations = {
    "classpath:/org/cruk/clarity/api/clarity-client-context.xml",
    "classpath:/org/cruk/clarity/api/clarity-playback-context.xml"
})
public class MyUnitTest
{
    @Autowired(required = false)
    ClarityAPIPlaybackAspect playbackAspect;

    File recordedMessageDirectory = new File("src/test/servermessages");

    @PostConstruct
    public void setPlaybackDirectory()
    {
        if (playbackAspect != null)
        {
            playbackAspect.setMessageDirectory(recordedMessageDirectory);
        }
    }
}
```

This mechanism works for recording too. Just inject `ClarityAPIRecordingAspect`
instead of `ClarityAPIPlaybackAspect`. This directory is not automatically created,
so when recording make sure that it exists before communicating with the server.

### Setting the Updates Directory during Playback

The playback wrapper returns results from previously recorded calls to
the API's load, retrieve, load, find and list methods, finding the matching
file in the messages directory, unmarshalling its XML and returning the object
as the real API would. It quietly ignores create, delete and upload methods.
Update calls can either by also quietly ignored, or they can be recorded
in a separate directory. If one sets the `updatesDirectory`, any update calls
will have the new versions of the entities written here with an incrementing
index number.

For example, a sample with the id `S1234` has been read from the server
and is recorded in the messages directory in a file `Sample-S1234.xml`.
Each call to the API's `update` or `updateAll` methods for this sample will
result in a file being added to the updates directory. The first will be
`Sample-S1234.000.xml`, a second update will write `Sample-S1234.001.xml`
and so forth. These updates can be checked as part of unit tests.

Update calls are ignored unless the `updatesDirectory` is set, either in
Spring configuration or in code. Following from the example above, the latter
would be done thus:

```Java
@PostConstruct
public void setPlaybackDirectory()
{
    if (playbackAspect != null)
    {
        playbackAspect.setMessageDirectory(recordedMessageDirectory);
        playbackAspect.setUpdatesDirectory(new File("target/updates"));
    }
}
```
