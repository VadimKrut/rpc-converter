package ru.pathcreator.pyc.rpc.converter.core.generation;

import ru.pathcreator.pyc.rpc.converter.core.model.AnalysisResult;
import ru.pathcreator.pyc.rpc.converter.core.model.AnalysisStrategy;

import java.util.List;

public final class RegistryGenerator {

    public String generateFactory(
            final String packageName,
            final List<AnalysisResult> results
    ) {
        final StringBuilder source = new StringBuilder();
        source.append("package ").append(packageName).append(";\n\n");
        source.append("import ru.pathcreator.pyc.rpc.converter.runtime.GeneratedCodec;\n");
        source.append("import ru.pathcreator.pyc.rpc.converter.runtime.GeneratedCodecFactory;\n");
        source.append("import java.util.List;\n\n");
        source.append("public final class GeneratedCodecFactoryImpl implements GeneratedCodecFactory {\n");
        source.append("    @Override\n");
        source.append("    public List<GeneratedCodec<?>> codecs() {\n");
        source.append("        return List.of(\n");
        boolean first = true;
        for (final AnalysisResult result : results) {
            if (result.strategy() == AnalysisStrategy.FAIL) {
                continue;
            }
            if (!first) {
                source.append(",\n");
            }
            first = false;
            source.append("                new ").append(packageName).append(".").append(Naming.codecSimpleName(result.rootType())).append("()");
        }
        source.append("\n        );\n");
        source.append("    }\n");
        source.append("}\n");
        return source.toString();
    }
}