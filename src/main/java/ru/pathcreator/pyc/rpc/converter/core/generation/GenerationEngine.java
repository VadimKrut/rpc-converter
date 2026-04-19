package ru.pathcreator.pyc.rpc.converter.core.generation;

import ru.pathcreator.pyc.rpc.converter.core.analysis.StrategyPlanner;
import ru.pathcreator.pyc.rpc.converter.core.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full code-generation pipeline for a batch of root DTOs.
 *
 * <h2>Pipeline</h2>
 *
 * <pre>
 * root classes
 *   -> analysis/planning
 *   -> generated Java codecs
 *   -> generated SBE XML resources
 *   -> generated codec factory + service registration
 * </pre>
 *
 * <p>The engine is intentionally fail-fast: if any root type cannot be
 * generated, the whole invocation fails with a combined diagnostic message so a
 * Maven build does not silently publish a partial result set.</p>
 */
public final class GenerationEngine {

    private final StrategyPlanner planner = new StrategyPlanner();
    private final SbeXmlGenerator xmlGenerator = new SbeXmlGenerator();
    private final SbeCodecGenerator sbeCodecGenerator = new SbeCodecGenerator();
    private final RegistryGenerator registryGenerator = new RegistryGenerator();

    /**
     * Generates all Java and resource artifacts for the supplied root DTOs.
     *
     * @param rootTypes        root DTO classes annotated with {@code @RpcSbe}
     * @param generatedPackage package for generated runtime classes
     * @return bundle of Java sources and resources that should be written to the
     * build output
     */
    public GenerationBundle generate(List<Class<?>> rootTypes, String generatedPackage) {
        final List<AnalysisResult> results = new ArrayList<>();
        final List<JavaSourceArtifact> javaSources = new ArrayList<>();
        final List<ResourceArtifact> resources = new ArrayList<>();
        final List<String> failures = new ArrayList<>();
        for (final Class<?> rootType : rootTypes) {
            final AnalysisResult result = planner.plan(rootType);
            results.add(result);
            if (result.strategy() == AnalysisStrategy.FAIL) {
                failures.add(rootType.getName() + " -> " + String.join("; ", result.problems()));
                continue;
            }
            final String codecSource = sbeCodecGenerator.generate(result.rootSpec(), generatedPackage);
            javaSources.add(new JavaSourceArtifact(generatedPackage + "." + Naming.codecSimpleName(rootType), codecSource));
            resources.add(new ResourceArtifact(
                    "META-INF/rpc-converter/sbe/" + rootType.getName().replace('.', '/') + ".xml",
                    xmlGenerator.generate(result.rootSpec())
            ));
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("RPC converter generation failed:\n" + String.join("\n", failures));
        }
        javaSources.add(new JavaSourceArtifact(
                generatedPackage + ".GeneratedCodecFactoryImpl",
                registryGenerator.generateFactory(generatedPackage, results)
        ));
        resources.add(new ResourceArtifact(
                "META-INF/services/ru.pathcreator.pyc.rpc.converter.runtime.GeneratedCodecFactory",
                generatedPackage + ".GeneratedCodecFactoryImpl\n"
        ));
        return new GenerationBundle(javaSources, resources);
    }
}