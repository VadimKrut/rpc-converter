package ru.pathcreator.pyc.rpc.converter.core.model;

/**
 * Supported DTO reconstruction strategies used by generated decoders.
 */
public enum InstantiationStyle {
    RECORD,
    NO_ARGS_CONSTRUCTOR,
    UNSUPPORTED_FOR_SBE
}