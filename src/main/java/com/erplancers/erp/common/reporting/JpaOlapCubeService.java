package com.erplancers.erp.common.reporting;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds logical OLAP cubes automatically from the JPA model.
 *
 * <p>Every registered ERP entity becomes a cube. Scalar JPA attributes become
 * dimensions and numeric scalar attributes become measures. Relationship
 * attributes discovered by {@link EntityDataAnalysisService} are exposed as
 * dotted dimensions/measures, for example {@code customer.name} or
 * {@code product.amount}.</p>
 *
 * <p>The cubes are virtual: no separate star-schema tables are required.
 * Queries are translated to the existing JPA Criteria aggregation engine.</p>
 */
@Service
@Transactional(readOnly = true)
public class JpaOlapCubeService {

    private final EntityDataAnalysisService entityAnalysis;
    private final OlapAnalysisEngine olap;
    private final EntityManager entityManager;

    public JpaOlapCubeService(
            EntityDataAnalysisService entityAnalysis,
            OlapAnalysisEngine olap,
            EntityManager entityManager) {
        this.entityAnalysis = entityAnalysis;
        this.olap = olap;
        this.entityManager = entityManager;
    }

    /**
     * Creates a cube definition for every registered JPA entity.
     */
    public AutoCubeCatalog cubes() {
        return cubes(2);
    }

    /**
     * Creates an automatic cube for every registered entity using the supplied
     * maximum relationship hop count.
     */
    public AutoCubeCatalog cubes(int relationDepth) {
        int depth = normalizeRelationDepth(relationDepth);
        EntityDataAnalysisService.EntityCatalog catalog = entityAnalysis.catalog(depth);

        List<AutoCube> cubes = catalog.resources().stream()
                .map(schema -> createCube(schema, depth))
                .toList();

        return new AutoCubeCatalog(cubes.size(), cubes);
    }

    /**
     * Creates one logical cube from the current JPA metadata.
     */
    /**
     * Discovers JPA relationships for one cube. When relation is blank all
     * direct and nested relationships are returned; otherwise relation may be
     * a dotted path such as {@code customer.address}.
     */
    public RelationDiscovery discoverRelations(String resource, String relation) {
        return discoverRelations(resource, relation, 2);
    }

    /**
     * Discovers JPA relationship paths up to the requested relationship hop
     * count. A depth of 1 means direct relationships, depth 2 includes
     * relationships of those related entities, etc.
     */
    public RelationDiscovery discoverRelations(String resource, String relation, int relationDepth) {
        int depthLimit = normalizeRelationDepth(relationDepth);
        EntityDataAnalysisService.EntityCatalog catalog = entityAnalysis.catalog(depthLimit);
        EntityDataAnalysisService.ResourceSchema schema = catalog.resources().stream()
                .filter(r -> r.slug().equals(resource))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown ERP resource: " + resource));

        Class<?> entityType = schemaEntityType(resource);
        String requested = relation == null ? "" : relation.trim();
        List<RelationPath> paths = new ArrayList<>();
        discoverRelationPaths(entityType, "", 0, depthLimit, new LinkedHashSet<>(), paths);

        List<RelationPath> selected = requested.isBlank()
                ? paths
                : paths.stream()
                    .filter(path -> path.path().equals(requested)
                            || path.path().startsWith(requested + "."))
                    .toList();

        if (!requested.isBlank() && selected.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown JPA relation '" + requested + "' for cube '" + resource + "'");
        }

        return new RelationDiscovery(
                resource,
                requested,
                selected);
    }

    public AutoCube cube(String resource) {
        return cube(resource, 2);
    }

    public AutoCube cube(String resource, int relationDepth) {
        int depth = normalizeRelationDepth(relationDepth);
        EntityDataAnalysisService.EntityCatalog catalog = entityAnalysis.catalog(depth);

        EntityDataAnalysisService.ResourceSchema schema = catalog.resources().stream()
                .filter(r -> r.slug().equals(resource))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown ERP resource: " + resource));

        return createCube(schema, depth);
    }

