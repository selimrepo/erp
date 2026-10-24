package com.erplancers.erp.common.reporting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-quality view over the same database-backed scalar profiling used by analytics.
 * Scores describe completeness only; uniqueness and top values are surfaced as diagnostics,
 * not treated as defects automatically.
 */
@Service
@Transactional(readOnly = true)
public class DataQualityAnalyticsService {

    private final EntityDataAnalysisService analysis;

    public DataQualityAnalyticsService(EntityDataAnalysisService analysis) {
        this.analysis = analysis;
    }

    public DataQualityReport inspect(String slug, int topValues) {
        EntityDataAnalysisService.EntityProfile profile = analysis.profile(slug, topValues);
        List<DataQualityField> fields = new ArrayList<>();
        double completenessTotal = 0;
        long fieldsWithMissingValues = 0;
        long fieldsWithHighNullRate = 0;

        for (EntityDataAnalysisService.FieldProfile field : profile.fields()) {
            double completeness = profile.rowCount() == 0 ? 0.0 : 100.0 - field.nullPercent();
            List<String> issues = new ArrayList<>();
            if (profile.rowCount() == 0) {
                issues.add("NO_DATA");
            } else if (field.nullPercent() >= 20.0) {
                issues.add("HIGH_NULL_RATE");
                fieldsWithHighNullRate++;
            } else if (field.nullCount() > 0) {
                issues.add("NULL_VALUES");
            }
            if (field.nullCount() > 0) fieldsWithMissingValues++;
            double distinctPercent = profile.rowCount() == 0
                    ? 0.0 : field.distinctCount() * 100.0 / profile.rowCount();

            fields.add(new DataQualityField(
                    field.name(), field.javaType(), field.rowCount(),
                    field.nonNullCount(), field.nullCount(), field.nullPercent(),
                    field.distinctCount(), distinctPercent, completeness,
                    field.min(), field.max(), field.average(), field.sum(),
                    field.topValues(), issues));
            completenessTotal += completeness;
        }

        double score = fields.isEmpty() || profile.rowCount() == 0
                ? 0.0 : completenessTotal / fields.size();
        String status = profile.rowCount() == 0 ? "NO_DATA"
                : fieldsWithHighNullRate > 0 ? "NEEDS_ATTENTION"
                : fieldsWithMissingValues > 0 ? "REVIEW"
                : "HEALTHY";

        return new DataQualityReport(
                profile.resource(), profile.module(), profile.entity(), profile.rowCount(),
                fields.size(), Math.round(score * 100.0) / 100.0, status,
                fieldsWithMissingValues, fieldsWithHighNullRate, fields);
    }

    public record DataQualityReport(
            String resource,
            String module,
            String entity,
            long rowCount,
            int fieldCount,
            double completenessScore,
            String status,
            long fieldsWithMissingValues,
            long fieldsWithHighNullRate,
            List<DataQualityField> fields) {}

    public record DataQualityField(
            String name,
            String javaType,
            long rowCount,
            long nonNullCount,
            long nullCount,
            double nullPercent,
            long distinctCount,
            double distinctPercent,
            double completenessScore,
            Object min,
            Object max,
            Double average,
            Object sum,
            List<EntityDataAnalysisService.ValueCount> topValues,
            List<String> issues) {}
}
