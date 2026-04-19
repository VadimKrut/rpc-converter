# Contributing

Thank you for your interest in `rpc-converter`.

This project focuses on build-time generation of SBE schemas and Java codecs from DTO definitions. The priorities are:

- predictable code generation
- explicit validation of unsupported DTO shapes
- maintainable runtime API
- clear documentation for users and contributors

## Core Principles

### 1. Fail early

If a DTO shape cannot be represented safely, the build should fail with a clear message instead of generating ambiguous
or partially valid output.

### 2. Keep generated output deterministic

Changes to field ordering, schema naming, generated class names, or resource layout should be intentional and covered by
tests.

### 3. Favor explicitness over hidden magic

The plugin should stay understandable to users reading their build output and to contributors debugging generation
failures.

### 4. Document public API changes

When changing annotations, runtime contracts, plugin parameters, or generated resource layout, update:

- Javadoc
- `README.md`
- docs under `docs/` when behavior or workflow changes

## Pull Request Expectations

Before opening a PR:

- explain the problem being solved
- include tests for generation, validation, or runtime behavior
- update documentation for user-visible changes
- mention any compatibility impact on generated source names, resource paths, or runtime loading

## Development Workflow

Common commands:

```bash
mvn test
mvn verify
```

Areas worth checking after changes:

- schema XML shape
- generated codec compilation
- `META-INF/services` registration
- runtime encode/decode behavior

## Commit Style

Prefer short, specific commit messages, for example:

```text
Document GeneratedCodec runtime contract
Add source and javadoc artifacts to Maven build
Clarify fixed-length nested DTO rules
```

## Documentation Style

Use concise technical language. When possible, explain:

- what the API or rule does
- when to use it
- what fails and why

Bilingual documentation is acceptable, but each document should remain readable as a standalone text.