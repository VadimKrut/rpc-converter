package ru.pathcreator.pyc.rpc.converter.core.generation;

import org.junit.jupiter.api.Test;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcFixedLength;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcSbe;
import ru.pathcreator.pyc.rpc.converter.core.model.GenerationBundle;
import ru.pathcreator.pyc.rpc.converter.core.testsupport.GeneratedSourceCompiler;
import ru.pathcreator.pyc.rpc.converter.runtime.GeneratedCodec;
import ru.pathcreator.pyc.rpc.converter.runtime.GeneratedCodecFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GenerationEngineIntegrationTest {
    private final GenerationEngine generationEngine = new GenerationEngine();

    @Test
    void generatesExpectedXmlAndCompilableSources() throws Exception {
        GenerationBundle bundle = generationEngine.generate(
                List.of(SbeOrderDto.class, FixedLengthNestedEnvelope.class),
                "ru.pathcreator.pyc.rpc.converter.core.generation"
        );

        assertEquals(3, bundle.javaSources().size());
        assertEquals(3, bundle.resources().size());

        var xml = bundle.resources().stream()
                .filter(resource -> resource.relativePath().endsWith("SbeOrderDto.xml"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                "META-INF/rpc-converter/sbe/"
                + SbeOrderDto.class.getName().replace('.', '/')
                + ".xml",
                xml.relativePath()
        );
        assertTrue(xml.content().contains("<composite name=\"PriceDto\">"));
        assertTrue(xml.content().contains("<field name=\"bestPrice\""));
        assertTrue(xml.content().contains("<data name=\"symbol\""));
        assertTrue(xml.content().indexOf("<field name=\"bestPrice\"") < xml.content().indexOf("<data name=\"symbol\""));

        assertTrue(bundle.resources().stream().anyMatch(resource ->
                resource.relativePath().equals("META-INF/services/ru.pathcreator.pyc.rpc.converter.runtime.GeneratedCodecFactory")));

        Path outputDirectory = GeneratedSourceCompiler.compile(bundle.javaSources());
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            GeneratedCodecFactory factory = loadFactory(classLoader, "ru.pathcreator.pyc.rpc.converter.core.generation.GeneratedCodecFactoryImpl");
            assertNotNull(factory);
            assertEquals(2, factory.codecs().size());
        }
    }

    @Test
    void roundTripsThroughGeneratedSbeCodecs() throws Exception {
        GenerationBundle bundle = generationEngine.generate(
                List.of(SbeOrderDto.class, FixedLengthNestedEnvelope.class),
                "ru.pathcreator.pyc.rpc.converter.core.generation"
        );
        Path outputDirectory = GeneratedSourceCompiler.compile(bundle.javaSources());

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            GeneratedCodecFactory factory = loadFactory(classLoader, "ru.pathcreator.pyc.rpc.converter.core.generation.GeneratedCodecFactoryImpl");

            GeneratedCodec<SbeOrderDto> orderCodec = codecFor(factory, SbeOrderDto.class);
            SbeOrderDto order = new SbeOrderDto();
            order.orderId = 42L;
            order.side = Side.BUY;
            order.bestPrice = new PriceDto();
            order.bestPrice.price = 125_500L;
            order.bestPrice.quantity = 7;
            order.symbol = "BTCUSDT";
            order.opaque = new byte[]{7, 1, 9};

            byte[] encodedOrder = orderCodec.encodeToBytes(order);
            SbeOrderDto decodedOrder = orderCodec.decode(encodedOrder, 0, encodedOrder.length);

            assertEquals(order.orderId, decodedOrder.orderId);
            assertEquals(order.side, decodedOrder.side);
            assertEquals(order.bestPrice.price, decodedOrder.bestPrice.price);
            assertEquals(order.bestPrice.quantity, decodedOrder.bestPrice.quantity);
            assertEquals(order.symbol, decodedOrder.symbol);
            assertArrayEquals(order.opaque, decodedOrder.opaque);

            GeneratedCodec<FixedLengthNestedEnvelope> fixedCodec = codecFor(factory, FixedLengthNestedEnvelope.class);
            FixedLengthNestedEnvelope envelope = new FixedLengthNestedEnvelope();
            envelope.id = 15L;
            envelope.header = new FixedHeader();
            envelope.header.symbol = "ABC";
            envelope.header.signature = new byte[]{1, 2, 3, 4};

            byte[] encodedEnvelope = fixedCodec.encodeToBytes(envelope);
            FixedLengthNestedEnvelope decodedEnvelope = fixedCodec.decode(encodedEnvelope, 0, encodedEnvelope.length);

            assertEquals(envelope.id, decodedEnvelope.id);
            assertEquals(envelope.header.symbol, decodedEnvelope.header.symbol);
            assertArrayEquals(envelope.header.signature, decodedEnvelope.header.signature);
        }
    }

    @Test
    void roundTripsNullablesAndBoxedValuesThroughGeneratedSbeCodec() throws Exception {
        GenerationBundle bundle = generationEngine.generate(
                List.of(NullableSbeDto.class),
                "ru.pathcreator.pyc.rpc.converter.core.generation"
        );
        Path outputDirectory = GeneratedSourceCompiler.compile(bundle.javaSources());

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            GeneratedCodecFactory factory = loadFactory(classLoader, "ru.pathcreator.pyc.rpc.converter.core.generation.GeneratedCodecFactoryImpl");
            GeneratedCodec<NullableSbeDto> codec = codecFor(factory, NullableSbeDto.class);

            NullableSbeDto dto = new NullableSbeDto();
            dto.enabled = Boolean.TRUE;
            dto.grade = 'A';
            dto.optionalCount = 15;
            dto.optionalValue = null;
            dto.text = null;
            dto.bytes = null;

            byte[] encoded = codec.encodeToBytes(dto);
            NullableSbeDto decoded = codec.decode(encoded, 0, encoded.length);

            assertEquals(dto.enabled, decoded.enabled);
            assertEquals(dto.grade, decoded.grade);
            assertEquals(dto.optionalCount, decoded.optionalCount);
            assertEquals(dto.optionalValue, decoded.optionalValue);
            assertEquals(dto.text, decoded.text);
            assertEquals(dto.bytes, decoded.bytes);
        }
    }

    @Test
    void roundTripsNullNestedObjectsThroughGeneratedSbeCodec() throws Exception {
        GenerationBundle bundle = generationEngine.generate(
                List.of(NullableNestedSbeDto.class),
                "ru.pathcreator.pyc.rpc.converter.core.generation"
        );
        Path outputDirectory = GeneratedSourceCompiler.compile(bundle.javaSources());

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            GeneratedCodecFactory factory = loadFactory(classLoader, "ru.pathcreator.pyc.rpc.converter.core.generation.GeneratedCodecFactoryImpl");
            GeneratedCodec<NullableNestedSbeDto> codec = codecFor(factory, NullableNestedSbeDto.class);

            NullableNestedSbeDto dto = new NullableNestedSbeDto();
            dto.id = 99L;
            dto.nested = null;
            dto.name = "nullable-nested";

            byte[] encoded = codec.encodeToBytes(dto);
            NullableNestedSbeDto decoded = codec.decode(encoded, 0, encoded.length);

            assertEquals(dto.id, decoded.id);
            assertEquals(dto.name, decoded.name);
            assertNull(decoded.nested);
        }
    }

    @Test
    void rejectsCharsThatDoNotFitSingleByteSbeEncoding() throws Exception {
        GenerationBundle bundle = generationEngine.generate(
                List.of(InvalidCharDto.class),
                "ru.pathcreator.pyc.rpc.converter.core.generation"
        );
        Path outputDirectory = GeneratedSourceCompiler.compile(bundle.javaSources());

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            GeneratedCodecFactory factory = loadFactory(classLoader, "ru.pathcreator.pyc.rpc.converter.core.generation.GeneratedCodecFactoryImpl");
            GeneratedCodec<InvalidCharDto> codec = codecFor(factory, InvalidCharDto.class);

            InvalidCharDto dto = new InvalidCharDto();
            dto.symbol = '\u20AC';

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> codec.encodeToBytes(dto));
            assertTrue(error.getMessage().contains("must fit in one byte"));
        }
    }

    @Test
    void rejectsFixedLengthStringsThatDoNotFitEncoding() throws Exception {
        GenerationBundle bundle = generationEngine.generate(
                List.of(FixedLengthRootDto.class),
                "ru.pathcreator.pyc.rpc.converter.core.generation"
        );
        Path outputDirectory = GeneratedSourceCompiler.compile(bundle.javaSources());

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            GeneratedCodecFactory factory = loadFactory(classLoader, "ru.pathcreator.pyc.rpc.converter.core.generation.GeneratedCodecFactoryImpl");
            GeneratedCodec<FixedLengthRootDto> codec = codecFor(factory, FixedLengthRootDto.class);

            FixedLengthRootDto dto = new FixedLengthRootDto();
            dto.symbol = "TOO-LONG";
            dto.payload = new byte[]{1, 2};

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> codec.encodeToBytes(dto));
            assertTrue(error.getMessage().contains("exceeds fixed SBE length"));
        }
    }

    @Test
    void failsGenerationWhenDtoUsesUnsupportedCollectionShape() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> generationEngine.generate(List.of(UnsupportedCollectionDto.class), "ru.pathcreator.pyc.rpc.converter.generated.fail")
        );

        assertTrue(error.getMessage().contains("SBE unsupported field"));
    }

    @SuppressWarnings("unchecked")
    private <T> GeneratedCodec<T> codecFor(GeneratedCodecFactory factory, Class<T> type) {
        return (GeneratedCodec<T>) factory.codecs().stream()
                .filter(codec -> codec.javaType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private GeneratedCodecFactory loadFactory(ClassLoader classLoader, String factoryClassName) throws Exception {
        Class<?> factoryType = classLoader.loadClass(factoryClassName);
        return (GeneratedCodecFactory) factoryType.getDeclaredConstructor().newInstance();
    }

    public enum Side {
        BUY,
        SELL
    }

    public static final class PriceDto {
        public long price;
        public int quantity;
    }

    @RpcSbe
    public static final class SbeOrderDto {
        public long orderId;
        public Side side;
        public PriceDto bestPrice;
        public String symbol;
        public byte[] opaque;
    }

    public static final class FixedHeader {
        @RpcFixedLength(6)
        public String symbol;
        @RpcFixedLength(8)
        public byte[] signature;
    }

    @RpcSbe
    public static final class FixedLengthNestedEnvelope {
        public long id;
        public FixedHeader header;
    }

    @RpcSbe
    public static final class UnsupportedCollectionDto {
        public java.util.List<String> items;
    }

    @RpcSbe
    public static final class NullableSbeDto {
        public Boolean enabled;
        public Character grade;
        public Integer optionalCount;
        public Long optionalValue;
        public String text;
        public byte[] bytes;
    }

    @RpcSbe
    public static final class InvalidCharDto {
        public char symbol;
    }

    @RpcSbe
    public static final class NullableNestedSbeDto {
        public long id;
        public PriceDto nested;
        public String name;
    }

    @RpcSbe
    public static final class FixedLengthRootDto {
        @RpcFixedLength(4)
        public String symbol;
        @RpcFixedLength(4)
        public byte[] payload;
    }
}