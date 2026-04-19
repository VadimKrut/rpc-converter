package ru.pathcreator.pyc.rpc.converter.core.model;

import java.util.List;

/**
 * Structural description of one DTO type after reflection analysis.
 *
 * @param javaType           original Java class
 * @param schemaName         schema/composite name used in generated output
 * @param instantiationStyle decode-time construction strategy
 * @param fields             normalized fields in generation order
 */
public record TypeSpec(
        Class<?> javaType,
        String schemaName,
        InstantiationStyle instantiationStyle,
        List<FieldSpec> fields
) {
}