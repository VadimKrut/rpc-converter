package ru.pathcreator.pyc.rpc.converter.core.model;

public record JavaSourceArtifact(
        String fullyQualifiedClassName,
        String sourceCode
) {
}