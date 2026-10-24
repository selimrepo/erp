import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Column } from "primereact/column";
import { DataTable } from "primereact/datatable";
import { Dropdown } from "primereact/dropdown";
import { InputText } from "primereact/inputtext";
import { Message } from "primereact/message";
import { ProgressBar } from "primereact/progressbar";
import { ProgressSpinner } from "primereact/progressspinner";
import { Tag } from "primereact/tag";
import { useAuth } from "../auth";
import { api } from "../api";
import type { AggregateReport, DataQualityReport, FieldSchema } from "../types";

const NUMERIC_TYPES = new Set([
  "byte", "short", "int", "long", "float", "double", "java.lang.Byte",
  "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float",
  "java.lang.Double", "java.math.BigDecimal", "java.math.BigInteger"
]);
const OPERATIONS = [
  { label: "Sum", value: "sum" },
  { label: "Average", value: "avg" },
  { label: "Minimum", value: "min" },
  { label: "Maximum", value: "max" },
  { label: "Count distinct", value: "countDistinct" },
  { label: "Row count", value: "count" }
];
const errorText = (error: unknown) => error instanceof Error ? error.message : String(error);
const display = (value: unknown) => value == null ? "—" : typeof value === "object" ? JSON.stringify(value) : String(value);

function downloadCsv(report: AggregateReport) {
  const rows = report.data ?? [];
  const headers = [...new Set(rows.flatMap(row => Object.keys(row)))];
  const quote = (value: unknown) => {
    const raw = value == null ? "" : typeof value === "object" ? JSON.stringify(value) : String(value);
    return '"' + raw.replace(/"/g, '""') + '"';
  };
  const csv = [headers.map(quote).join(","), ...rows.map(row => headers.map(key => quote(row[key])).join(","))].join("\r\n");
  const url = URL.createObjectURL(new Blob(["\uFEFF", csv], { type: "text/csv;charset=utf-8;" }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `erp-insights-${report.resource}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function InsightsPage() {
  const { credentials } = useAuth();
  const [slug, setSlug] = useState("");
  const [dimension, setDimension] = useState("");
  const [operation, setOperation] = useState("sum");
  const [measure, setMeasure] = useState("");
  const [filterField, setFilterField] = useState("");
  const [filterValue, setFilterValue] = useState("");
  const [limit, setLimit] = useState(100);
  const [quality, setQuality] = useState<DataQualityReport | null>(null);
  const [aggregate, setAggregate] = useState<AggregateReport | null>(null);
  const [qualityError, setQualityError] = useState("");
  const [aggregateError, setAggregateError] = useState("");
  const catalog = useQuery({
    queryKey: ["insights-catalog", credentials?.username],
    queryFn: () => api.catalog(credentials!),
    enabled: Boolean(credentials),
    staleTime: 60_000
  });
  const resource = catalog.data?.resources.find(item => item.slug === slug);
  const fields = resource?.fields ?? [];
  const dimensions = useMemo(() => fields.map(field => ({ label: field.name, value: field.name })), [fields]);
  const numericFields = useMemo(() => fields.filter((field: FieldSchema) => NUMERIC_TYPES.has(field.javaType)).map(field => ({ label: field.name, value: field.name })), [fields]);
  const metric = operation === "count" ? "count" : `${operation}:${measure}`;

  async function runQuality() {
    if (!credentials || !slug) return;
    setQualityError("");
    try { setQuality(await api.quality(slug, credentials, 10)); }
    catch (error) { setQualityError(errorText(error)); }
  }

  async function runAggregate() {
    if (!credentials || !slug || (operation !== "count" && !measure)) return;
    setAggregateError("");
    setAggregate(null);
    const filters = filterField && filterValue.trim() ? { [filterField]: filterValue.trim() } : {};
    try {
      const result = await api.aggregate(slug, {
        groupBy: dimension ? [dimension] : [],
        metrics: [metric],
        filters,
        orderBy: metric + ":desc",
        limit: Math.max(1, Math.min(1000, limit))
      }, credentials);
      setAggregate(result);
    } catch (error) { setAggregateError(errorText(error)); }
  }

  if (catalog.isPending) return <ProgressSpinner />;
  if (catalog.isError) return <Message severity="error" text={errorText(catalog.error)} />;

  const statusSeverity = quality?.status === "HEALTHY" ? "success" : quality?.status === "NEEDS_ATTENTION" ? "danger" : quality?.status === "NO_DATA" ? "secondary" : "warning";
  const chartData = (aggregate?.data ?? []).map(row => ({ ...row, [dimension || "group"]: dimension ? display(row[dimension]) : "All records", [metric]: Number(row[metric] ?? 0) }));

  return (
    <div className="stack" style={{ display: "grid", gap: "1rem" }}>
      <div className="section-heading">
        <div>
          <div className="eyebrow">BUSINESS INTELLIGENCE</div>
          <h2>Insights Studio</h2>
          <p style={{ margin: 0, color: "var(--text-color-secondary)" }}>Profile data quality, build live aggregate queries, chart results and export datasets.</p>
        </div>
        <Tag value="Database-backed" severity="info" />
      </div>

      <Card title="Choose an ERP dataset" subTitle="All requests use the authenticated analytics API and the resource's READ policy.">
        <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: ".75rem" }}>
          <Dropdown value={slug} options={catalog.data.resources} optionLabel="slug" optionValue="slug"
            onChange={event => { setSlug(event.value); setQuality(null); setAggregate(null); setDimension(""); setMeasure(""); setFilterField(""); setFilterValue(""); }}
            placeholder="Select a resource" filter style={{ minWidth: 260, maxWidth: "100%" }} />
          <Button label="Profile data quality" icon="pi pi-shield" onClick={runQuality} disabled={!slug} />
        </div>
      </Card>

      {qualityError && <Message severity="error" text={qualityError} />}
      {quality && (
        <>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))", gap: ".75rem" }}>
            <Card title="Completeness score">
              <div style={{ fontSize: "1.8rem", fontWeight: 700 }}>{quality.completenessScore.toFixed(1)}%</div>
              <ProgressBar value={quality.completenessScore} showValue={false} />
              <small className="text-500">Average non-null rate across scalar fields</small>
            </Card>
            <Card title="Rows scanned"><div style={{ fontSize: "1.8rem", fontWeight: 700 }}>{quality.rowCount.toLocaleString()}</div><small className="text-500">{quality.fieldCount} scalar fields</small></Card>
            <Card title="Fields with nulls"><div style={{ fontSize: "1.8rem", fontWeight: 700 }}>{quality.fieldsWithMissingValues}</div><small className="text-500">Fields with at least one missing value</small></Card>
            <Card title="Quality status"><div style={{ marginTop: ".35rem" }}><Tag value={quality.status.replaceAll("_", " ")} severity={statusSeverity} /></div><small className="text-500">{quality.fieldsWithHighNullRate} fields have null rates of 20% or more</small></Card>
          </div>
          <Card title="Field quality profile" subTitle="Completeness and distinct-value ratios are diagnostics, not automatic pass/fail rules.">
            <DataTable value={quality.fields} dataKey="name" paginator rows={10} size="small" stripedRows emptyMessage="No scalar fields found.">
              <Column field="name" header="Field" sortable />
              <Column field="javaType" header="Type" />
              <Column field="rowCount" header="Rows" />
              <Column field="nullCount" header="Nulls" sortable />
              <Column field="nullPercent" header="Null %" body={(row: DataQualityReport["fields"][number]) => `${row.nullPercent.toFixed(1)}%`} sortable />
              <Column field="distinctCount" header="Distinct" sortable />
              <Column field="completenessScore" header="Completeness" body={(row: DataQualityReport["fields"][number]) => <ProgressBar value={row.completenessScore} showValue />} />
              <Column header="Finding" body={(row: DataQualityReport["fields"][number]) => row.issues.length ? <Tag value={row.issues.join(", ").replaceAll("_", " ")} severity={row.issues.includes("HIGH_NULL_RATE") ? "danger" : "warning"} /> : <Tag value="No nulls" severity="success" />} />
            </DataTable>
          </Card>
        </>
      )}

      <Card title="Aggregate query builder" subTitle="Group records, calculate a measure, optionally filter one field, and export the result as CSV.">
        {!slug && <Message severity="info" text="Select a resource above to load its available fields." />}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))", gap: ".75rem" }}>
          <label className="field-control"><span>Group by dimension</span><Dropdown value={dimension} options={dimensions} onChange={event => setDimension(event.value ?? "")} placeholder="No grouping (total)" showClear className="w-full" /></label>
          <label className="field-control"><span>Measure operation</span><Dropdown value={operation} options={OPERATIONS} onChange={event => setOperation(event.value)} optionLabel="label" optionValue="value" className="w-full" /></label>
          <label className="field-control"><span>Numeric field</span><Dropdown value={measure} options={numericFields} onChange={event => setMeasure(event.value ?? "")} placeholder="Select measure" disabled={operation === "count"} showClear className="w-full" /></label>
          <label className="field-control"><span>Filter field (optional)</span><Dropdown value={filterField} options={dimensions} onChange={event => setFilterField(event.value ?? "")} placeholder="No filter" showClear className="w-full" /></label>
          <label className="field-control"><span>Filter equals</span><InputText value={filterValue} onChange={event => setFilterValue(event.target.value)} placeholder="Exact value" className="w-full" /></label>
          <label className="field-control"><span>Maximum groups (1–1000)</span><InputText type="number" value={String(limit)} onChange={event => setLimit(Number(event.target.value) || 100)} className="w-full" /></label>
        </div>
        <div style={{ display: "flex", gap: ".5rem", marginTop: "1rem", flexWrap: "wrap" }}>
          <Button label="Run aggregate" icon="pi pi-play" onClick={runAggregate} disabled={!slug || (operation !== "count" && !measure)} />
          {aggregate && <Button label="Export CSV" icon="pi pi-download" severity="secondary" outlined onClick={() => downloadCsv(aggregate)} />}
        </div>
        {aggregateError && <Message severity="error" text={aggregateError} className="mt-3" />}
      </Card>

      {aggregate && (
        <>
          <Card title="Aggregate visualization" subTitle={`${aggregate.data.length} result rows · metric ${metric}`}>
            {aggregate.data.length > 0 && dimension ? (
              <div style={{ width: "100%", height: 340 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData} margin={{ top: 10, right: 20, left: 5, bottom: 65 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey={dimension} angle={-35} textAnchor="end" interval={0} height={80} tickFormatter={value => String(value ?? "—")} />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey={metric} name={metric} fill="#3b82f6" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            ) : <Message severity="info" text="Choose a grouping dimension to display a chart; ungrouped queries show a total row." />}
          </Card>
          <Card title="Query results">
            <DataTable value={aggregate.data} paginator rows={10} size="small" stripedRows emptyMessage="The query returned no rows.">
              {Object.keys(aggregate.data[0] ?? {}).map(key => <Column key={key} field={key} header={key} sortable body={(row: Record<string, unknown>) => display(row[key])} />)}
            </DataTable>
          </Card>
        </>
      )}
    </div>
  );
}
