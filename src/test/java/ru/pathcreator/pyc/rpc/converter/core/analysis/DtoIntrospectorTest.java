package ru.pathcreator.pyc.rpc.converter.core.analysis;

import org.junit.jupiter.api.Test;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcFieldOrder;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcFixedLength;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcSbe;
import ru.pathcreator.pyc.rpc.converter.core.model.FieldKind;
import ru.pathcreator.pyc.rpc.converter.core.model.InstantiationStyle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DtoIntrospectorTest {
    private final DtoIntrospector introspector = new DtoIntrospector();

    @Test
    void sortsFieldsByExplicitOrderThenDeclaration() {
        var spec = introspector.inspectRoot(OrderedDto.class);

        assertEquals(List.of("first", "second", "third"), spec.fields().stream().map(field -> field.name()).toList());
    }

    @Test
    void capturesNestedFixedDtoAndFixedLengthHints() {
        var spec = introspector.inspectRoot(FixedDto.class);

        var symbol = spec.fields().stream().filter(field -> field.name().equals("symbol")).findFirst().orElseThrow();
        var raw = spec.fields().stream().filter(field -> field.name().equals("raw")).findFirst().orElseThrow();
        var payload = spec.fields().stream().filter(field -> field.name().equals("payload")).findFirst().orElseThrow();

        assertEquals(FieldKind.FIXED_STRING, symbol.kind());
        assertEquals(12, symbol.fixedLength());
        assertEquals(FieldKind.FIXED_BYTES, raw.kind());
        assertEquals(16, raw.fixedLength());
        assertEquals(FieldKind.NESTED_FIXED, payload.kind());
        assertEquals(FieldKind.PRIMITIVE, payload.nestedType().fields().get(0).kind());
    }

    @Test
    void marksUnsupportedShapesForSbe() {
        var spec = introspector.inspectRoot(UnsupportedShapesDto.class);

        assertEquals(
                List.of(
                        FieldKind.UNSUPPORTED,
                        FieldKind.UNSUPPORTED,
                        FieldKind.UNSUPPORTED,
                        FieldKind.UNSUPPORTED,
                        FieldKind.UNSUPPORTED
                ),
                spec.fields().stream().map(field -> field.kind()).toList()
        );
    }

    @Test
    void detectsRecordInstantiationStyle() {
        var spec = introspector.inspectRoot(RecordDto.class);

        assertEquals(InstantiationStyle.RECORD, spec.instantiationStyle());
    }

    @Test
    void marksMissingNoArgsConstructorAsUnsupportedForSbe() {
        var spec = introspector.inspectRoot(ConstructorOnlyDto.class);

        assertEquals(InstantiationStyle.UNSUPPORTED_FOR_SBE, spec.instantiationStyle());
    }

    @Test
    void rejectsRecursiveDtoGraphsDuringInspection() {
        assertThrows(IllegalStateException.class, () -> introspector.inspectRoot(RecursiveDto.class));
    }

    public static final class OrderedDto {
        @RpcFieldOrder(2)
        public long third;

        @RpcFieldOrder(0)
        public long first;

        @RpcFieldOrder(1)
        public long second;
    }

    public static final class FixedPayload {
        public int code;
    }

    @RpcSbe
    public static final class FixedDto {
        @RpcFixedLength(12)
        public String symbol;
        @RpcFixedLength(16)
        public byte[] raw;
        public FixedPayload payload;
    }

    public static final class UnsupportedShapesDto {
        public List<String> list;
        public Map<String, String> map;
        public Optional<String> optional;
        public BigDecimal amount;
        public Instant timestamp;
    }

    public record RecordDto(long id, String name) {
    }

    public static final class ConstructorOnlyDto {
        public long id;

        public ConstructorOnlyDto(long id) {
            this.id = id;
        }
    }

    public static final class RecursiveDto {
        public RecursiveDto next;
    }
}