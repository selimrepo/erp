package com.erplancers.erp.common.reporting;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.common.web.CrudRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class EntityDataAnalysisService {

    private static final int MAX_TOP_VALUES = 25;

    private final EntityManager entityManager;
    private final CrudRegistry registry;

    public EntityDataAnalysisService(EntityManager entityManager, CrudRegistry registry) {
        this.entityManager = entityManager;
        this.registry = registry;
    }

    public EntityCatalog catalog() {
        return catalog(2);
    }

    /**
     * Builds the JPA field catalog with a caller-selected relationship depth.
     * Depth 0 exposes only root scalar fields; depth 1 adds one relationship hop.
     */
    public EntityCatalog catalog(int relationDepth) {
        int depth = normalizeRelationDepth(relationDepth);
        List<ResourceSchema> resources = registry.all().values().stream()
                .sorted(Comparator.comparing(CrudRegistry.CrudDescriptor::slug))
                .map(descriptor -> schema(descriptor, depth))
                .toList();

        return new EntityCatalog(resources.size(), resources);
    }

    public EntityProfile profile(String slug, int topValues) {
        CrudRegistry.CrudDescriptor descriptor = registry.get(slug);
        int topLimit = Math.max(1, Math.min(topValues, MAX_TOP_VALUES));

        EntityType<?> entity = entityManager.getMetamodel().entity(descriptor.entityType());
        long rowCount = count(descriptor.entityType());

        List<FieldProfile> fields = entity.getAttributes().stream()
                .filter(this::isBasicScalar)
                .sorted(Comparator.comparing(Attribute::getName))
                .map(attribute -> profileField(descriptor.entityType(), attribute, rowCount, topLimit))
                .toList();

        return new EntityProfile(
                descriptor.slug(),
                descriptor.module(),
                descriptor.entityType().getSimpleName(),
                rowCount,
                fields
        );
    }

    private ResourceSchema schema(CrudRegistry.CrudDescriptor descriptor, int relationDepth) {
        EntityType<?> entity = entityManager.getMetamodel().entity(descriptor.entityType());

        List<FieldSchema> fields = new ArrayList<>();
        collectBiFields(entity, "", 0, relationDepth, new HashSet<>(), fields);
        fields.sort(Comparator.comparing(FieldSchema::name));

        return new ResourceSchema(
                descriptor.slug(),
                descriptor.module(),
                descriptor.entityType().getSimpleName(),
                fields
        );
    }

    private static final int MAX_RELATIONSHIP_DEPTH = 6;

    private void collectBiFields(
            ManagedType<?> type,
            String prefix,
            int depth,
            int relationDepth,
            Set<Class<?>> visited,
            List<FieldSchema> fields) {

        if (depth > relationDepth || !visited.add(type.getJavaType())) return;

        type.getAttributes().stream()
                .sorted(Comparator.comparing(Attribute::getName))
                .forEach(attribute -> {
                    String name = prefix.isBlank()
                            ? attribute.getName()
                            : prefix + "." + attribute.getName();

                    if (isBasicScalar(attribute)) {
                        fields.add(new FieldSchema(
                                name,
                                attribute.getJavaType().getName(),
                                attribute.getPersistentAttributeType().name()));
                        return;
                    }

                    if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE
                            || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_ONE) {
                        ManagedType<?> target = entityManager.getMetamodel()
                                .managedType(attribute.getJavaType());
                        // The relationship itself is a valid OLAP dimension. The
                        // aggregation engine resolves terminal relationships to
                        // the target entity identifier for GROUP BY operations.
                        fields.add(new FieldSchema(
                                name,
                                attribute.getJavaType().getName(),
                                attribute.getPersistentAttributeType().name()));
                        collectBiFields(target, name, depth + 1, relationDepth, new HashSet<>(visited), fields);
                    } else if (attribute instanceof PluralAttribute<?, ?, ?> plural
                            && (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_MANY
                            || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_MANY)) {
                        Class<?> elementType = plural.getElementType().getJavaType();
                        if (EntityBase.class.isAssignableFrom(elementType)) {
                            ManagedType<?> target = entityManager.getMetamodel().managedType(elementType);
                            // Collection relationships are also valid dimensions.
                            // The Java type recorded here is the related entity
                            // type rather than java.util.Collection.
                            fields.add(new FieldSchema(
                                    name,
                                    elementType.getName(),
                                    attribute.getPersistentAttributeType().name()));
                            collectBiFields(target, name, depth + 1, relationDepth, new HashSet<>(visited), fields);
                        }
                    }
                });
    }

    private int normalizeRelationDepth(int relationDepth) {
        return Math.max(0, Math.min(relationDepth, MAX_RELATIONSHIP_DEPTH));
    }

    private boolean isBasicScalar(Attribute<?, ?> attribute) {
        return attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC
                && !"id".equals(attribute.getName())
                && !"version".equals(attribute.getName());
    }

    private long count(Class<?> type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<?> root = query.from(type);
        query.select(cb.count(root));
        return entityManager.createQuery(query).getSingleResult();
    }

    private FieldProfile profileField(
            Class<?> entityType,
            Attribute<?, ?> attribute,
            long rowCount,
            int topLimit) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<?> root = query.from(entityType);
        Expression<?> field = root.get(attribute.getName());

        List<Expression<?>> selections = new ArrayList<>();
        selections.add(cb.count(field));
        selections.add(cb.countDistinct(field));

        boolean numeric = isNumeric(attribute.getJavaType());

        boolean comparable = supportsOrdering(attribute.getJavaType());
        if (numeric) {
            selections.add(cb.min(asNumber(field)));
            selections.add(cb.max(asNumber(field)));
            selections.add(cb.avg(asNumber(field)));
            selections.add(cb.sum(asNumber(field)));
        } else if (comparable) {
            selections.add(cb.least(asComparable(field)));
            selections.add(cb.greatest(asComparable(field)));
        }

        query.multiselect(selections.toArray(Expression[]::new));
        Object[] row = entityManager.createQuery(query).getSingleResult();

        long nonNullCount = ((Number) row[0]).longValue();
        long distinctCount = ((Number) row[1]).longValue();
        long nullCount = Math.max(0, rowCount - nonNullCount);

        int index = 2;
        Object min = null;
        Object max = null;
        Double average = null;
        Object sum = null;

        if (numeric) {
            min = row[index++];
            max = row[index++];
            Number avg = (Number) row[index++];
            sum = row[index++];
            average = avg == null ? null : avg.doubleValue();
        } else if (comparable) {
            min = row[index++];
            max = row[index];
        }

        List<ValueCount> top = isCategorical(attribute.getJavaType())
                ? topValues(entityType, attribute.getName(), topLimit)
                : List.of();

        return new FieldProfile(
                attribute.getName(),
                attribute.getJavaType().getName(),
                rowCount,
                nonNullCount,
                nullCount,
                distinctCount,
                nullCount * 100.0 / Math.max(1, rowCount),
                min,
                max,
                average,
                sum,
                top
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Expression<? extends Number> asNumber(Expression<?> expression) {
        return (Expression) expression;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Expression<? extends Comparable> asComparable(Expression<?> expression) {
        return (Expression) expression;
    }

    private boolean isNumeric(Class<?> type) {
        return Number.class.isAssignableFrom(type)
                || type == byte.class
                || type == short.class
                || type == int.class
                || type == long.class
                || type == float.class
                || type == double.class;
    }

    private boolean supportsOrdering(Class<?> type) {
        return String.class.isAssignableFrom(type)
                || Temporal.class.isAssignableFrom(type)
                || java.util.Date.class.isAssignableFrom(type);
    }

    private boolean isCategorical(Class<?> type) {
        return type == String.class
                || type == Boolean.class
                || type == boolean.class
                || type.isEnum();
    }

    private List<ValueCount> topValues(Class<?> entityType, String fieldName, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<?> root = query.from(entityType);
        Expression<?> field = root.get(fieldName);
        Expression<Long> count = cb.count(field);

        query.multiselect(field, count)
                .where(cb.isNotNull(field))
                .groupBy(field)
                .orderBy(cb.desc(count));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(row -> new ValueCount(row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    public record EntityCatalog(int resourceCount, List<ResourceSchema> resources) {}

    public record ResourceSchema(
            String slug,
            String module,
            String entity,
            List<FieldSchema> fields) {}

    public record FieldSchema(
            String name,
            String javaType,
            String persistentType) {}

    public record EntityProfile(
            String resource,
            String module,
            String entity,
            long rowCount,
            List<FieldProfile> fields) {}

    public record FieldProfile(
            String name,
            String javaType,
            long rowCount,
            long nonNullCount,
            long nullCount,
            long distinctCount,
            double nullPercent,
            Object min,
            Object max,
            Double average,
            Object sum,
            List<ValueCount> topValues) {}

    public record ValueCount(Object value, long count) {}
}
