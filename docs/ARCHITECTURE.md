# Architecture

## Overview

`rpc-converter` has three main layers:

1. annotation and runtime API
2. analysis and generation pipeline
3. Maven integration

## Annotation and Runtime API

Packages:

- `ru.pathcreator.pyc.rpc.converter.annotations`
- `ru.pathcreator.pyc.rpc.converter.runtime`

These classes are consumed by user code and by generated codecs.

## Analysis Pipeline

The analysis flow is:

1. `RpcConverterMojo` discovers compiled classes annotated with `@RpcSbe`
2. `StrategyPlanner` validates whether a root DTO can be represented as SBE
3. `DtoIntrospector` converts Java reflection metadata into `TypeSpec` / `FieldSpec`
4. `GenerationEngine` coordinates XML and codec generation

Important model objects:

- `TypeSpec` describes a DTO and its fields
- `FieldSpec` describes one field and its compatibility category
- `AnalysisResult` stores the planning outcome and problems

## Generation

For each accepted DTO:

- `SbeXmlGenerator` writes schema XML
- `SbeCodecGenerator` writes a Java codec implementation

After all DTOs are processed:

- `RegistryGenerator` writes a `GeneratedCodecFactory` implementation
- a service registration resource is emitted

## Runtime Discovery

Generated factories are loaded through `ServiceLoader`.

`GeneratedCodecRegistry.load()` aggregates codec instances by Java type and exposes typed lookup.

## Design Constraints

The project intentionally keeps the runtime side small:

- generated code should depend on a narrow helper API
- reflection is concentrated in utility points
- unsupported DTO shapes are rejected during generation rather than handled dynamically at runtime