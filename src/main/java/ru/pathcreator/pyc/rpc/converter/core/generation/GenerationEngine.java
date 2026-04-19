package ru.pathcreator.pyc.rpc.converter.core.generation;

import ru.pathcreator.pyc.rpc.converter.core.analysis.StrategyPlanner;
import ru.pathcreator.pyc.rpc.converter.core.model.*;

import java.util.ArrayList;
import java.util.List;

public final class GenerationEngine {

    private final StrategyPlanner planner = new StrategyPlanner();
    private final SbeXmlGenerator xmlGenerator = new SbeXmlGenerator();
    private final SbeCodecGenerator sbeCodecGenerator = new SbeCodecGenerator();
    private final RegistryGenerator registryGenerator = new RegistryGenerator();

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