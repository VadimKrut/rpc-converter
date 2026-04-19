package ru.pathcreator.pyc.rpc.converter.core.model;

import java.util.List;

public record TypeSpec(
        Class<?> javaType,
        String schemaName,
        InstantiationStyle instantiationStyle,
        List<FieldSpec> fields
) {
}