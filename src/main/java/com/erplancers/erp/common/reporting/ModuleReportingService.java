package com.erplancers.erp.common.reporting;

import com.erplancers.erp.common.web.CrudRegistry;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ModuleReportingService {
    private final CrudRegistry registry;

    public ModuleReportingService(CrudRegistry registry) {
        this.registry = registry;
    }

    public Map<String, ModuleReport> all() {
        return registry.all().values().stream()
                .collect(Collectors.groupingBy(
                        CrudRegistry.CrudDescriptor::module,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), this::reportForDescriptors)));
    }

    public ModuleReport module(String module) {
        List<CrudRegistry.CrudDescriptor> descriptors = registry.all().values().stream()
                .filter(d -> d.module().equals(module))
                .toList();
        if (descriptors.isEmpty()) {
            throw new IllegalArgumentException("Unknown ERP module: " + module);
        }
        return reportForDescriptors(descriptors);
    }

    private ModuleReport reportForDescriptors(List<CrudRegistry.CrudDescriptor> descriptors) {
        List<ResourceReport> resources = descriptors.stream().map(this::resourceReport).toList();
        long total = resources.stream().mapToLong(ResourceReport::count).sum();
        long populated = resources.stream().filter(r -> r.count() > 0).count();

        return new ModuleReport(
                descriptors.get(0).module(),
                total,
                resources.size(),
                populated,
                resources,
                resources.stream()
                        .flatMap(r -> r.numeric().entrySet().stream()
                                .map(e -> new NumericMetric(r.name(), e.getKey(), e.getValue())))
                        .toList());
    }

    private ResourceReport resourceReport(CrudRegistry.CrudDescriptor descriptor) {
        long count = descriptor.repository().count();
        int pageSize = Math.min(100, Math.max(1, (int) Math.min(count, 100)));
        List<?> sample = descriptor.repository()
                .findAll(PageRequest.of(0, pageSize))
                .getContent();

        Map<String, NumericSummary> numeric = new LinkedHashMap<>();
        Map<String, Map<String, Long>> enums = new LinkedHashMap<>();

        for (PropertyDescriptor property : properties(descriptor.entityType())) {
            String name = property.getName();

            if (property.getReadMethod() == null
                    || name.equals("class")
                    || name.equals("id")
                    || name.equals("version")) {
                continue;
            }

            Class<?> type = property.getPropertyType();

            if (isNumber(type)) {
                numeric.put(name, numericSummary(sample, property));
            } else if (type != null && type.isEnum()) {
                Map<String, Long> distribution = new LinkedHashMap<>();

                for (Object entity : sample) {
                    Object value = read(entity, name);
                    if (value != null) {
                        distribution.merge(value.toString(), 1L, Long::sum);
                    }
                }

                if (!distribution.isEmpty()) {
                    enums.put(name, distribution);
                }
            }
        }

        return new ResourceReport(
                descriptor.slug(),
                descriptor.entityType().getSimpleName(),
                count,
                numeric,
                enums);
    }

    private NumericSummary numericSummary(List<?> sample, PropertyDescriptor property) {
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = null;
        long populated = 0;

        for (Object entity : sample) {
            Object value = read(entity, property.getName());
            if (!(value instanceof Number number)) {
                continue;
            }

            BigDecimal decimal = new BigDecimal(number.toString());
            sum = sum.add(decimal);
            min = min == null || decimal.compareTo(min) < 0 ? decimal : min;
            max = max == null || decimal.compareTo(max) > 0 ? decimal : max;
            populated++;
        }

        return new NumericSummary(sum, min, max, populated);
    }

    private Object read(Object entity, String property) {
        try {
            return new BeanWrapperImpl(entity).getPropertyValue(property);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<PropertyDescriptor> properties(Class<?> type) {
        try {
            return Arrays.asList(Introspector.getBeanInfo(type).getPropertyDescriptors());
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isNumber(Class<?> type) {
        return type != null
                && (Number.class.isAssignableFrom(type)
                || type == byte.class
                || type == short.class
                || type == int.class
                || type == long.class
                || type == float.class
                || type == double.class);
    }

    public record ModuleReport(
            String module,
            long totalRecords,
            int resourceCount,
            long populatedResources,
            List<ResourceReport> resources,
            List<NumericMetric> numericMetrics) {
    }

    public record ResourceReport(
            String slug,
            String name,
            long count,
            Map<String, NumericSummary> numeric,
            Map<String, Map<String, Long>> enums) {
    }

    public record NumericSummary(
            BigDecimal sum,
            BigDecimal min,
            BigDecimal max,
            long populated) {
    }

    public record NumericMetric(
            String resource,
            String field,
            NumericSummary summary) {
    }
}
