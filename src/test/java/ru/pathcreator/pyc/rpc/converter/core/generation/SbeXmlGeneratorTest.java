package ru.pathcreator.pyc.rpc.converter.core.generation;

import org.junit.jupiter.api.Test;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcFixedLength;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcSbe;
import ru.pathcreator.pyc.rpc.converter.core.analysis.DtoIntrospector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SbeXmlGeneratorTest {
    private final DtoIntrospector introspector = new DtoIntrospector();
    private final SbeXmlGenerator generator = new SbeXmlGenerator();

    @Test
    void emitsHeaderTypesNestedCompositeAndVarDataInStableOrder() {
        var spec = introspector.inspectRoot(XmlOrderDto.class);
        var xml = generator.generate(spec);

        assertTrue(xml.contains("<composite name=\"messageHeader\">"));
        assertTrue(xml.contains("<composite name=\"varStringEncoding\">"));
        assertTrue(xml.contains("<composite name=\"varDataEncoding\">"));
        assertTrue(xml.contains("<composite name=\"InnerFixedDto\">"));
        assertTrue(xml.contains("<composite name=\"NullableInnerFixedDto\">"));
        assertTrue(xml.contains("<field name=\"id\""));
        assertTrue(xml.contains("<field name=\"active\""));
        assertTrue(xml.contains("<field name=\"inner\""));
        assertTrue(xml.contains("<data name=\"name\""));
        assertTrue(xml.contains("<data name=\"payload\""));
        assertTrue(xml.indexOf("<field name=\"inner\"") < xml.indexOf("<data name=\"name\""));
        assertTrue(xml.indexOf("<data name=\"name\"") < xml.indexOf("<data name=\"payload\""));
    }

    @Test
    void usesSchemaNameAndPackageFromDtoAnnotation() {
        var spec = introspector.inspectRoot(CustomSchemaDto.class);
        var xml = generator.generate(spec);

        assertTrue(xml.contains("package=\"" + CustomSchemaDto.class.getPackageName() + "\""));
        assertTrue(xml.contains("<message name=\"CustomSchema\" id=\"1\">"));
    }

    @Test
    void mapsPrimitiveKindsToExpectedXmlTypes() {
        var spec = introspector.inspectRoot(TypeMappingDto.class);
        var xml = generator.generate(spec);

        assertTrue(xml.contains("name=\"flag\" id=\"1\" type=\"uint8\""));
        assertTrue(xml.contains("name=\"letter\" id=\"2\" type=\"char\""));
        assertTrue(xml.contains("name=\"side\" id=\"3\" type=\"NullableEnumInt32\""));
        assertTrue(xml.contains("name=\"count\" id=\"4\" type=\"int32\""));
        assertTrue(xml.contains("name=\"total\" id=\"5\" type=\"int64\""));
    }

    @Test
    void emitsNullableWrappersForNestedAndBoxedTypes() {
        var spec = introspector.inspectRoot(NullableXmlDto.class);
        var xml = generator.generate(spec);

        assertTrue(xml.contains("<composite name=\"NullableInnerFixedDto\">"));
        assertTrue(xml.contains("<ref name=\"value\" type=\"InnerFixedDto\"/>"));
        assertTrue(xml.contains("<composite name=\"NullableInt32\">"));
        assertTrue(xml.contains("<composite name=\"NullableBoolean\">"));
        assertTrue(xml.contains("<field name=\"inner\" id=\"1\" type=\"NullableInnerFixedDto\"/>"));
        assertTrue(xml.contains("<field name=\"count\" id=\"2\" type=\"NullableInt32\"/>"));
        assertTrue(xml.contains("<field name=\"enabled\" id=\"3\" type=\"NullableBoolean\"/>"));
    }

    @Test
    void emitsFixedLengthWrappersForStringAndBytes() {
        var spec = introspector.inspectRoot(FixedLengthXmlDto.class);
        var xml = generator.generate(spec);

        assertTrue(xml.contains("<composite name=\"NullableFixedString12\">"));
        assertTrue(xml.contains("primitiveType=\"char\" length=\"12\" characterEncoding=\"ISO-8859-1\""));
        assertTrue(xml.contains("<composite name=\"NullableFixedBytes8\">"));
        assertTrue(xml.contains("primitiveType=\"uint8\" length=\"8\""));
    }

    @Test
    void emitsNestedCompositeOnlyOnceForRepeatedTypeUsage() {
        var spec = introspector.inspectRoot(RepeatedNestedDto.class);
        var xml = generator.generate(spec);

        assertEquals(1, occurrences(xml, "<composite name=\"InnerFixedDto\">"));
    }

    private int occurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    public static final class InnerFixedDto {
        public long price;
        public int quantity;
    }

    @RpcSbe
    public static final class XmlOrderDto {
        public long id;
        public boolean active;
        public InnerFixedDto inner;
        public String name;
        public byte[] payload;
    }

    @RpcSbe(schemaName = "CustomSchema")
    public static final class CustomSchemaDto {
        public long id;
    }

    public enum Side {
        BUY,
        SELL
    }

    @RpcSbe
    public static final class TypeMappingDto {
        public boolean flag;
        public char letter;
        public Side side;
        public int count;
        public long total;
    }

    @RpcSbe
    public static final class RepeatedNestedDto {
        public InnerFixedDto first;
        public InnerFixedDto second;
    }

    @RpcSbe
    public static final class NullableXmlDto {
        public InnerFixedDto inner;
        public Integer count;
        public Boolean enabled;
    }

    @RpcSbe
    public static final class FixedLengthXmlDto {
        @RpcFixedLength(12)
        public String symbol;
        @RpcFixedLength(8)
        public byte[] payload;
    }
}