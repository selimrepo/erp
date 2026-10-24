package com.erplancers.erp.common.service;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.common.web.CrudRegistry;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DynamicCrudService {
    private final CrudRegistry registry;
    private final DefaultConversionService conversionService = new DefaultConversionService();

    public DynamicCrudService(CrudRegistry registry) {
        this.registry = registry;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> list(String slug, int page, int size) {
        CrudRegistry.CrudDescriptor descriptor = registry.get(slug);
        return descriptor.repository().findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))).map(this::row);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String slug, Long id) {
        var optional = registry.get(slug).repository().findById(id);
        if (optional.isEmpty()) throw new IllegalArgumentException("Resource not found: " + id);
        return row(optional.get());
    }

    @Transactional
    public EntityBase save(String slug, Long id, Map<String, String> values) {
        CrudRegistry.CrudDescriptor descriptor = registry.get(slug);
        EntityBase entity;
        if (id == null) {
            try {
                entity = (EntityBase) descriptor.entityType().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Entity needs a no-args constructor: " + descriptor.entityType().getName(), e);
            }
        } else {
            var optional = descriptor.repository().findById(id);
            if (optional.isEmpty()) throw new IllegalArgumentException("Resource not found: " + id);
            entity = (EntityBase) optional.get();
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        Map<String, String> scalarValues = values.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("ref.") && !e.getKey().startsWith("collection."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

        scalarValues.forEach((name, value) -> {
            if (!wrapper.isWritableProperty(name) || value == null || value.isBlank()) return;
            Class<?> propertyType = wrapper.getPropertyType(name);
            if (propertyType == null || !isSimple(propertyType)) return;
            Object converted = conversionService.convert(value, propertyType);
            if (converted != null) wrapper.setPropertyValue(name, converted);
        });

        applyReferences(wrapper, descriptor.entityType(), values);
        applyNestedCollections(wrapper, descriptor.entityType(), values);
        return (EntityBase) descriptor.repository().save(entity);
    }

    private void applyReferences(BeanWrapper wrapper, Class<?> entityType, Map<String, String> values) {
        for (PropertyDescriptor property : properties(entityType)) {
            String key = "ref." + property.getName();
            String rawId = values.get(key);
            if (rawId == null || rawId.isBlank() || !wrapper.isWritableProperty(property.getName())) continue;
            Class<?> target = property.getPropertyType();
            if (target == null || !EntityBase.class.isAssignableFrom(target)) continue;
            CrudRegistry.CrudDescriptor targetDescriptor = findByEntityType(target);
            EntityBase reference = findRelated(targetDescriptor, Long.valueOf(rawId), target.getSimpleName());
            wrapper.setPropertyValue(property.getName(), reference);
        }

        for (PropertyDescriptor property : properties(entityType)) {
            String key = "collection." + property.getName();
            String rawIds = values.get(key);
            if (rawIds == null || !wrapper.isWritableProperty(property.getName())) continue;
            Class<?> elementType = collectionElementType(property);
            if (elementType == null || !EntityBase.class.isAssignableFrom(elementType)) continue;
            CrudRegistry.CrudDescriptor targetDescriptor = findByEntityType(elementType);
            Collection<Object> related = Arrays.stream(rawIds.split(","))
                    .map(String::trim).filter(s -> !s.isBlank()).map(Long::valueOf)
                    .map(id -> findRelated(targetDescriptor, id, elementType.getSimpleName()))
                    .collect(Collectors.toCollection(ArrayList::new));
            wrapper.setPropertyValue(property.getName(), collectionValue(wrapper.getPropertyType(property.getName()), related));
        }
    }


    private void applyNestedCollections(BeanWrapper parentWrapper, Class<?> parentType, Map<String, String> values) {
        for (PropertyDescriptor property : properties(parentType)) {
            if (!isCollection(property) || !parentWrapper.isWritableProperty(property.getName())) continue;
            Class<?> childType = collectionElementType(property);
            if (childType == null || !EntityBase.class.isAssignableFrom(childType)) continue;
            CrudRegistry.CrudDescriptor childDescriptor = findByEntityType(childType);
            String prefix = "nested." + property.getName() + ".";
            Map<Integer, Map<String, String>> rows = new TreeMap<>();
            values.forEach((key, value) -> {
                if (!key.startsWith(prefix)) return;
                String remainder = key.substring(prefix.length());
                int dot = remainder.indexOf('.');
                if (dot <= 0) return;
                try {
                    int index = Integer.parseInt(remainder.substring(0, dot));
                    rows.computeIfAbsent(index, ignored -> new LinkedHashMap<>()).put(remainder.substring(dot + 1), value);
                } catch (NumberFormatException ignored) {}
            });
            if (rows.isEmpty()) continue;
            List<Object> activeChildren = new ArrayList<>();
            for (Map<String, String> row : rows.values()) {
                Long childId = parseId(row.get("_id"));
                if ("true".equalsIgnoreCase(row.get("_delete"))) {
                    if (childId != null) childDescriptor.repository().deleteById(childId);
                    continue;
                }
                activeChildren.add(saveNestedEntity(childDescriptor, childId, row));
            }
            parentWrapper.setPropertyValue(property.getName(), collectionValue(parentWrapper.getPropertyType(property.getName()), activeChildren));
        }
    }

    private EntityBase saveNestedEntity(CrudRegistry.CrudDescriptor descriptor, Long id, Map<String, String> values) {
        EntityBase entity;
        if (id == null) {
            try {
                entity = (EntityBase) descriptor.entityType().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Nested entity needs a no-args constructor: " + descriptor.entityType().getName(), e);
            }
        } else {
            Object found = descriptor.repository().findById(id).orElse(null);
            if (!(found instanceof EntityBase existing)) throw new IllegalArgumentException("Nested resource not found: " + id);
            entity = existing;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        values.forEach((name, value) -> {
            if ("_id".equals(name) || "_delete".equals(name) || name.startsWith("ref.") || name.startsWith("collection.")) return;
            if (!wrapper.isWritableProperty(name) || value == null || value.isBlank()) return;
            Class<?> propertyType = wrapper.getPropertyType(name);
            if (propertyType == null || !isSimple(propertyType)) return;
            Object converted = conversionService.convert(value, propertyType);
            if (converted != null) wrapper.setPropertyValue(name, converted);
        });
        applyReferences(wrapper, descriptor.entityType(), values);
        return (EntityBase) descriptor.repository().save(entity);
    }

    private Long parseId(String value) {
        if (value == null || value.isBlank()) return null;
        return Long.valueOf(value);
    }

    private List<Map<String, Object>> nestedCollectionMetadata(Class<?> type, Long id) {
        Map<String, Object> currentValues = new LinkedHashMap<>();
        if (id != null) {
            Object parent = findByEntityType(type).repository().findById(id).orElse(null);
            if (parent instanceof EntityBase entity) {
                BeanWrapper parentWrapper = new BeanWrapperImpl(entity);
                for (PropertyDescriptor property : properties(type)) {
                    if (isCollection(property) && parentWrapper.isReadableProperty(property.getName())) {
                        currentValues.put(property.getName(), parentWrapper.getPropertyValue(property.getName()));
                    }
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (PropertyDescriptor property : properties(type)) {
            if (!isCollection(property) || property.getWriteMethod() == null) continue;
            Class<?> childType = collectionElementType(property);
            if (childType == null || !EntityBase.class.isAssignableFrom(childType)) continue;
            CrudRegistry.CrudDescriptor childDescriptor = findByEntityType(childType);
            Map<String, Object> collection = new LinkedHashMap<>();
            collection.put("name", property.getName());
            collection.put("type", childType.getSimpleName());
            collection.put("slug", childDescriptor.slug());
            collection.put("fields", scalarMetadata(childType));
            collection.put("references", referenceMetadata(childType));
            List<Map<String, Object>> rows = new ArrayList<>();
            Object current = currentValues.get(property.getName());
            if (current instanceof Collection<?> children) {
                for (Object child : children) {
                    BeanWrapper childWrapper = new BeanWrapperImpl(child);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", ((EntityBase) child).getId());
                    Map<String, Object> rowValues = new LinkedHashMap<>();
                    for (PropertyDescriptor field : properties(childType)) {
                        if (isSimple(field.getPropertyType()) && childWrapper.isReadableProperty(field.getName())) rowValues.put(field.getName(), childWrapper.getPropertyValue(field.getName()));
                    }
                    row.put("values", rowValues);
                    Map<String, Object> selected = new LinkedHashMap<>();
                    for (PropertyDescriptor field : properties(childType)) {
                        if (isReference(field) && childWrapper.isReadableProperty(field.getName())) {
                            Object ref = childWrapper.getPropertyValue(field.getName());
                            if (ref instanceof EntityBase related) selected.put(field.getName(), related.getId());
                        }
                    }
                    row.put("selected", selected);
                    rows.add(row);
                }
            }
            collection.put("rows", rows);
            result.add(collection);
        }
        return result;
    }

    private Map<String, List<Map<String, Object>>> nestedOptions(Class<?> type) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (PropertyDescriptor parentProperty : properties(type)) {
            if (!isCollection(parentProperty)) continue;
            Class<?> childType = collectionElementType(parentProperty);
            if (childType == null || !EntityBase.class.isAssignableFrom(childType)) continue;
            for (PropertyDescriptor childProperty : properties(childType)) {
                if (!isReference(childProperty)) continue;
                CrudRegistry.CrudDescriptor targetDescriptor = findByEntityType(childProperty.getPropertyType());
                List<Map<String, Object>> options = new ArrayList<>();
                targetDescriptor.repository().findAll(PageRequest.of(0, 100)).forEach(entity -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("id", ((EntityBase) entity).getId());
                    option.put("label", displayLabel(entity));
                    options.add(option);
                });
                result.put(parentProperty.getName() + "." + childProperty.getName(), options);
            }
        }
        return result;
    }

    @Transactional
    public void delete(String slug, Long id) {
        registry.get(slug).repository().deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> formMetadata(String slug, Long id) {
        CrudRegistry.CrudDescriptor descriptor = registry.get(slug);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fields", scalarMetadata(descriptor.entityType()));
        metadata.put("references", referenceMetadata(descriptor.entityType()));
        metadata.put("collections", collectionMetadata(descriptor.entityType()));
        metadata.put("options", relationshipOptions(descriptor.entityType()));
        metadata.put("values", id == null ? Map.of() : get(slug, id));
        metadata.put("selected", id == null ? Map.of() : selectedRelationships(descriptor.entityType(), id));
        metadata.put("nestedCollections", nestedCollectionMetadata(descriptor.entityType(), id));
        metadata.put("nestedOptions", nestedOptions(descriptor.entityType()));
        return metadata;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> counts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        registry.all().values().forEach(d -> counts.put(d.slug(), d.repository().count()));
        return counts;
    }

    private Map<String, String> scalarMetadata(Class<?> type) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (PropertyDescriptor p : properties(type)) {
            if (isSimple(p.getPropertyType()) && p.getWriteMethod() != null) fields.put(p.getName(), p.getPropertyType().getSimpleName());
        }
        return fields;
    }

    private List<Map<String, String>> referenceMetadata(Class<?> type) {
        List<Map<String, String>> result = new ArrayList<>();
        for (PropertyDescriptor p : properties(type)) {
            if (!isReference(p) || p.getWriteMethod() == null) continue;
            Map<String, String> row = new LinkedHashMap<>();
            row.put("name", p.getName());
            row.put("type", p.getPropertyType().getSimpleName());
            row.put("slug", findByEntityType(p.getPropertyType()).slug());
            result.add(row);
        }
        return result;
    }

    private List<Map<String, String>> collectionMetadata(Class<?> type) {
        List<Map<String, String>> result = new ArrayList<>();
        for (PropertyDescriptor p : properties(type)) {
            if (!isCollection(p) || isNestedCollection(p) || p.getWriteMethod() == null) continue;
            Class<?> elementType = collectionElementType(p);
            if (elementType == null || !EntityBase.class.isAssignableFrom(elementType)) continue;
            Map<String, String> row = new LinkedHashMap<>();
            row.put("name", p.getName());
            row.put("type", elementType.getSimpleName());
            row.put("slug", findByEntityType(elementType).slug());
            result.add(row);
        }
        return result;
    }

    private Map<String, List<Map<String, Object>>> relationshipOptions(Class<?> type) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (PropertyDescriptor p : properties(type)) {
            Class<?> target = isReference(p) ? p.getPropertyType() : collectionElementType(p);
            if (target == null || !EntityBase.class.isAssignableFrom(target)) continue;
            CrudRegistry.CrudDescriptor d = findByEntityType(target);
            List<Map<String, Object>> options = new ArrayList<>();
            d.repository().findAll(PageRequest.of(0, 100)).forEach(entity -> {
                Map<String, Object> option = new LinkedHashMap<>();
                option.put("id", ((EntityBase) entity).getId());
                option.put("label", displayLabel(entity));
                options.add(option);
            });
            result.put(p.getName(), options);
        }
        return result;
    }

    private EntityBase findRelated(CrudRegistry.CrudDescriptor descriptor, Long id, String typeName) {
        Object found = descriptor.repository().findById(id).orElse(null);
        if (!(found instanceof EntityBase entity)) {
            throw new IllegalArgumentException("Related " + typeName + " not found: " + id);
        }
        return entity;
    }

    private Object collectionValue(Class<?> collectionType, Collection<Object> values) {
        if (collectionType != null && Set.class.isAssignableFrom(collectionType)) return new LinkedHashSet<>(values);
        if (collectionType != null && Queue.class.isAssignableFrom(collectionType)) return new LinkedList<>(values);
        return new ArrayList<>(values);
    }

    private Map<String, Object> selectedRelationships(Class<?> type, Long id) {
        Object entity = findByEntityType(type).repository().findById(id).orElse(null);
        if (!(entity instanceof EntityBase)) throw new IllegalArgumentException("Resource not found: " + id);
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        Map<String, Object> selected = new LinkedHashMap<>();
        for (PropertyDescriptor p : properties(type)) {
            if (isReference(p) && wrapper.isReadableProperty(p.getName())) {
                Object value = wrapper.getPropertyValue(p.getName());
                if (value instanceof EntityBase related) selected.put(p.getName(), related.getId());
            } else if (isCollection(p) && wrapper.isReadableProperty(p.getName())) {
                Object value = wrapper.getPropertyValue(p.getName());
                if (value instanceof Collection<?> collection) selected.put(p.getName(), collection.stream()
                        .filter(EntityBase.class::isInstance).map(EntityBase.class::cast).map(EntityBase::getId).toList());
            }
        }
        return selected;
    }

    private CrudRegistry.CrudDescriptor findByEntityType(Class<?> type) {
        return registry.all().values().stream()
                .filter(d -> d.entityType().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No repository registered for " + type.getName()));
    }

    private String displayLabel(Object entity) {
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        for (String name : List.of("name", "code", "title", "number", "email", "description")) {
            if (wrapper.isReadableProperty(name)) {
                Object value = wrapper.getPropertyValue(name);
                if (value != null && !value.toString().isBlank()) return entity.getClass().getSimpleName() + " · " + value;
            }
        }
        return entity.getClass().getSimpleName() + " #" + ((EntityBase) entity).getId();
    }

    private Map<String, Object> row(Object entity) {
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        Map<String, Object> row = new LinkedHashMap<>();
        for (var property : wrapper.getPropertyDescriptors()) {
            if ("class".equals(property.getName()) || property.getReadMethod() == null) continue;
            Class<?> type = property.getPropertyType();
            if (type != null && isSimple(type)) {
                try { row.put(property.getName(), wrapper.getPropertyValue(property.getName())); } catch (RuntimeException ignored) {}
            }
        }
        return row;
    }

    private List<PropertyDescriptor> properties(Class<?> type) {
        try {
            return Arrays.asList(Introspector.getBeanInfo(type).getPropertyDescriptors());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot inspect " + type.getName(), e);
        }
    }

    private boolean isReference(PropertyDescriptor p) {
        return p.getPropertyType() != null
                && (hasAnnotation(p, ManyToOne.class) || hasAnnotation(p, OneToOne.class))
                && EntityBase.class.isAssignableFrom(p.getPropertyType());
    }

    private boolean isNestedCollection(PropertyDescriptor p) {
        return Collection.class.isAssignableFrom(p.getPropertyType())
                && hasAnnotation(p, OneToMany.class);
    }

    private boolean isCollection(PropertyDescriptor p) {
        return Collection.class.isAssignableFrom(p.getPropertyType())
                && (hasAnnotation(p, OneToMany.class) || hasAnnotation(p, ManyToMany.class));
    }

    private boolean hasAnnotation(PropertyDescriptor p, Class<? extends Annotation> annotationType) {
        if (p.getReadMethod() != null && p.getReadMethod().isAnnotationPresent(annotationType)) return true;
        if (p.getWriteMethod() != null && p.getWriteMethod().isAnnotationPresent(annotationType)) return true;
        try {
            Field field = p.getPropertyType() == null ? null : findField(p.getReadMethod().getDeclaringClass(), p.getName());
            return field != null && field.isAnnotationPresent(annotationType);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private Class<?> collectionElementType(PropertyDescriptor p) {
        Type type = p.getReadMethod().getGenericReturnType();
        if (type instanceof ParameterizedType pt && pt.getActualTypeArguments()[0] instanceof Class<?> c) return c;
        return null;
    }

    private boolean isSimple(Class<?> type) {
        return type != null && (type.isPrimitive() || Number.class.isAssignableFrom(type) || CharSequence.class.isAssignableFrom(type)
                || Boolean.class == type || type.isEnum() || UUID.class == type || Date.class.isAssignableFrom(type)
                || Temporal.class.isAssignableFrom(type));
    }
}
