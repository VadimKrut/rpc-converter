package ru.pathcreator.pyc.rpc.converter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a fixed-length SBE representation for a {@link String} or {@code byte[]} field.
 *
 * <p>This annotation is primarily used inside nested DTOs where SBE-compatible fixed-size composites are required.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RpcFixedLength {
    /**
     * Returns the fixed element count used by the generated schema.
     *
     * @return positive fixed size for the annotated field
     */
    int value();
}