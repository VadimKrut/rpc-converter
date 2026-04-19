package ru.pathcreator.pyc.rpc.converter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides declaration-order sorting for generated schema and codec field order.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RpcFieldOrder {
    /**
     * Defines the field order value.
     *
     * @return lower values are emitted earlier
     */
    int value();
}