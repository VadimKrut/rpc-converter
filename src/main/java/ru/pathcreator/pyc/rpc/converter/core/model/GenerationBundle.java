package ru.pathcreator.pyc.rpc.converter.core.model;

import java.util.List;

/**
 * Full output of one generation run.
 *
 * @param javaSources generated Java compilation units
 * @param resources   generated non-Java resources to be written into the build
 */
public record GenerationBundle(
        List<JavaSourceArtifact> javaSources,
        List<ResourceArtifact> resources
) {
}