package ru.pathcreator.pyc.rpc.converter.core.model;

public enum FieldKind {
    PRIMITIVE,
    BOXED_PRIMITIVE,
    BOOLEAN,
    BOXED_BOOLEAN,
    CHAR,
    BOXED_CHAR,
    ENUM,
    STRING,
    BYTES,
    FIXED_STRING,
    FIXED_BYTES,
    NESTED_FIXED,
    UNSUPPORTED
}