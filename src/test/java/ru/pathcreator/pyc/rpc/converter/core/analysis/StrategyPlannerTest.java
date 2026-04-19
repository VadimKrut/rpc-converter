package ru.pathcreator.pyc.rpc.converter.core.analysis;

import org.junit.jupiter.api.Test;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcFixedLength;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcSbe;
import ru.pathcreator.pyc.rpc.converter.core.model.AnalysisStrategy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StrategyPlannerTest {
    private final StrategyPlanner planner = new StrategyPlanner();

    @Test
    void plansPureFixedAndRootVarDataDtoAsSbe() {
        var result = planner.plan(QuoteSnapshotDto.class);

        assertEquals(AnalysisStrategy.SBE, result.strategy());
        assertNotNull(result.rootSpec());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void failsWhenNestedDtoContainsVarData() {
        var result = planner.plan(EnvelopeWithNestedString.class);

        assertEquals(AnalysisStrategy.FAIL, result.strategy());
        assertTrue(result.problems().stream().anyMatch(problem -> problem.contains("Nested DTO used by SBE must be fixed-only")));
    }

    @Test
    void failsForRecursiveGraphs() {
        var result = planner.plan(RecursiveNode.class);

        assertEquals(AnalysisStrategy.FAIL, result.strategy());
        assertTrue(result.problems().stream().anyMatch(problem -> problem.contains("Recursive DTO graph")));
    }

    @Test
    void failsForDynamicObjectField() {
        var result = planner.plan(DynamicEnvelope.class);

        assertEquals(AnalysisStrategy.FAIL, result.strategy());
        assertTrue(result.problems().stream().anyMatch(problem -> problem.contains("SBE unsupported field")));
    }

    @Test
    void supportsFixedLengthNestedStringAsSbe() {
        var result = planner.plan(FixedLengthEnvelope.class);

        assertEquals(AnalysisStrategy.SBE, result.strategy());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void failsForInvalidFixedLengthHint() {
        var result = planner.plan(InvalidFixedLengthDto.class);

        assertEquals(AnalysisStrategy.FAIL, result.strategy());
        assertTrue(result.problems().stream().anyMatch(problem -> problem.contains("fixed-length hint must be positive")));
    }

    public enum Side {
        BUY,
        SELL
    }

    public static final class PriceLevel {
        public long price;
        public int quantity;
    }

    @RpcSbe
    public static final class QuoteSnapshotDto {
        public long instrumentId;
        public int sequence;
        public Side side;
        public PriceLevel bestBid;
        public PriceLevel bestAsk;
        public String symbol;
        public byte[] rawExtension;
    }

    public static final class NestedVarData {
        public String innerPayload;
    }

    @RpcSbe
    public static final class EnvelopeWithNestedString {
        public long correlationId;
        public NestedVarData payload;
    }

    @RpcSbe
    public static final class RecursiveNode {
        public long id;
        public RecursiveNode next;
    }

    @RpcSbe
    public static final class DynamicEnvelope {
        public long correlationId;
        public Object payload;
    }

    public static final class FixedChild {
        @RpcFixedLength(8)
        public String symbol;
    }

    @RpcSbe
    public static final class FixedLengthEnvelope {
        public long correlationId;
        public FixedChild payload;
    }

    @RpcSbe
    public static final class InvalidFixedLengthDto {
        @RpcFixedLength(0)
        public String symbol;
        public List<String> items;
    }
}