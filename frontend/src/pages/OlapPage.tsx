import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Dropdown } from "primereact/dropdown";
import { MultiSelect } from "primereact/multiselect";
import { InputText } from "primereact/inputtext";
import { InputNumber } from "primereact/inputnumber";
import { Checkbox } from "primereact/checkbox";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Message } from "primereact/message";
import { ProgressSpinner } from "primereact/progressspinner";
import { Tag } from "primereact/tag";
import { TabView, TabPanel } from "primereact/tabview";
import { useAuth } from "../auth";
import { api } from "../api";
import type {
  AutoOlapCatalog,
  AutoOlapCube,
  AutoOlapRelationDiscovery
} from "../types";

type Row = Record<string, unknown>;

type QueryResult = {
  rows?: Row[];
  data?: Row[];
  cells?: Row[];
  grandTotal?: Row;
};

type FilterOperator =
  | "eq"
  | "ne"
  | "like"
  | "gt"
  | "gte"
  | "lt"
  | "lte"
  | "isnull"
  | "notnull";

const FILTER_OPERATORS: Array<{ label: string; value: FilterOperator }> = [
  { label: "Equals", value: "eq" },
  { label: "Not equal", value: "ne" },
  { label: "Contains", value: "like" },
  { label: "Greater than", value: "gt" },
  { label: "Greater or equal", value: "gte" },
  { label: "Less than", value: "lt" },
  { label: "Less or equal", value: "lte" },
  { label: "Is null", value: "isnull" },
  { label: "Is not null", value: "notnull" }
];