    /**
     * Queries an automatically generated cube. Requested fields are validated
     * against the JPA-derived cube definition before the query reaches JPA.
     */
    public OlapAnalysisEngine.CubeResult query(
            String cubeName,
            AutoCubeQueryRequest request) {

        int relationDepth = normalizeRelationDepth(request.relationDepth());
        AutoCube cube = cube(cubeName, relationDepth);

        String relation = request.relation();
        List<String> dimensions;
        if (request.dimensions().isEmpty() && !relation.isBlank()) {
            RelationDiscovery discovery = discoverRelations(cube.resource(), relation, relationDepth);
            String prefix = relation + ".";
            dimensions = cube.dimensions().stream()
                    .filter(path -> path.startsWith(prefix))
                    .toList();

            if (dimensions.isEmpty()) {
                throw new IllegalArgumentException(
                        "JPA relation '" + relation + "' has no scalar dimensions at relation depth "
                                + relationDepth + " for cube '" + cube.resource() + "'");
            }
        } else {
            dimensions = request.dimensions().isEmpty()
                    ? List.of()
                    : validateDimensions(cube, request.dimensions());
            if (!relation.isBlank()) {
                RelationDiscovery discovery = discoverRelations(cube.resource(), relation, relationDepth);
                String prefix = relation + ".";
                if (dimensions.stream().noneMatch(d -> d.equals(relation) || d.startsWith(prefix))) {
                    throw new IllegalArgumentException(
                            "Requested dimensions do not use JPA relation '" + relation + "'");
                }
                dimensions = List.copyOf(dimensions);
            }
        }

        List<String> measures = request.measures().isEmpty()
                ? List.of("count")
                : validateMeasures(cube, request.measures());

        return olap.cube(
                cube.resource(),
                new OlapAnalysisEngine.CubeRequest(
                        dimensions,
                        measures,
                        request.filters(),
                        request.orderBy(),
                        request.limit(),
                        request.includeTotals(),
                        request.includeRollup(),
                        request.pivotDimension()));
    }

