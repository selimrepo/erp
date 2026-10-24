import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { Card } from "primereact/card";
import { Dropdown } from "primereact/dropdown";
import { MultiSelect } from "primereact/multiselect";
import { Button } from "primereact/button";
import { InputText } from "primereact/inputtext";
import { InputNumber } from "primereact/inputnumber";
import { Checkbox } from "primereact/checkbox";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Message } from "primereact/message";
import { ProgressSpinner } from "primereact/progressspinner";
import { Chart } from "primereact/chart";
import { Tag } from "primereact/tag";
import { useAuth } from "../auth";
import { api } from "../api";
import { BiWorkspacePanel } from "../components/BiWorkspacePanel";

type FilterOperator = "eq" | "ne" | "like" | "gt" | "gte" | "lt" | "lte" | "isnull" | "notnull";
type BiRow = Record<string, unknown>;
type BiResult = { rows?: BiRow[]; grandTotal?: BiRow; rowCount?: number };
const OPERATORS: Array<{ label: string; value: FilterOperator }> = [
  { label: "Equals", value: "eq" }, { label: "Not equal", value: "ne" },
  { label: "Contains (SQL LIKE)", value: "like" }, { label: "Greater than", value: "gt" },
  { label: "Greater or equal", value: "gte" }, { label: "Less than", value: "lt" },
  { label: "Less or equal", value: "lte" }, { label: "Is null", value: "isnull" },
  { label: "Is not null", value: "notnull" }
];
const NUMERIC_TYPES = /byte|short|integer|(^|\.)int$|long|double|float|bigdecimal|biginteger/i;

