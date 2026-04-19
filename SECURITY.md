# Security Policy

## Reporting Vulnerabilities

If you discover a vulnerability, please avoid opening a public issue first.

Instead:

- contact the repository maintainer directly
- provide a short description of the issue
- include affected version and environment details
- include safe reproduction steps when possible

## Scope

`rpc-converter` is a build-time code generation plugin and a small runtime support library.

Security-relevant areas include:

- malicious or unexpected classpath scanning behavior
- unsafe reflective access in generated/runtime code
- generated code that can corrupt payload boundaries
- unsafe service loading or resource registration

## Out of Scope

The project does not provide:

- authentication
- authorization
- encryption
- secret management
- sandboxing of user DTO classes

Those concerns belong to the consuming application and deployment environment.

## Supported Versions

Only the latest published version is assumed to receive fixes.

## Responsible Use

Before production adoption:

- review generated source output
- validate schema compatibility expectations
- test with representative DTOs and payload sizes
- review reflective access requirements in your runtime environment