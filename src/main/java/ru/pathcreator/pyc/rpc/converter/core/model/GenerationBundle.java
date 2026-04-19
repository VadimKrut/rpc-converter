package ru.pathcreator.pyc.rpc.converter.core.model;

import java.util.List;

public record GenerationBundle(
        List<JavaSourceArtifact> javaSources,
        List<ResourceArtifact> resources
) {
}