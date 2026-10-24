package com.erplancers.erp.common.web;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.common.repository.ErpRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CrudRegistry {
    private final ApplicationContext context;
    private final Map<String, CrudDescriptor> descriptors = new LinkedHashMap<>();

    public CrudRegistry(ApplicationContext context) {
        this.context = context;
    }

    @PostConstruct
    void initialize() {
        context.getBeansOfType(ErpRepository.class).forEach((beanName, repository) -> register(repository));
    }

    private void register(ErpRepository repository) {
        Class<?> entityType = resolveEntityType(repository.getClass());
        if (entityType == null || !EntityBase.class.isAssignableFrom(entityType)) return;

        String slug = toSlug(entityType.getSimpleName());
        String baseSlug = slug;
        int suffix = 2;
        while (descriptors.containsKey(slug)) slug = baseSlug + "-" + suffix++;
        descriptors.put(slug, new CrudDescriptor(
                slug,
                entityType,
                repository,
                entityType.getPackageName().replace("com.erplancers.erp.", "").split("\\.")[0]
        ));
    }

    private Class<?> resolveEntityType(Class<?> type) {
        if (type == null) return null;
        for (Type genericInterface : type.getGenericInterfaces()) {
            Class<?> result = resolveEntityType(genericInterface);
            if (result != null) return result;
        }
        return resolveEntityType(type.getSuperclass());
    }

    private Class<?> resolveEntityType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            Type raw = parameterized.getRawType();
            if (raw instanceof Class<?> rawClass) {
                if (ErpRepository.class.isAssignableFrom(rawClass)) {
                    Type argument = parameterized.getActualTypeArguments()[0];
                    if (argument instanceof Class<?> argumentClass) return argumentClass;
                    if (argument instanceof ParameterizedType nested && nested.getRawType() instanceof Class<?> nestedClass) return nestedClass;
                }
                return resolveEntityType(rawClass);
            }
        }
        if (type instanceof Class<?> clazz) return resolveEntityType(clazz);
        return null;
    }

    public CrudDescriptor get(String slug) {
        CrudDescriptor descriptor = descriptors.get(slug);
        if (descriptor == null) throw new IllegalArgumentException("Unknown ERP resource: " + slug);
        return descriptor;
    }

    public Map<String, CrudDescriptor> all() {
        return Map.copyOf(descriptors);
    }

    private static String toSlug(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    public record CrudDescriptor(
            String slug,
            Class<?> entityType,
            ErpRepository repository,
            String module
    ) {}
}
