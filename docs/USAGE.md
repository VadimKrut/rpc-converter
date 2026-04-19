# Usage Guide

## Goal

`rpc-converter` turns annotated DTO classes into:

- generated SBE XML schemas
- generated Java codecs
- a `ServiceLoader`-discoverable codec factory

## DTO Rules

### Root DTO

A root DTO must be annotated with `@RpcSbe`.

The root DTO may contain:

- primitives and boxed primitives
- `boolean` / `Boolean`
- `char` / `Character`
- enums
- nested fixed DTOs
- variable-length `String`
- variable-length `byte[]`
- fixed-length `String` and `byte[]` via `@RpcFixedLength`

### Nested DTO

Nested DTOs are inlined into the SBE schema as fixed composites.

That means nested DTOs must not contain:

- variable-length `String`
- variable-length `byte[]`
- collections
- maps
- optionals
- arbitrary object references

If you need text or bytes inside a nested DTO, use `@RpcFixedLength`.

## Annotations

### `@RpcSbe`

Marks a class as an SBE root message candidate.

Optional `schemaName` overrides the default schema/message name derived from the class name.

### `@RpcFieldOrder`

Overrides declaration-order sorting for generated field order.

Lower values come first.

### `@RpcFixedLength`

Declares a fixed-length representation for `String` or `byte[]`.

Use this when a field must remain compatible with SBE fixed-size composite rules.

## Generated Output

For each supported root DTO the plugin produces:

- one Java codec implementation
- one SBE XML resource under `META-INF/rpc-converter/sbe/...`

For the whole generation run it also produces:

- one `GeneratedCodecFactoryImpl`
- one `META-INF/services/ru.pathcreator.pyc.rpc.converter.runtime.GeneratedCodecFactory` entry

## Build Integration

The plugin runs in `process-classes`, so compiled DTO classes already exist and can be scanned from the build output
directory.

Typical flow:

1. project DTOs compile
2. `rpc-converter:generate` scans compiled classes
3. annotated DTOs are analyzed
4. XML/resources and Java codecs are generated
5. generated Java sources are compiled into the same output directory

## Runtime Loading

Use the runtime registry:

```java
GeneratedCodecRegistry registry = GeneratedCodecRegistry.load();
GeneratedCodec<MyDto> codec = registry.codecFor(MyDto.class);
```

## Typical Failure Cases

- missing `@RpcSbe` on a root class
- recursive DTO graph
- nested DTO with variable-length field
- unsupported field type such as `List`, `Map`, `Optional`, `Object`, `Instant`
- DTO that cannot be instantiated during decode
- invalid `@RpcFixedLength` value

## Tips

- keep DTOs simple and explicit
- use `@RpcFieldOrder` only when wire ordering must be controlled manually
- prefer nested fixed DTOs for structured headers
- keep application-facing DTO design aligned with SBE constraints from the start