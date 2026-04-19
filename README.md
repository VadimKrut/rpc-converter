# rpc-converter

`rpc-converter` is a Maven plugin that discovers DTO classes annotated with `@RpcSbe`,
analyzes whether they can be represented as strict SBE messages, and generates:

- SBE XML schemas under `META-INF/rpc-converter/sbe/...`
- Java codec implementations for encode/decode at runtime
- a `ServiceLoader` registry for generated codecs

The project is designed for build-time generation. DTO constraints are checked during the Maven build so schema or
runtime incompatibilities fail early.

## Why

Use `rpc-converter` when you want:

- DTO-first schema generation instead of hand-writing SBE XML
- reproducible codecs generated during the build
- strict validation of supported DTO shapes
- a lightweight runtime API for loading generated codecs

## Current Scope

The current generator targets strict SBE-compatible DTOs with these rules:

- root DTOs must be annotated with `@RpcSbe`
- nested DTOs must be fixed-size only
- root DTOs may contain variable-length `String` and `byte[]`
- nested DTOs may use fixed-length `String` or `byte[]` via `@RpcFixedLength`
- records and classes with no-args constructors are supported for decode

Unsupported shapes currently include collections, maps, optionals, abstract/interface fields, `Object`, temporal
types, and recursive DTO graphs.

## Requirements

- Java 25+
- Maven 3.9+

## Installation

### Maven via GitVerse

```xml

<pluginRepositories>
    <pluginRepository>
        <id>gitverse</id>
        <url>https://gitverse.ru/api/packages/VadimKrut/maven/</url>
    </pluginRepository>
</pluginRepositories>
```

Add the plugin to your build:

```xml

<build>
    <plugins>
        <plugin>
            <groupId>ru.pathcreator.pyc</groupId>
            <artifactId>rpc-converter</artifactId>
            <version>0.1.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Maven via GitHub Packages

GitHub Packages requires authentication even for reads. Add a token with `read:packages` to Maven `settings.xml`, then
configure:

```xml

<pluginRepositories>
    <pluginRepository>
        <id>github</id>
        <url>https://maven.pkg.github.com/VadimKrut/rpc-converter</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```

By default the plugin writes generated sources to:

- `target/generated-sources/rpc-converter`
- `target/generated-resources/rpc-converter`

and compiles generated Java classes into the project's output directory.

## Quick Start

Annotate a root DTO:

```java
import ru.pathcreator.pyc.rpc.converter.annotations.RpcFieldOrder;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcFixedLength;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcSbe;

@RpcSbe(schemaName = "OrderSnapshot")
public final class OrderDto {
    @RpcFieldOrder(0)
    public long orderId;

    @RpcFieldOrder(1)
    public Side side;

    @RpcFieldOrder(2)
    public Header header;

    @RpcFieldOrder(3)
    public String symbol;
}

public final class Header {
    @RpcFixedLength(8)
    public String venue;
}
```

Run Maven:

```bash
mvn compile
```

Then load generated codecs at runtime:

```java
GeneratedCodecRegistry registry = GeneratedCodecRegistry.load();
GeneratedCodec<OrderDto> codec = registry.codecFor(OrderDto.class);

byte[] payload = codec.encodeToBytes(order);
OrderDto decoded = codec.decode(payload, 0, payload.length);
```

## Configuration

The Maven goal is `rpc-converter:generate`.

Supported plugin parameters:

| Parameter                     | Property               | Default                                                        | Description                                    |
|-------------------------------|------------------------|----------------------------------------------------------------|------------------------------------------------|
| `generatedPackage`            | `rpc.generatedPackage` | `${project.groupId}.rpc.converter.generated`                   | Package for generated Java sources             |
| `compilerRelease`             | -                      | `${maven.compiler.release}`                                    | Java release used to compile generated classes |
| `generatedSourcesDirectory`   | -                      | `${project.build.directory}/generated-sources/rpc-converter`   | Output directory for `.java` files             |
| `generatedResourcesDirectory` | -                      | `${project.build.directory}/generated-resources/rpc-converter` | Output directory for generated resources       |

## Runtime API

The runtime module included in this artifact exposes:

- `GeneratedCodec<T>` for encode/decode operations
- `GeneratedCodecRegistry` for `ServiceLoader`-based codec discovery
- `GeneratedCodecFactory` for generated registry implementations
- `BinaryIo` and `ReflectionSupport` used by generated code

## Project Layout

- `src/main/java/.../annotations` - DTO annotations used by application code
- `src/main/java/.../runtime` - minimal runtime API consumed by generated codecs
- `src/main/java/.../converter` - analysis, schema generation, source generation, and Maven integration
- `docs/USAGE.md` - end-to-end usage notes and DTO rules
- `docs/ARCHITECTURE.md` - internal pipeline overview

## Build

Run tests:

```bash
mvn test
```

Build the plugin, source jar, and javadoc jar:

```bash
mvn verify
```

## Publishing Checklist

Before pushing a release:

1. Update the version in `pom.xml`.
2. Verify SCM and repository URLs.
3. Run `mvn verify`.
4. Tag the release in git.
5. Push the tag and publish to the target Maven repository.

## CI/CD

GitHub workflows in `.github/workflows/` provide:

- Maven `verify` on push and pull request
- package publishing to GitHub Packages on `main` / `master`
- Javadoc publishing to GitHub Pages

GitVerse workflows in `.gitverse/workflows/` provide:

- Maven `verify` on push and pull request
- Maven `deploy` to GitVerse Package Registry on `main` / `master`

Required secrets:

- GitHub Packages uses the built-in `GITHUB_TOKEN`
- GitVerse publishing expects repository secret `GITVERSE_MAVEN_TOKEN`

## Documentation

See:

- [docs/USAGE.md](docs/USAGE.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)

## License

Apache License 2.0