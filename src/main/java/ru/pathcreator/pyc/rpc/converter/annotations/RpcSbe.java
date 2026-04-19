package ru.pathcreator.pyc.rpc.converter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a DTO class as an SBE root message candidate for {@code rpc-converter}.
 *
 * <p>The Maven plugin scans compiled classes for this annotation and attempts to generate an SBE schema, a Java codec,
 * and registry metadata for every discovered type.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcSbe {
    /**
     * Overrides the default schema/message name.
     *
     * @return explicit schema name, or blank to use the simple Java class name
     */
    String schemaName() default "";
}