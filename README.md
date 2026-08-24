# Apache HttpClient Reuse Companion

Warning icon on a `HttpClients.createDefault()`,
`HttpClients.createSystem()`, or `HttpClients.custom().build()`
construction (Apache HttpClient, both the 4.x `org.apache.http` and
5.x `org.apache.hc.client5.http` packages use `HttpClients` as the
factory class) written inside a regular method body — Apache's own
javadoc annotates `CloseableHttpClient`
`@Contract(threading = ThreadingBehavior.SAFE)` and states client
instances "are expected to be thread safe" and "it is recommended
that the same instance of this class is reused for multiple request
executions". Building one inside a regular method means a brand new
connection pool on every call.

## Why it exists

`HttpClients.createDefault()` compiles fine and returns a working
client — call it once per request handler and each call quietly opens
a brand new connection pool instead of reusing the one the application
actually needs.

## Why built this way

- **100% static text/PSI analysis** — matches by simple text, so it
  works whether the real Apache HttpClient jar (4.x or 5.x) is on the
  classpath or not. Java and Kotlin.

## v0.1 scope — stated honestly, not exhaustively

Only flags the "build from scratch" shape — a client obtained by
reference from an existing shared instance/dependency injection is
never flagged (correctly, since it isn't the anti-pattern this plugin
targets).

## Usage

Open any Java/Kotlin file using Apache HttpClient. A client built
inside a regular method shows a warning icon.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
