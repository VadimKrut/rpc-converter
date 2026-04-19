package ru.pathcreator.pyc.rpc.converter.core.model;

import java.util.List;

/**
 * Final outcome of planning for one root DTO.
 *
 * @param rootType analyzed root class
 * @param rootSpec normalized structure, if analysis reached that stage
 * @param strategy selected generation strategy
 * @param problems human-readable diagnostics collected during validation
 */
public record AnalysisResult(
        Class<?> rootType,
        TypeSpec rootSpec,
        AnalysisStrategy strategy,
        List<String> problems
) {
}