function rowsOf(result: QueryResult | null): Row[] {
  return result?.rows ?? result?.data ?? result?.cells ?? [];
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

function options(values: string[]) {
  return values.map((value) => ({ label: value, value }));
}

function SelectionSummary({
  values,
  label,
  onClear
}: {
  values: string[];
  label: string;
  onClear: () => void;
}) {
  if (values.length === 0) {
    return <span className="text-500">None selected</span>;
  }

  return (
    <div className="olap-selection-summary">
      <div className="flex align-items-center gap-2">
        <Tag value={`${values.length} ${label}`} severity="info" />
        <Button
          label="Clear"
          icon="pi pi-times"
          text
          size="small"
          onClick={onClear}
        />
      </div>
      <div
        className="olap-selection-list"
        style={{
          maxHeight: "180px",
          overflowY: "auto",
          overflowX: "hidden"
        }}
      >
        {values.slice(0, 20).map((value) => (
          <span key={value} className="olap-selection-item">
            {value}
          </span>
        ))}
        {values.length > 20 && (
          <span className="olap-selection-item text-500">
            + {values.length - 20} more selected
          </span>
        )}
      </div>
    </div>
  );
}

function FieldSelector({
  value,
  optionValues,
  placeholder,
  summaryLabel,
  onChange,
  onClear
}: {
  value: string[];
  optionValues: string[];
  placeholder: string;
  summaryLabel: string;
  onChange: (values: string[]) => void;
  onClear: () => void;
}) {
  return (
    <>
      <MultiSelect
        value={value}
        options={options(optionValues)}
        onChange={(event) => onChange((event.value ?? []) as string[])}
        placeholder={placeholder}
        display="comma"
        maxSelectedLabels={1}
        selectedItemsLabel="{0} selected"
        filter
        showSelectAll
        virtualScrollerOptions={{ itemSize: 40 }}
        scrollHeight="320px"
        panelClassName="olap-field-selector-panel"
        className="field-control"
      />
      <SelectionSummary
        values={value}
        label={summaryLabel}
        onClear={onClear}
      />
    </>
  );
}

export function OlapPage() {
  const { credentials } = useAuth();

  const [cubeName, setCubeName] = useState("");
  const [dimensions, setDimensions] = useState<string[]>([]);
  const [measures, setMeasures] = useState<string[]>([]);

  const [filterField, setFilterField] = useState("");
  const [filterOperator, setFilterOperator] =
    useState<FilterOperator>("eq");
  const [filterValue, setFilterValue] = useState("");
  const [filters, setFilters] = useState<Record<string, string>>({});

  const [orderBy, setOrderBy] = useState("");
  const [limit, setLimit] = useState(100);
  const [includeTotals, setIncludeTotals] = useState(true);
  const [includeRollup, setIncludeRollup] = useState(false);
  const [pivotDimension, setPivotDimension] = useState("");
  const [result, setResult] = useState<QueryResult | null>(null);
  const [queryError, setQueryError] = useState("");

  const [reportDepth, setReportDepth] = useState(2);
  const [reportName, setReportName] = useState("");
  const [reportRelations, setReportRelations] = useState<string[]>([]);
  const [reportDimensions, setReportDimensions] = useState<string[]>([]);
  const [reportMeasure, setReportMeasure] = useState("");
  const [reportResult, setReportResult] = useState<QueryResult | null>(null);
  const [reportError, setReportError] = useState("");

  const catalog = useQuery<AutoOlapCatalog>({
    queryKey: ["auto-olap-cubes", credentials?.username, reportDepth],
    queryFn: () => api.autoOlapCubes(credentials!, reportDepth),
    enabled: Boolean(credentials),
    staleTime: 30_000
  });

  const cubes = catalog.data?.cubes ?? [];
  const cube = cubes.find((item) => item.name === cubeName);

  const relationQueries = useQuery<AutoOlapRelationDiscovery[]>({
    queryKey: [
      "auto-olap-report-relations",
      credentials?.username,
      cubeName,
      reportRelations,
      reportDepth
    ],
    queryFn: async () =>
      Promise.all(
        reportRelations.map((relation) =>
          api.autoOlapRelations(
            cubeName,
            relation,
            credentials!,
            reportDepth
          )
        )
      ),
    enabled:
      Boolean(credentials) &&
      Boolean(cubeName) &&
      reportRelations.length > 0 &&
      reportDepth > 0,
    staleTime: 30_000
  });

  const dimensionOptions = useMemo(
    () => cube?.dimensions ?? [],
    [cube]
  );

  const measureOptions = useMemo(
    () => cube?.measures ?? [],
    [cube]
  );

  const reportDimensionOptions = useMemo(() => {
    if (!cube) return [];

    if (reportRelations.length === 0) {
      return cube.dimensions;
    }

    return cube.dimensions.filter((field) =>
      reportRelations.some(
        (relation) =>
          field === relation || field.startsWith(relation + ".")
      )
    );
  }, [cube, reportRelations]);

  const rows = rowsOf(result);
  const reportRows = rowsOf(reportResult);
  const columns = rows.length ? Object.keys(rows[0]) : [];
  const reportColumns = reportRows.length
    ? Object.keys(reportRows[0])
    : [];

  function resetQuery() {
    setDimensions([]);
    setMeasures([]);
    setFilterField("");
    setFilterOperator("eq");
    setFilterValue("");
    setFilters({});
    setOrderBy("");
    setLimit(100);
    setIncludeTotals(true);
    setIncludeRollup(false);
    setPivotDimension("");
    setResult(null);
    setQueryError("");
  }

  function selectCube(value: string | null) {
    const name = value ?? "";
    const selected = cubes.find((item) => item.name === name);

    setCubeName(name);
    setDimensions(selected?.dimensions.slice(0, 2) ?? []);
    setMeasures(
      selected
        ? selected.measures.includes("count")
          ? ["count"]
          : selected.measures.slice(0, 1)
        : []
    );

    const defaultMeasure =
      selected?.measures.find((measure) => measure.startsWith("sum:")) ??
      selected?.measures[0] ??
      "";

    setReportMeasure(defaultMeasure);
    setReportRelations([]);
    setReportDimensions([]);
    setReportName("");
    setReportResult(null);
    setReportError("");
    resetQuery();
  }

  function addFilter() {
    if (!filterField) return;

    const needsValue =
      filterOperator !== "isnull" && filterOperator !== "notnull";

    if (needsValue && !filterValue.trim()) return;

    const key = `${filterField}:${filterOperator}`;

    setFilters((previous) => ({
      ...previous,
      [key]: needsValue ? filterValue : ""
    }));

    setFilterField("");
    setFilterValue("");
  }

  function removeFilter(key: string) {
    setFilters((previous) => {
      const next = { ...previous };
      delete next[key];
      return next;
    });
  }

  async function runQuery() {
    if (
      !credentials ||
      !cube ||
      dimensions.length === 0 ||
      measures.length === 0
    ) {
      return;
    }

    setQueryError("");
    setResult(null);

    try {
      const response = await api.autoOlapQuery(
        cube.name,
        {
          dimensions,
          measures,
          filters,
          orderBy: orderBy || undefined,
          limit,
          includeTotals,
          includeRollup,
          pivotDimension: pivotDimension || undefined
        },
        credentials
      );

      setResult(response as QueryResult);
    } catch (error) {
      setQueryError(errorMessage(error));
    }
  }

  async function generateReport() {
    if (
      !credentials ||
      !cube ||
      reportDimensions.length === 0 ||
      !reportMeasure
    ) {
      return;
    }

    setReportError("");
    setReportResult(null);

    try {
      const response = await api.autoOlapQuery(
        cube.name,
        {
          dimensions: reportDimensions,
          measures: [reportMeasure],
          limit: 1000,
          includeTotals: true,
          includeRollup: false
        },
        credentials
      );

      setReportResult(response as QueryResult);
    } catch (error) {
      setReportError(errorMessage(error));
    }
  }

  if (catalog.isPending) {
    return (
      <div className="flex justify-content-center p-4">
        <ProgressSpinner />
      </div>
    );
  }

  if (catalog.isError) {
    return <Message severity="error" text={errorMessage(catalog.error)} />;
  }

  return (
    <div
      className="olap-page"
      style={{
        width: "100%",
        maxWidth: "100%",
        minWidth: 0,
        maxHeight: "calc(100vh - 24px)",
        overflowY: "auto",
        overflowX: "hidden",
        boxSizing: "border-box",
        paddingBottom: "2rem"
      }}
    >
      <div className="section-heading olap-header">
        <div>
          <div className="eyebrow">OLAP / CUBES</div>
          <h2>OLAP Cube Explorer</h2>
          <p>
            Build high-dimensional queries without letting large selections
            overwhelm the workspace.
          </p>
        </div>
        <div className="flex align-items-center gap-2">
          <Tag value={`${cubes.length} cubes`} icon="pi pi-database" />
          {cube && (
            <Tag
              value={`${dimensions.length}D · ${measures.length}M`}
              severity="success"
            />
          )}
        </div>
      </div>

      <TabView>
        <TabPanel header="Query Builder" leftIcon="pi pi-sliders-h">
          <div className="olap-workspace">
            <Card title="1. Select cube">
              <div className="olap-cube-picker">
                <label>
                  Cube
                  <Dropdown
                    value={cubeName}
                    options={options(cubes.map((item) => item.name))}
                    onChange={(event) =>
                      selectCube(event.value as string | null)
                    }
                    placeholder="Select a cube"
                    filter
                    showClear
                    className="field-control"
                  />
                </label>
                {cube && (
                  <div className="olap-cube-info">
                    <span>
                      <b>{cube.entity}</b> · {cube.module}
                    </span>
                    <span>
                      {cube.dimensions.length} dimensions ·{" "}
                      {cube.measures.length} measures ·{" "}
                      {cube.relationships.length} relationships
                    </span>
                  </div>
                )}
              </div>
            </Card>

            <div className="olap-builder-grid">
              <Card title="2. Dimensions">
                {cube ? (
                  <FieldSelector
                    value={dimensions}
                    optionValues={dimensionOptions}
                    placeholder="Search and select dimensions"
                    summaryLabel="dimensions"
                    onChange={setDimensions}
                    onClear={() => setDimensions([])}
                  />
                ) : (
                  <p className="text-500">Select a cube first.</p>
                )}
              </Card>

              <Card title="3. Measures">
                {cube ? (
                  <FieldSelector
                    value={measures}
                    optionValues={measureOptions}
                    placeholder="Search and select measures"
                    summaryLabel="measures"
                    onChange={setMeasures}
                    onClear={() => setMeasures([])}
                  />
                ) : (
                  <p className="text-500">Select a cube first.</p>
                )}
              </Card>
            </div>

            <Card title="4. Filters">
              <div className="olap-filter-row">
                <label>
                  Field
                  <Dropdown
                    value={filterField}
                    options={options(dimensionOptions)}
                    onChange={(event) =>
                      setFilterField((event.value as string | null) ?? "")
                    }
                    placeholder="Dimension"
                    filter
                    showClear
                    className="field-control"
                  />
                </label>

                <label>
                  Operator
                  <Dropdown
                    value={filterOperator}
                    options={FILTER_OPERATORS}
                    onChange={(event) =>
                      setFilterOperator(event.value as FilterOperator)
                    }
                    className="field-control"
                  />
                </label>

                <label>
                  Value
                  <InputText
                    value={filterValue}
                    onChange={(event) => setFilterValue(event.target.value)}
                    disabled={
                      filterOperator === "isnull" ||
                      filterOperator === "notnull"
                    }
                  />
                </label>

                <Button
                  label="Add filter"
                  icon="pi pi-plus"
                  outlined
                  onClick={addFilter}
                  disabled={
                    !filterField ||
                    (filterOperator !== "isnull" &&
                      filterOperator !== "notnull" &&
                      !filterValue.trim())
                  }
                />
              </div>

              {Object.entries(filters).length > 0 && (
                <div className="olap-filter-list">
                  <div className="flex align-items-center justify-content-between mb-2">
                    <span className="font-medium">
                      {Object.keys(filters).length} active filters
                    </span>
                    <Button
                      label="Clear all"
                      icon="pi pi-trash"
                      text
                      size="small"
                      onClick={() => setFilters({})}
                    />
                  </div>
                  <div
              className="olap-scroll-selection"
              style={{
                maxHeight: "220px",
                overflowY: "auto",
                overflowX: "hidden"
              }}
            >
                    {Object.entries(filters).map(([key, value]) => (
                      <div key={key} className="olap-filter-item">
                        <span>
                          <b>{key}</b>
                          {value && <> = {value}</>}
                        </span>
                        <Button
                          icon="pi pi-times"
                          text
                          rounded
                          size="small"
                          severity="secondary"
                          aria-label={`Remove filter ${key}`}
                          onClick={() => removeFilter(key)}
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </Card>

            <Card title="5. Query options">
              <div className="olap-options-grid">
                <label>
                  Order by
                  <Dropdown
                    value={orderBy}
                    options={options(measures)}
                    onChange={(event) =>
                      setOrderBy((event.value as string | null) ?? "")
                    }
                    placeholder="Measure"
                    showClear
                    className="field-control"
                  />
                </label>

                <label>
                  Limit
                  <InputNumber
                    value={limit}
                    onValueChange={(event) => {
                      const value = event.value ?? 100;
                      setLimit(Math.min(1000, Math.max(1, value)));
                    }}
                    min={1}
                    max={1000}
                  />
                </label>

                <label>
                  Pivot dimension
                  <Dropdown
                    value={pivotDimension}
                    options={options(dimensions)}
                    onChange={(event) =>
                      setPivotDimension((event.value as string | null) ?? "")
                    }
                    placeholder="None"
                    showClear
                    className="field-control"
                  />
                </label>

                <div className="olap-options">
                  <div>
                    <Checkbox
                      inputId="olap-totals"
                      checked={includeTotals}
                      onChange={(event) =>
                        setIncludeTotals(Boolean(event.checked))
                      }
                    />
                    <label htmlFor="olap-totals">Grand totals</label>
                  </div>
                  <div>
                    <Checkbox
                      inputId="olap-rollup"
                      checked={includeRollup}
                      onChange={(event) =>
                        setIncludeRollup(Boolean(event.checked))
                      }
                    />
                    <label htmlFor="olap-rollup">Rollup rows</label>
                  </div>
                </div>
              </div>
            </Card>

            <Card className="olap-run-card">
              <div className="flex align-items-center justify-content-between gap-3 flex-wrap">
                <div>
                  <div className="font-semibold">
                    {dimensions.length} dimensions · {measures.length} measures ·{" "}
                    {Object.keys(filters).length} filters
                  </div>
                  <small className="text-500">
                    Large selections stay inside bounded scroll areas.
                  </small>
                </div>
                <Button
                  label="Run Cube Query"
                  icon="pi pi-play"
                  size="large"
                  onClick={runQuery}
                  disabled={
                    !cube ||
                    dimensions.length === 0 ||
                    measures.length === 0
                  }
                />
              </div>
            </Card>
          </div>

          {queryError && (
            <Message severity="error" text={queryError} className="mt-3" />
          )}

          {result && (
            <Card title="Query Result" className="mt-3">
              <div className="olap-result-meta">
                <span>{rows.length} rows</span>
                {result.grandTotal && (
                  <Tag value="Grand total included" severity="success" />
                )}
              </div>
              {rows.length > 0 ? (
                <DataTable
                  value={rows}
                  paginator
                  rows={20}
                  scrollable
                  size="small"
                  stripedRows
                >
                  {columns.map((column) => (
                    <Column key={column} field={column} header={column} sortable />
                  ))}
                </DataTable>
              ) : (
                <Message severity="info" text="The query returned no rows." />
              )}
            </Card>
          )}
        </TabPanel>

        <TabPanel header="Cube Catalog" leftIcon="pi pi-th-large">
          <Card title={`${cubes.length} available cubes`}>
            <DataTable
              value={cubes}
              paginator
              rows={15}
              stripedRows
              responsiveLayout="scroll"
            >
              <Column field="name" header="Cube" sortable />
              <Column field="module" header="Module" sortable />
              <Column field="entity" header="Entity" sortable />
              <Column
                field="dimensions"
                header="Dimensions"
                body={(row: AutoOlapCube) => row.dimensions.length}
              />
              <Column
                field="measures"
                header="Measures"
                body={(row: AutoOlapCube) => row.measures.length}
              />
              <Column
                field="relationships"
                header="Relationships"
                body={(row: AutoOlapCube) => row.relationships.length}
              />
            </DataTable>
          </Card>
        </TabPanel>

        <TabPanel header="Auto Reports" leftIcon="pi pi-chart-bar">
          <div className="olap-workspace">
            <Card title="1. Report source">
              <div className="olap-options-grid">
                <label>
                  Fact cube
                  <Dropdown
                    value={cubeName}
                    options={options(cubes.map((item) => item.name))}
                    onChange={(event) =>
                      selectCube(event.value as string | null)
                    }
                    placeholder="Select a fact cube"
                    filter
                    showClear
                    className="field-control"
                  />
                </label>

                <label>
                  Discovery depth
                  <InputNumber
                    value={reportDepth}
                    onValueChange={(event) => {
                      const value = event.value ?? 2;
                      setReportDepth(Math.min(6, Math.max(1, value)));
                    }}
                    min={1}
                    max={6}
                  />
                </label>
              </div>

              {cube && (
                <div className="olap-cube-info">
                  <span>
                    <b>{cube.entity}</b> · {cube.module}
                  </span>
                  <span>
                    {cube.relationships.length} relationships available
                  </span>
                </div>
              )}
            </Card>

            <div className="olap-builder-grid">
              <Card title="2. Relationships">
                {cube ? (
                  <FieldSelector
                    value={reportRelations}
                    optionValues={cube.relationships}
                    placeholder="Search relationships"
                    summaryLabel="relationships"
                    onChange={(values) => {
                      setReportRelations(values);
                      setReportDimensions([]);
                      setReportResult(null);
                      setReportError("");
                    }}
                    onClear={() => {
                      setReportRelations([]);
                      setReportDimensions([]);
                    }}
                  />
                ) : (
                  <p className="text-500">Select a fact cube first.</p>
                )}
              </Card>

              <Card title="3. Report dimensions">
                {cube ? (
                  <FieldSelector
                    value={reportDimensions}
                    optionValues={reportDimensionOptions}
                    placeholder="Search report dimensions"
                    summaryLabel="dimensions"
                    onChange={setReportDimensions}
                    onClear={() => setReportDimensions([])}
                  />
                ) : (
                  <p className="text-500">Select a fact cube first.</p>
                )}
              </Card>
            </div>

            {relationQueries.isPending && (
              <div className="flex justify-content-center p-3">
                <ProgressSpinner style={{ width: "28px", height: "28px" }} />
              </div>
            )}

            {relationQueries.isError && (
              <Message severity="error" text={errorMessage(relationQueries.error)} />
            )}

            {relationQueries.data && relationQueries.data.length > 0 && (
              <Card title="Discovered relationship paths">
                <div
                  className="olap-scroll-selection"
                  style={{
                    maxHeight: "240px",
                    overflowY: "auto",
                    overflowX: "hidden"
                  }}
                >
                  {relationQueries.data.map((discovery) => (
                    <div key={discovery.relation} className="olap-relation-item">
                      <b>{discovery.relation}</b>
                      <span>
                        {discovery.paths.length > 0
                          ? discovery.paths
                              .map(
                                (path) =>
                                  `${path.path} → ${path.targetEntity}`
                              )
                              .join(", ")
                          : "No paths discovered"}
                      </span>
                    </div>
                  ))}
                </div>
              </Card>
            )}

            <Card title="4. Report output">
              <div className="olap-options-grid">
                <label>
                  Report name
                  <InputText
                    value={reportName}
                    onChange={(event) => setReportName(event.target.value)}
                    placeholder="Income per product by customer"
                  />
                </label>

                <label>
                  Measure
                  <Dropdown
                    value={reportMeasure}
                    options={options(measureOptions)}
                    onChange={(event) =>
                      setReportMeasure((event.value as string | null) ?? "")
                    }
                    placeholder="Select a measure"
                    filter
                    className="field-control"
                  />
                </label>
              </div>

              <div className="flex align-items-center justify-content-between gap-3 flex-wrap mt-3">
                <div className="font-semibold">
                  {reportDimensions.length} dimensions selected
                </div>
                <Button
                  label="Generate Report"
                  icon="pi pi-chart-bar"
                  size="large"
                  onClick={generateReport}
                  disabled={
                    !cube ||
                    reportDimensions.length === 0 ||
                    !reportMeasure
                  }
                />
              </div>
            </Card>
          </div>

          {reportError && (
            <Message severity="error" text={reportError} className="mt-3" />
          )}

          {reportResult && (
            <Card
              title={
                reportName.trim() ||
                (cube ? `${cube.entity} report` : "OLAP report")
              }
              className="mt-3"
            >
              <div className="olap-result-meta">
                <span>{reportRows.length} rows</span>
                <Tag value={reportMeasure} />
              </div>
              {reportRows.length > 0 ? (
                <DataTable
                  value={reportRows}
                  paginator
                  rows={20}
                  scrollable
                  size="small"
                  stripedRows
                >
                  {reportColumns.map((column) => (
                    <Column key={column} field={column} header={column} sortable />
                  ))}
                </DataTable>
              ) : (
                <Message
                  severity="info"
                  text="The generated report returned no rows."
                />
              )}
            </Card>
          )}
        </TabPanel>
      </TabView>
    </div>
  );
}
