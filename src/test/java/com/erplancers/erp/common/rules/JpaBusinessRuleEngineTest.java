package com.erplancers.erp.common.rules;

import com.erplancers.erp.common.web.CrudRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JpaBusinessRuleEngineTest {

    private EntityManager entityManager;
    private CrudRegistry registry;
    private JpaBusinessRuleEngine engine;
    private RootEntity root;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        registry = mock(CrudRegistry.class);
        engine = new JpaBusinessRuleEngine(entityManager, registry);
        root = new RootEntity();
        root.lines = List.of(
                new LineEntity(new ProductEntity(new BigDecimal("10"))),
                new LineEntity(new ProductEntity(new BigDecimal("15"))));
        when(registry.get("order")).thenReturn(
                new CrudRegistry.CrudDescriptor("order", RootEntity.class, null, "sales"));
    }

    @Test
    void calculatesFromNestedJpaRelationCollection() {
        JpaBusinessRuleEngine.RuleSet rules = new JpaBusinessRuleEngine.RuleSet(
                "profit",
                "order",
                "BEFORE_SAVE",
                List.of(new JpaBusinessRuleEngine.Rule(
                        "total-cost",
                        10,
                        null,
                        List.of(new JpaBusinessRuleEngine.Action(
                                "CALCULATE", "totalCost", "SUM(lines.product.cost)", "")))));

        stubMetamodelForRelations();

        var result = engine.evaluateEntity("order", root, rules, false);

        assertTrue(result.valid());
        assertEquals(new BigDecimal("25"), result.values().get("totalCost"));
        assertEquals(new BigDecimal("25"), root.totalCost);
    }

    @Test
    void conditionPreventsRuleAndArithmeticUsesRelationValue() {
        root.approved = false;
        JpaBusinessRuleEngine.RuleSet rules = new JpaBusinessRuleEngine.RuleSet(
                "approval",
                "order",
                "BEFORE_SAVE",
                List.of(new JpaBusinessRuleEngine.Rule(
                        "approval-rule",
                        1,
                        new JpaBusinessRuleEngine.Condition("approved", "EQUALS", true),
                        List.of(new JpaBusinessRuleEngine.Action(
                                "CALCULATE", "totalCost", "total + 5", "")))));

        stubMetamodelForRelations();

        var result = engine.evaluateEntity("order", root, rules, false);

        assertTrue(result.valid());
        assertTrue(result.values().isEmpty());
        assertEquals(BigDecimal.ZERO, root.totalCost);
    }

    @Test
    void rejectsUnknownRelationPathBeforeExecution() {
        stubMetamodelForRelations();
        var rules = new JpaBusinessRuleEngine.RuleSet(
                "bad",
                "order",
                "BEFORE_SAVE",
                List.of(new JpaBusinessRuleEngine.Rule(
                        "bad-rule", 1,
                        new JpaBusinessRuleEngine.Condition("lines.product.missing", "EXISTS", null),
                        List.of())));

        assertThrows(IllegalArgumentException.class,
                () -> engine.evaluateEntity("order", root, rules, false));
    }

    @Test
    void executeMergesChangedEntity() {
        stubMetamodelForRelations();
        when(entityManager.merge(root)).thenReturn(root);

        var rules = new JpaBusinessRuleEngine.RuleSet(
                "set-total",
                "order",
                "AFTER_SAVE",
                List.of(new JpaBusinessRuleEngine.Rule(
                        "total", 1, null,
                        List.of(new JpaBusinessRuleEngine.Action(
                                "SET", "total", "total + 5", "")))));

        var result = engine.evaluateEntity("order", root, rules, true);

        assertTrue(result.valid());
        assertEquals(new BigDecimal("5"), root.total);
        verify(entityManager).merge(root);
    }

    private void stubMetamodelForRelations() {
        Metamodel metamodel = mock(Metamodel.class);
        ManagedType<RootEntity> rootType = mock(ManagedType.class);
        ManagedType<LineEntity> lineType = mock(ManagedType.class);
        ManagedType<ProductEntity> productType = mock(ManagedType.class);

        Attribute<RootEntity, ?> approved = mock(Attribute.class);
        when(approved.getName()).thenReturn("approved");
        when(approved.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.BASIC);
        when(approved.getJavaType()).thenReturn((Class) boolean.class);

        Attribute<RootEntity, ?> total = mock(Attribute.class);
        when(total.getName()).thenReturn("total");
        when(total.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.BASIC);
        when(total.getJavaType()).thenReturn((Class) BigDecimal.class);

        Attribute<RootEntity, ?> totalCost = mock(Attribute.class);
        when(totalCost.getName()).thenReturn("totalCost");
        when(totalCost.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.BASIC);
        when(totalCost.getJavaType()).thenReturn((Class) BigDecimal.class);

        when(rootType.getAttribute("approved")).thenReturn((Attribute) approved);
        when(rootType.getAttribute("total")).thenReturn((Attribute) total);
        when(rootType.getAttribute("totalCost")).thenReturn((Attribute) totalCost);

        Attribute<RootEntity, ?> lines = mock(Attribute.class);
        when(lines.getName()).thenReturn("lines");
        when(lines.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.ONE_TO_MANY);
        PluralAttribute<RootEntity, List<LineEntity>, LineEntity> plural =
                mock(PluralAttribute.class);
        when(plural.getName()).thenReturn("lines");
        when(plural.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.ONE_TO_MANY);
        when(plural.getJavaType()).thenReturn((Class) List.class);
        when(plural.getElementType()).thenReturn((Type) mock(Type.class));
        when(((Type) plural.getElementType()).getJavaType()).thenReturn(LineEntity.class);

        Attribute<LineEntity, ?> product = mock(Attribute.class);
        when(product.getName()).thenReturn("product");
        when(product.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.MANY_TO_ONE);
        when(product.getJavaType()).thenReturn((Class) ProductEntity.class);

        Attribute<ProductEntity, ?> cost = mock(Attribute.class);
        when(cost.getName()).thenReturn("cost");
        when(cost.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.BASIC);
        when(cost.getJavaType()).thenReturn((Class) BigDecimal.class);

        when(rootType.getAttribute("lines")).thenReturn((Attribute) plural);
        when(lineType.getAttribute("product")).thenReturn((Attribute) product);
        when(productType.getAttribute("cost")).thenReturn((Attribute) cost);
        when(metamodel.managedType(RootEntity.class)).thenReturn(rootType);
        when(metamodel.managedType(LineEntity.class)).thenReturn(lineType);
        when(metamodel.managedType(ProductEntity.class)).thenReturn(productType);
        when(metamodel.getManagedTypes()).thenReturn(
                java.util.Set.of(rootType, lineType, productType));
        when(entityManager.getMetamodel()).thenReturn(metamodel);
    }

    static class RootEntity {
        List<LineEntity> lines;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        boolean approved;
    }
    static class LineEntity {
        ProductEntity product;
        LineEntity(ProductEntity product) { this.product = product; }
    }
    static class ProductEntity {
        BigDecimal cost;
        ProductEntity(BigDecimal cost) { this.cost = cost; }
    }
}
