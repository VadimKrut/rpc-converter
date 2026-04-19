package ru.pathcreator.pyc.rpc.converter.core.model;

import java.util.List;

public record AnalysisResult(
        Class<?> rootType,
        TypeSpec rootSpec,
        AnalysisStrategy strategy,
        List<String> problems
) {
}