function csvCell(value: unknown): string {
  const text = value == null ? "" : typeof value === "object" ? JSON.stringify(value) : String(value);
  return '"' + text.replace(/"/g, '""') + '"';
}
function downloadCsv(rows: BiRow[], filename: string) {
  if (!rows.length) return;
  const headers = Array.from(new Set(rows.flatMap(row => Object.keys(row))));
  const csv = [headers.map(csvCell).join(","), ...rows.map(row => headers.map(key => csvCell(row[key])).join(","))].join("\r\n");
  const url = URL.createObjectURL(new Blob(["\uFEFF", csv], { type: "text/csv;charset=utf-8;" }));
  const link = document.createElement("a");
  link.href = url; link.download = filename; document.body.appendChild(link); link.click(); link.remove();
  URL.revokeObjectURL(url);
}

function htmlCell(value: unknown): string {
  const text = value == null ? "" : typeof value === "object" ? JSON.stringify(value) : String(value);
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
function downloadExcel(rows: BiRow[], filename: string) {
  if (!rows.length) return;
  const headers = Array.from(new Set(rows.flatMap(row => Object.keys(row))));
  const table = "<table><thead><tr>" + headers.map(h => "<th>" + htmlCell(h) + "</th>").join("")
    + "</tr></thead><tbody>" + rows.map(row => "<tr>" + headers.map(h => "<td>" + htmlCell(row[h]) + "</td>").join("") + "</tr>").join("")
    + "</tbody></table>";
  const blob = new Blob(["<html><head><meta charset=\"utf-8\"></head><body>" + table + "</body></html>"],
    { type: "application/vnd.ms-excel;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url; link.download = filename; document.body.appendChild(link); link.click(); link.remove();
  URL.revokeObjectURL(url);
}
function printPdf(rows: BiRow[], title: string) {
  if (!rows.length) return;
  const target = window.open("", "_blank");
  if (!target) return;
  const headers = Array.from(new Set(rows.flatMap(row => Object.keys(row))));
  const table = "<table><thead><tr>" + headers.map(h => "<th>" + htmlCell(h) + "</th>").join("")
    + "</tr></thead><tbody>" + rows.map(row => "<tr>" + headers.map(h => "<td>" + htmlCell(row[h]) + "</td>").join("") + "</tr>").join("")
    + "</tbody></table>";
  target.document.write("<!doctype html><html><head><meta charset=\"utf-8\"><title>" + htmlCell(title)
    + "</title><style>body{font:12px Arial;padding:20px}table{border-collapse:collapse;width:100%}th,td{border:1px solid #bbb;padding:5px;text-align:left;overflow-wrap:anywhere}th{background:#eee}h1{font-size:18px}</style></head><body><h1>"
    + htmlCell(title) + "</h1>" + table + "</body></html>");
  target.document.close();
  target.focus();
  target.print();
}

export function BiPage() {
  const { slug = "" } = useParams();
  const { credentials } = useAuth();
  const [dimensions, setDimensions] = useState<string[]>([]);
  const [measures, setMeasures] = useState<string[]>(["count"]);
  const [filterField, setFilterField] = useState("");
  const [filterOperator, setFilterOperator] = useState<FilterOperator>("eq");
  const [filterValue, setFilterValue] = useState("");
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [orderBy, setOrderBy] = useState("count:desc");
  const [limit, setLimit] = useState(100);
  const [includeRank, setIncludeRank] = useState(true);
  const [includeShare, setIncludeShare] = useState(false);
  const [includeGrandTotal, setIncludeGrandTotal] = useState(true);
  const [calculatedMeasures, setCalculatedMeasures] = useState<Record<string, string>>({});
  const [result, setResult] = useState<BiResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const catalog = useQuery({ queryKey: ["catalog"], queryFn: () => api.catalog(credentials!), enabled: !!credentials });
  const schema = catalog.data?.resources.find(resource => resource.slug === slug);
  const fields = useMemo(() => schema?.fields.filter(field => !["id", "version"].includes(field.name)) ?? [], [schema]);
  const dimensionOptions = useMemo(() => fields.map(field => ({ label: field.name, value: field.name })), [fields]);
  const measureOptions = useMemo(() => [
    { label: "Count records", value: "count" },
    ...fields.flatMap(field => {
      const choices: Array<{ label: string; value: string }> = [{ label: "Distinct " + field.name, value: "countDistinct:" + field.name }];
      if (NUMERIC_TYPES.test(field.javaType)) {
        choices.push(
          { label: "Sum · " + field.name, value: "sum:" + field.name },
          { label: "Average · " + field.name, value: "avg:" + field.name },
          { label: "Minimum · " + field.name, value: "min:" + field.name },
          { label: "Maximum · " + field.name, value: "max:" + field.name }
        );
      }
      return choices;
    })
  ], [fields]);

  function addFilter() {
    if (!filterField) return;
    const requiresValue = filterOperator !== "isnull" && filterOperator !== "notnull";
    if (requiresValue && !filterValue.trim()) return;
    setFilters(previous => ({ ...previous, [filterField + ":" + filterOperator]: requiresValue ? filterValue.trim() : "" }));
    setFilterField(""); setFilterValue(""); setFilterOperator("eq");
  }
  function runQuery() {
    if (!credentials || !slug || dimensions.length === 0 || measures.length === 0) return;
    setError(null); setResult(null); setLoading(true);
    api.biQuery(slug, {
      dimensions, measures, filters, orderBy: orderBy || undefined,
      limit: Math.min(1000, Math.max(1, limit)), includeRank,
      includeShareOfTotal: includeShare, includeGrandTotal, calculatedMeasures
    }, credentials)
      .then((data: BiResult) => setResult(data))
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "BI query failed"))
      .finally(() => setLoading(false));
  }

  if (catalog.isPending) return <ProgressSpinner />;
  if (catalog.isError) return <Message severity="error" text={catalog.error.message} />;
  if (!schema) return <Message severity="warn" text={'Resource "' + slug + '" is not available in the BI catalog.'} />;

  const rows = result?.rows ?? [];
  const columns = rows.length ? Array.from(new Set(rows.flatMap(row => Object.keys(row)))) : [];
  const chartRows = rows.slice(0, 20);
  const chartLabels = chartRows.map(row => dimensions.map(dimension => String(row[dimension] ?? "Unknown")).join(" / "));
  const chartData = result ? {
    labels: chartLabels,
    datasets: [...measures, ...Object.keys(calculatedMeasures)].map(measure => ({
      label: measure,
      data: chartRows.map(row => { const value = row[measure]; return typeof value === "number" ? value : Number(value ?? 0); })
    }))
  } : undefined;

  return (
    <>
      <div className="section-heading"><div><div className="eyebrow">BUSINESS INTELLIGENCE</div><h2>{slug}</h2>
        <p>Build multi-dimensional queries, apply filters, rank results and export a data slice.</p></div></div>
      <Card title="Query designer" className="mb-3">
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "1rem" }}>
          <label className="flex flex-column gap-2"><span>Dimensions (group by)</span>
            <MultiSelect value={dimensions} options={dimensionOptions} optionLabel="label" optionValue="value"
              onChange={event => setDimensions(event.value ?? [])} placeholder="Select dimensions" display="chip" filter className="w-full" maxSelectedLabels={2} />
          </label>
          <label className="flex flex-column gap-2"><span>Measures</span>
            <MultiSelect value={measures} options={measureOptions} optionLabel="label" optionValue="value"
              onChange={event => setMeasures(event.value ?? [])} placeholder="Select measures" display="chip" filter className="w-full" maxSelectedLabels={2} />
          </label>
          <label className="flex flex-column gap-2"><span>Sort results</span>
            <Dropdown value={orderBy} options={[
              ...measures.map(measure => ({ label: measure + " · descending", value: measure + ":desc" })),
              ...measures.map(measure => ({ label: measure + " · ascending", value: measure + ":asc" })),
              ...dimensions.flatMap(dimension => [
                { label: dimension + " · A to Z", value: dimension + ":asc" },
                { label: dimension + " · Z to A", value: dimension + ":desc" }
              ])
            ]} onChange={event => setOrderBy(event.value)} placeholder="Sort by" className="w-full" />
          </label>
          <label className="flex flex-column gap-2"><span>Maximum result rows</span>
            <InputNumber value={limit} onValueChange={event => setLimit(event.value ?? 100)} min={1} max={1000} step={25} showButtons className="w-full" />
          </label>
        </div>
        <div style={{ marginTop: "1.25rem", paddingTop: "1rem", borderTop: "1px solid var(--surface-border)" }}>
          <h3 style={{ marginTop: 0 }}>Filters</h3>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: ".75rem", alignItems: "end" }}>
            <label className="flex flex-column gap-2"><span>Field</span>
              <Dropdown value={filterField} options={dimensionOptions} onChange={event => setFilterField(event.value)}
                placeholder="Choose field" className="w-full" filter />
            </label>
            <label className="flex flex-column gap-2"><span>Operator</span>
              <Dropdown value={filterOperator} options={OPERATORS} onChange={event => setFilterOperator(event.value)}
                optionLabel="label" optionValue="value" className="w-full" />
            </label>
            {filterOperator !== "isnull" && filterOperator !== "notnull" && (
              <label className="flex flex-column gap-2"><span>Value</span>
                <InputText value={filterValue} onChange={event => setFilterValue(event.target.value)}
                  onKeyDown={event => { if (event.key === "Enter") addFilter(); }} placeholder="Filter value" />
              </label>
            )}
            <Button label="Add filter" icon="pi pi-plus" onClick={addFilter} disabled={!filterField ||
              ((filterOperator !== "isnull" && filterOperator !== "notnull") && !filterValue.trim())} />
          </div>
          <div className="flex flex-wrap gap-2" style={{ marginTop: ".75rem" }}>
            {Object.entries(filters).map(([key, value]) => (
              <Tag key={key} value={key + (value ? " = " + value : "")} icon="pi pi-filter" severity="info" />
            ))}
            {Object.keys(filters).length > 0 && <Button label="Clear filters" icon="pi pi-times" text size="small" onClick={() => setFilters({})} />}
            {Object.keys(filters).length === 0 && <span className="text-500">No filters applied</span>}
          </div>
        </div>
        <div className="flex flex-wrap gap-4" style={{ marginTop: "1rem" }}>
          <div className="flex align-items-center gap-2"><Checkbox inputId="bi-rank" checked={includeRank} onChange={event => setIncludeRank(Boolean(event.checked))} /><label htmlFor="bi-rank">Include rank</label></div>
          <div className="flex align-items-center gap-2"><Checkbox inputId="bi-share" checked={includeShare} onChange={event => setIncludeShare(Boolean(event.checked))} /><label htmlFor="bi-share">Share of total</label></div>
          <div className="flex align-items-center gap-2"><Checkbox inputId="bi-total" checked={includeGrandTotal} onChange={event => setIncludeGrandTotal(Boolean(event.checked))} /><label htmlFor="bi-total">Grand total</label></div>
        </div>
        <div className="flex flex-wrap gap-2" style={{ marginTop: "1.25rem" }}>
          <Button label="Run analysis" icon="pi pi-play" onClick={runQuery} disabled={!dimensions.length || !measures.length} loading={loading} />
          <Button label="Reset" icon="pi pi-refresh" severity="secondary" outlined onClick={() => {
            setDimensions([]); setMeasures(["count"]); setFilters({}); setFilterField(""); setFilterValue(""); setFilterOperator("eq");
            setOrderBy("count:desc"); setLimit(100); setIncludeRank(true); setIncludeShare(false); setIncludeGrandTotal(true); setCalculatedMeasures({}); setResult(null); setError(null);
          }} />
          <Button label="Export CSV" icon="pi pi-download" severity="success" outlined disabled={!rows.length}
            onClick={() => downloadCsv(rows, slug + "-bi-" + new Date().toISOString().slice(0, 10) + ".csv")} />
          <Button label="Export Excel" icon="pi pi-file-excel" severity="success" outlined disabled={!rows.length}
            onClick={() => downloadExcel(rows, slug + "-bi-" + new Date().toISOString().slice(0, 10) + ".xls")} />
          <Button label="Print / Save PDF" icon="pi pi-file-pdf" severity="secondary" outlined disabled={!rows.length}
            onClick={() => printPdf(rows, slug + " BI report")} />
        </div>
      </Card>
      <BiWorkspacePanel
        slug={slug}
        credentials={credentials!}
        query={{
          dimensions, measures, filters, orderBy: orderBy || undefined,
          limit: Math.min(1000, Math.max(1, limit)), includeRank,
          includeShareOfTotal: includeShare, includeGrandTotal
        }}
        calculatedMeasures={calculatedMeasures}
        onCalculatedMeasuresChange={setCalculatedMeasures}
      />
      {error && <Message severity="error" text={error} className="mb-3" />}
      {result && <>
        <div className="flex flex-wrap gap-2 align-items-center mb-3">
          <Tag value={(result.rowCount ?? rows.length) + " rows"} severity="info" />
          <Tag value={dimensions.length + " dimensions"} /><Tag value={measures.length + " measures"} />
          {Object.keys(filters).length > 0 && <Tag value={Object.keys(filters).length + " filters"} severity="warning" />}
        </div>
        {result.grandTotal && Object.keys(result.grandTotal).length > 0 && <Card title="Grand total" className="mb-3">
          <div className="flex flex-wrap gap-3">{Object.entries(result.grandTotal).map(([key, value]) => (
            <div key={key} style={{ minWidth: "140px" }}><div className="text-500 text-sm">{key}</div><strong>{value == null ? "—" : String(value)}</strong></div>
          ))}</div>
        </Card>}
        <div className="bi-grid">
          <Card title="Measure comparison">
            {chartData && <Chart type="bar" data={chartData} options={{
              responsive: true, maintainAspectRatio: false,
              plugins: { legend: { position: "bottom" }, tooltip: { mode: "index", intersect: false } },
              scales: { x: { ticks: { autoSkip: true, maxRotation: 45, minRotation: 0 } } }
            }} style={{ height: "360px" }} />}
            <p className="text-500 text-sm">Chart shows the first 20 result groups; table and CSV contain all returned rows.</p>
          </Card>
          <Card title="Grouped result set">
            <DataTable value={rows} stripedRows paginator rows={15} rowsPerPageOptions={[15, 30, 50, 100]} scrollable scrollHeight="560px" size="small" emptyMessage="No groups matched this query.">
              {columns.map(column => <Column key={column} field={column} header={column} sortable />)}
            </DataTable>
          </Card>
        </div>
      </>}
    </>
  );
}