    private Class<?> schemaEntityType(String resource) {
        return entityAnalysis.catalog().resources().stream()
                .filter(r -> r.slug().equals(resource))
                .findFirst()
                .map(r -> entityManager.getMetamodel().getEntities().stream()
                        .filter(entity -> entity.getName().equals(r.entity()))
                        .map(ManagedType::getJavaType)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "JPA entity not found for resource: " + resource)))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown ERP resource: " + resource));
    }

    private void discoverRelationPaths(
            Class<?> entityType,
            String prefix,
            int depth,
            int relationDepth,
            Set<Class<?>> visited,
            List<RelationPath> result) {
        if (depth >= relationDepth || !visited.add(entityType)) {
            return;
        }

        ManagedType<?> type = entityManager.getMetamodel().managedType(entityType);
        type.getAttributes().stream()
                .sorted(java.util.Comparator.comparing(Attribute::getName))
                .forEach(attribute -> {
                    boolean singular = attribute.getPersistentAttributeType()
                            == Attribute.PersistentAttributeType.MANY_TO_ONE
                            || attribute.getPersistentAttributeType()
                            == Attribute.PersistentAttributeType.ONE_TO_ONE;
                    boolean plural = attribute instanceof PluralAttribute<?, ?, ?>
                            && (attribute.getPersistentAttributeType()
                            == Attribute.PersistentAttributeType.ONE_TO_MANY
                            || attribute.getPersistentAttributeType()
                            == Attribute.PersistentAttributeType.MANY_TO_MANY);
                    if (!singular && !plural) {
                        return;
                    }

                    Class<?> target = singular
                            ? attribute.getJavaType()
                            : ((PluralAttribute<?, ?, ?>) attribute).getElementType().getJavaType();
                    if (!entityManager.getMetamodel().getManagedTypes().stream()
                            .anyMatch(m -> m.getJavaType().equals(target))) {
                        return;
                    }

                    String path = prefix.isBlank()
                            ? attribute.getName()
                            : prefix + "." + attribute.getName();
                    result.add(new RelationPath(
                            path,
                            target.getName(),
                            attribute.getPersistentAttributeType().name()));
                    discoverRelationPaths(target, path, depth + 1,
                            relationDepth, new LinkedHashSet<>(visited), result);
                });
    }

    private AutoCube createCube(EntityDataAnalysisService.ResourceSchema schema, int relationDepth) {
        List<String> dimensions = schema.fields().stream()
                .map(EntityDataAnalysisService.FieldSchema::name)
                .toList();

        List<String> measures = new ArrayList<>();
        measures.add("count");

        schema.fields().stream()
                .filter(field -> isNumeric(field.javaType()))
                .forEach(field -> {
                    measures.add("sum:" + field.name());
                    measures.add("avg:" + field.name());
                    measures.add("min:" + field.name());
                    measures.add("max:" + field.name());
                });

        schema.fields().stream()
                .map(EntityDataAnalysisService.FieldSchema::name)
                .forEach(field -> measures.add("countDistinct:" + field));

        Set<String> relationships = schema.fields().stream()
                .map(EntityDataAnalysisService.FieldSchema::name)
                .filter(name -> name.contains("."))
                .map(name -> name.substring(0, name.indexOf('.')))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new AutoCube(
                schema.slug(),
                schema.slug(),
                schema.module(),
                schema.entity(),
                dimensions,
                List.copyOf(new LinkedHashSet<>(measures)),
                List.copyOf(relationships));
    }

    private List<String> validateDimensions(AutoCube cube, List<String> requested) {
        for (String dimension : requested) {
            if (!cube.dimensions().contains(dimension)) {
                throw new IllegalArgumentException(
                        "Unknown OLAP dimension '" + dimension
                                + "' for cube '" + cube.name() + "'");
            }
        }
        return List.copyOf(requested);
    }

    private List<String> validateMeasures(AutoCube cube, List<String> requested) {
        for (String measure : requested) {
            if (!cube.measures().contains(measure)
                    && !measure.equalsIgnoreCase("count")) {
                throw new IllegalArgumentException(
                        "Unknown OLAP measure '" + measure
                                + "' for cube '" + cube.name() + "'");
            }
        }
        return List.copyOf(requested);
    }

    private int normalizeRelationDepth(int relationDepth) {
        return Math.max(0, Math.min(relationDepth, 6));
    }

    private boolean isNumeric(String javaType) {
        return Set.of(
                "byte", "short", "int", "long", "float", "double",
                "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
                "java.lang.Long", "java.lang.Float", "java.lang.Double",
                "java.math.BigDecimal", "java.math.BigInteger"
        ).contains(javaType);
    }

    public record AutoCubeCatalog(
            int cubeCount,
            List<AutoCube> cubes) {
    }

    public record RelationDiscovery(
            String resource,
            String relation,
            List<RelationPath> paths) {
    }

    public record RelationPath(
            String path,
            String targetEntity,
            String relationshipType) {
    }

    public record AutoCube(
            String name,
            String resource,
            String module,
            String entity,
            List<String> dimensions,
            List<String> measures,
            List<String> relationships) {
    }

    public record AutoCubeQueryRequest(
            List<String> dimensions,
            List<String> measures,
            Map<String, String> filters,
            String orderBy,
            Integer limit,
            Boolean includeTotals,
            Boolean includeRollup,
            String pivotDimension,
            String relation,
            Integer relationDepth) {

        public AutoCubeQueryRequest {
            relation = relation == null ? "" : relation.trim();
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
            measures = measures == null ? List.of() : List.copyOf(measures);
            filters = filters == null ? Map.of() : Map.copyOf(filters);
            orderBy = orderBy == null ? "" : orderBy;
            limit = limit == null || limit <= 0 ? 100 : Math.min(limit, 1000);
            includeTotals = Boolean.TRUE.equals(includeTotals);
            includeRollup = Boolean.TRUE.equals(includeRollup);
            pivotDimension = pivotDimension == null ? "" : pivotDimension;
            relationDepth = relationDepth == null ? 2 : Math.max(0, Math.min(relationDepth, 6));
        }
    }
}
