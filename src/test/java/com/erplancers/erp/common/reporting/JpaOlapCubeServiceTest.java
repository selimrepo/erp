package com.erplancers.erp.common.reporting;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JpaOlapCubeServiceTest {

    private EntityDataAnalysisService entityAnalysis;
    private OlapAnalysisEngine olap;
    private EntityManager entityManager;
    private JpaOlapCubeService service;

    static class TestEntity {
    }

    @BeforeEach
    void setUp() {
        entityAnalysis = mock(EntityDataAnalysisService.class);
        olap = mock(OlapAnalysisEngine.class);
        entityManager = mock(EntityManager.class);
        service = new JpaOlapCubeService(entityAnalysis, olap, entityManager);

        when(entityAnalysis.catalog(anyInt())).thenReturn(catalog());
        when(entityAnalysis.catalog()).thenReturn(catalog());
    }

    @Test
    void buildsCubeWithScalarAndAggregateRelationshipDimensions() {
        JpaOlapCubeService.AutoCube cube = service.cube("sales-order", 2);

        assertEquals("sales-order", cube.name());
        assertEquals(List.of(
                "status",
                "totalAmount",
                "customer",
                "customer.name",
                "lines",
                "lines.productName"
        ), cube.dimensions());

        assertTrue(cube.measures().contains("count"));
        assertTrue(cube.measures().contains("sum:totalAmount"));
        assertTrue(cube.measures().contains("avg:totalAmount"));
        assertTrue(cube.measures().contains("min:totalAmount"));
        assertTrue(cube.measures().contains("max:totalAmount"));
        assertTrue(cube.measures().contains("countDistinct:customer"));
        assertTrue(cube.relationships().contains("customer"));
        assertTrue(cube.relationships().contains("lines"));
    }

    @Test
    void acceptsAggregatePropertyAsDimension() {
        when(olap.cube(eq("sales-order"), any(OlapAnalysisEngine.CubeRequest.class)))
                .thenReturn(emptyResult());

        JpaOlapCubeService.AutoCubeQueryRequest request =
                new JpaOlapCubeService.AutoCubeQueryRequest(
                        List.of("customer"),
                        List.of("count"),
                        Map.of(),
                        "",
                        100,
                        true,
                        false,
                        "",
                        "",
                        2);

        assertDoesNotThrow(() -> service.query("sales-order", request));

        var captor = org.mockito.ArgumentCaptor.forClass(OlapAnalysisEngine.CubeRequest.class);
        verify(olap).cube(eq("sales-order"), captor.capture());
        assertEquals(List.of("customer"), captor.getValue().dimensions());
        assertEquals(List.of("count"), captor.getValue().measures());
    }

    @Test
    void acceptsCollectionAggregatePropertyAsDimension() {
        when(olap.cube(eq("sales-order"), any(OlapAnalysisEngine.CubeRequest.class)))
                .thenReturn(emptyResult());

        var request = new JpaOlapCubeService.AutoCubeQueryRequest(
                List.of("lines"),
                List.of("count"),
                Map.of(),
                "",
                100,
                false,
                false,
                "",
                "",
                2);

        assertDoesNotThrow(() -> service.query("sales-order", request));

        var captor = org.mockito.ArgumentCaptor.forClass(OlapAnalysisEngine.CubeRequest.class);
        verify(olap).cube(eq("sales-order"), captor.capture());
        assertEquals(List.of("lines"), captor.getValue().dimensions());
    }

    @Test
    void acceptsNestedAggregateDimension() {
        when(olap.cube(eq("sales-order"), any(OlapAnalysisEngine.CubeRequest.class)))
                .thenReturn(emptyResult());

        var request = new JpaOlapCubeService.AutoCubeQueryRequest(
                List.of("lines.productName"),
                List.of("count"),
                Map.of(),
                "",
                50,
                true,
                true,
                "lines.productName",
                "",
                2);

        assertDoesNotThrow(() -> service.query("sales-order", request));

        var captor = org.mockito.ArgumentCaptor.forClass(OlapAnalysisEngine.CubeRequest.class);
        verify(olap).cube(eq("sales-order"), captor.capture());
        assertEquals(List.of("lines.productName"), captor.getValue().dimensions());
        assertEquals("lines.productName", captor.getValue().pivotDimension());
    }

    @Test
    void rejectsUnknownDimension() {
        var request = new JpaOlapCubeService.AutoCubeQueryRequest(
                List.of("doesNotExist"),
                List.of("count"),
                Map.of(),
                "",
                100,
                false,
                false,
                "",
                "",
                2);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.query("sales-order", request));

        assertTrue(error.getMessage().contains("Unknown OLAP dimension"));
        verifyNoInteractions(olap);
    }

    @Test
    void rejectsUnknownMeasure() {
        var request = new JpaOlapCubeService.AutoCubeQueryRequest(
                List.of("customer"),
                List.of("sum:missingAmount"),
                Map.of(),
                "",
                100,
                false,
                false,
                "",
                "",
                2);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.query("sales-order", request));

        assertTrue(error.getMessage().contains("Unknown OLAP measure"));
        verifyNoInteractions(olap);
    }

    @Test
    void defaultsEmptyMeasuresToCount() {
        when(olap.cube(eq("sales-order"), any(OlapAnalysisEngine.CubeRequest.class)))
                .thenReturn(emptyResult());

        var request = new JpaOlapCubeService.AutoCubeQueryRequest(
                List.of("customer"),
                List.of(),
                Map.of(),
                "",
                0,
                false,
                false,
                "",
                "",
                2);

        service.query("sales-order", request);

        var captor = org.mockito.ArgumentCaptor.forClass(OlapAnalysisEngine.CubeRequest.class);
        verify(olap).cube(eq("sales-order"), captor.capture());
        assertEquals(List.of("count"), captor.getValue().measures());
        assertEquals(100, captor.getValue().limit());
    }

    @Test
    void normalizesRelationDepth() {
        JpaOlapCubeService.AutoCubeCatalog catalog = service.cubes(99);

        assertEquals(1, catalog.cubeCount());
        verify(entityAnalysis).catalog(6);
    }

    private EntityDataAnalysisService.EntityCatalog catalog() {
        return new EntityDataAnalysisService.EntityCatalog(
                1,
                List.of(new EntityDataAnalysisService.ResourceSchema(
                        "sales-order",
                        "sales",
                        TestEntity.class.getName(),
                        List.of(
                                new EntityDataAnalysisService.FieldSchema(
                                        "status", String.class.getName(), "BASIC"),
                                new EntityDataAnalysisService.FieldSchema(
                                        "totalAmount", BigDecimal.class.getName(), "BASIC"),
                                new EntityDataAnalysisService.FieldSchema(
                                        "customer", TestEntity.class.getName(), "MANY_TO_ONE"),
                                new EntityDataAnalysisService.FieldSchema(
                                        "customer.name", String.class.getName(), "BASIC"),
                                new EntityDataAnalysisService.FieldSchema(
                                        "lines", TestEntity.class.getName(), "ONE_TO_MANY"),
                                new EntityDataAnalysisService.FieldSchema(
                                        "lines.productName", String.class.getName(), "BASIC")
                        ))));
    }

    private OlapAnalysisEngine.CubeResult emptyResult() {
        return new OlapAnalysisEngine.CubeResult(
                "sales-order",
                "sales",
                List.of(),
                List.of("count"),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                0);
    }
}
