package ru.pathcreator.pyc.rpc.converter.core.analysis;

import ru.pathcreator.pyc.rpc.converter.annotations.RpcSbe;
import ru.pathcreator.pyc.rpc.converter.core.model.*;

import java.util.ArrayList;
import java.util.List;

public final class StrategyPlanner {

    private final DtoIntrospector introspector = new DtoIntrospector();

    public AnalysisResult plan(final Class<?> rootType) {
        final RpcSbe annotation = rootType.getAnnotation(RpcSbe.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Missing @RpcSbe on " + rootType.getName());
        }
        final List<String> problems = new ArrayList<>();
        TypeSpec rootSpec;
        try {
            rootSpec = introspector.inspectRoot(rootType);
        } catch (final IllegalStateException e) {
            problems.add(e.getMessage());
            return new AnalysisResult(rootType, null, AnalysisStrategy.FAIL, List.copyOf(problems));
        }
        if (isSbeCompatible(rootSpec, true, problems)) {
            return new AnalysisResult(rootType, rootSpec, AnalysisStrategy.SBE, List.copyOf(problems));
        }
        return new AnalysisResult(rootType, rootSpec, AnalysisStrategy.FAIL, List.copyOf(problems));
    }

    private boolean isSbeCompatible(final TypeSpec spec, final boolean root, final List<String> problems) {
        if (spec.instantiationStyle() == InstantiationStyle.UNSUPPORTED_FOR_SBE) {
            problems.add("SBE decode requires a record or a no-args constructor: " + spec.javaType().getName());
            return false;
        }
        boolean compatible = true;
        for (final FieldSpec field : spec.fields()) {
            compatible &= validateFixedLengthUsage(field, problems);
            compatible &= switch (field.kind()) {
                case PRIMITIVE, BOXED_PRIMITIVE, BOOLEAN, BOXED_BOOLEAN, CHAR, BOXED_CHAR, ENUM, FIXED_STRING,
                     FIXED_BYTES -> true;
                case STRING, BYTES -> root;
                case NESTED_FIXED ->
                        field.nestedType() != null && isFixedOnly(field.nestedType(), problems, field.field().getDeclaringClass().getName() + "." + field.name());
                case UNSUPPORTED -> false;
            };
            if (field.kind() == FieldKind.UNSUPPORTED) {
                problems.add("SBE unsupported field: " + field.field().getDeclaringClass().getName() + "." + field.name() + " : " + field.javaType().getName());
            }
            if (!root && field.variableLength()) {
                problems.add("Nested variable-length field is not supported for SBE inline composite: "
                             + field.field().getDeclaringClass().getName() + "." + field.name());
            }
        }
        return compatible;
    }

    private boolean validateFixedLengthUsage(final FieldSpec field, final List<String> problems) {
        if (field.fixedLength() == null) {
            return true;
        }
        if (field.fixedLength() <= 0) {
            problems.add("SBE fixed-length hint must be positive: "
                         + field.field().getDeclaringClass().getName() + "." + field.name());
            return false;
        }
        if (!field.fixedLengthArray()) {
            problems.add("SBE fixed-length hint is supported only for String and byte[] fields: "
                         + field.field().getDeclaringClass().getName() + "." + field.name());
            return false;
        }
        if (field.fixedLength() > 65534) {
            problems.add("SBE fixed-length hint is too large for generated schema: "
                         + field.field().getDeclaringClass().getName() + "." + field.name());
            return false;
        }
        return true;
    }

    private boolean isFixedOnly(final TypeSpec spec, final List<String> problems, final String path) {
        boolean compatible = true;
        for (final FieldSpec field : spec.fields()) {
            if (field.kind() == FieldKind.NESTED_FIXED) {
                compatible &= isFixedOnly(field.nestedType(), problems, path + "." + field.name());
                continue;
            }
            if (!field.fixedLengthCompatible()) {
                compatible = false;
                problems.add("Nested DTO used by SBE must be fixed-only: " + path + "." + field.name());
            }
        }
        return compatible;
    }
}