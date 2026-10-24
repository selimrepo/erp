import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Card } from "primereact/card";
import { InputText } from "primereact/inputtext";
import { Button } from "primereact/button";
import { Message } from "primereact/message";
import { Tag } from "primereact/tag";
import type { Credentials } from "../api";
import { api } from "../api";

type Props = {
  slug: string;
  credentials: Credentials;
  query: Record<string, unknown>;
  calculatedMeasures: Record<string, string>;
  onCalculatedMeasuresChange: (measures: Record<string, string>) => void;
};

function splitValues(value: string) {
  return [...new Set(value.split(",").map(item => item.trim()).filter(Boolean))];
}

export function BiWorkspacePanel({ slug, credentials, query, calculatedMeasures, onCalculatedMeasuresChange }: Props) {
  const queryClient = useQueryClient();
  const [measureName, setMeasureName] = useState("");
  const [formula, setFormula] = useState("");
  const [title, setTitle] = useState(slug + " report");
  const [sharedUsers, setSharedUsers] = useState("");
  const [sharedRoles, setSharedRoles] = useState("");
  const [interval, setInterval] = useState("");
  const [emails, setEmails] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [executionId, setExecutionId] = useState("");
  const [editingReportId, setEditingReportId] = useState<string | null>(null);
  const [editingQuery, setEditingQuery] = useState<Record<string, unknown> | null>(null);
  const monitoring = useQuery({ queryKey: ["bi-workspace-monitoring"], queryFn: () => api.biWorkspaceMonitoring(credentials), enabled: false });

  const reports = useQuery({
    queryKey: ["bi-workspace-reports"],
    queryFn: () => api.biWorkspaceReports(credentials),
    enabled: !!credentials
  });
  const execution = useQuery({
    queryKey: ["bi-workspace-execution", executionId],
    queryFn: () => api.biWorkspaceExecution(executionId, credentials),
    enabled: !!executionId,
    refetchInterval: q => q.state.data?.status === "QUEUED" || q.state.data?.status === "RUNNING" ? 2000 : false
  });

  function addMeasure() {
    const name = measureName.trim();
    if (!/^[A-Za-z_][A-Za-z0-9_]{0,63}$/.test(name) || !formula.trim()) {
      setError("Use a valid measure name and a non-empty formula.");
      return;
    }
    onCalculatedMeasuresChange({ ...calculatedMeasures, [name]: formula.trim() });
    setMeasureName("");
    setFormula("");
    setError("");
  }

  async function saveReport() {
    setError("");
    setMessage("");
    try {
      const payload = {
        title,
        resourceSlug: slug,
        query: { ...(editingReportId && editingQuery ? editingQuery : query), calculatedMeasures },
        sharedWith: splitValues(sharedUsers),
        sharedRoles: splitValues(sharedRoles),
        scheduleIntervalMinutes: interval ? Number(interval) : null,
        scheduleEmails: splitValues(emails),
        enabled: Boolean(interval)
      };
      if (editingReportId) await api.updateBiWorkspaceReport(editingReportId, payload, credentials);
      else await api.saveBiWorkspaceReport(payload, credentials);
      setMessage(editingReportId ? "Report updated." : "Report saved. Sharing and schedule settings have been stored.");
      setEditingReportId(null);
      setEditingQuery(null);
      await queryClient.invalidateQueries({ queryKey: ["bi-workspace-reports"] });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Unable to save report");
    }
  }

  function editReport(report: any) {
    setEditingReportId(report.id);
    setTitle(report.title ?? "");
    setSharedUsers((report.sharedWith ?? []).join(", "));
    setSharedRoles((report.sharedRoles ?? []).join(", "));
    setInterval(report.scheduleIntervalMinutes == null ? "" : String(report.scheduleIntervalMinutes));
    setEmails((report.scheduleEmails ?? []).join(", "));
    const savedQuery = report.query && typeof report.query === "object" ? report.query : {};
    setEditingQuery(savedQuery as Record<string, unknown>);
    const savedMeasures = savedQuery.calculatedMeasures && typeof savedQuery.calculatedMeasures === "object" ? savedQuery.calculatedMeasures : {};
    onCalculatedMeasuresChange(savedMeasures as Record<string, string>);
    setError("");
    setMessage("Editing saved report. Save changes to update it.");
  }

  async function deleteReport(id: string) {
    setError("");
    setMessage("");
    try {
      await api.deleteBiWorkspaceReport(id, credentials);
      if (editingReportId === id) { setEditingReportId(null); setEditingQuery(null); }
      setMessage("Saved report deleted.");
      await queryClient.invalidateQueries({ queryKey: ["bi-workspace-reports"] });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Unable to delete report");
    }
  }

  async function executeReport(id: string) {
    setError("");
    setMessage("");
    try {
      const executionResult = await api.executeBiWorkspaceReport(id, credentials);
      setExecutionId(executionResult.id);
      setMessage("Execution queued. Status will update automatically.");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Unable to queue report");
    }
  }

  return (
    <>
      <Card title="Calculated measures" className="mb-3">
        <p className="text-500 text-sm">
          Safe arithmetic only. Reference existing result columns with braces, for example
          {" "}<code>{"{sum:amount}"}</code>. Time functions use the returned row order; sort by a date dimension for period calculations.
        </p>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: ".75rem", alignItems: "end" }}>
          <label className="flex flex-column gap-2"><span>Measure name</span>
            <InputText value={measureName} onChange={e => setMeasureName(e.target.value)} placeholder="grossMarginPct" />
          </label>
          <label className="flex flex-column gap-2"><span>Formula</span>
            <InputText value={formula} onChange={e => setFormula(e.target.value)} placeholder="({sum:revenue}-{sum:cost})/{sum:revenue}*100" />
          </label>
          <Button label="Add measure" icon="pi pi-plus" onClick={addMeasure} />
        </div>
        <p className="text-500 text-sm">
          Operators: + - * / % and parentheses. Time functions: RUNNING_SUM({"{"}sum:amount{"}"}), PREVIOUS({"{"}sum:amount{"}"}), PERIOD_CHANGE({"{"}sum:amount{"}"}), PERIOD_CHANGE_PCT({"{"}sum:amount{"}"}).
        </p>
        <div className="flex flex-column gap-2">
          {Object.entries(calculatedMeasures).map(([name, expression]) => (
            <div key={name} className="flex flex-wrap align-items-center gap-2">
              <Tag value={name} severity="info" />
              <code>{expression}</code>
              <Button icon="pi pi-times" text severity="danger" aria-label={"Remove " + name}
                onClick={() => {
                  const next = { ...calculatedMeasures };
                  delete next[name];
                  onCalculatedMeasuresChange(next);
                }} />
            </div>
          ))}
          {Object.keys(calculatedMeasures).length === 0 && <span className="text-500 text-sm">No calculated measures defined.</span>}
        </div>
      </Card>

      <Card title="Saved reports, sharing and scheduled delivery" className="mb-3">
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: ".75rem" }}>
          <label className="flex flex-column gap-2"><span>Report title</span>
            <InputText value={title} onChange={e => setTitle(e.target.value)} />
          </label>
          <label className="flex flex-column gap-2"><span>Share with usernames (comma-separated)</span>
            <InputText value={sharedUsers} onChange={e => setSharedUsers(e.target.value)} placeholder="analyst1, manager1" />
          </label>
          <label className="flex flex-column gap-2"><span>Share with roles (comma-separated)</span>
            <InputText value={sharedRoles} onChange={e => setSharedRoles(e.target.value)} placeholder="ANALYST, MANAGER" />
          </label>
          <label className="flex flex-column gap-2"><span>Delivery interval in minutes</span>
            <InputText inputMode="numeric" value={interval} onChange={e => setInterval(e.target.value.replace(/[^0-9]/g, ""))} placeholder="Optional, minimum 15" />
          </label>
          <label className="flex flex-column gap-2"><span>Delivery emails (comma-separated)</span>
            <InputText value={emails} onChange={e => setEmails(e.target.value)} placeholder="team@example.com" />
          </label>
        </div>
        <div className="flex flex-wrap gap-2 mt-3">
          <Button label={editingReportId ? "Update report" : "Save current report"} icon="pi pi-save" onClick={saveReport} disabled={!title.trim()} />
          {editingReportId && <Button label="Cancel edit" icon="pi pi-times" severity="secondary" outlined onClick={() => { setEditingReportId(null); setEditingQuery(null); setTitle(slug + " report"); setMessage(""); }} />}
          <Button label="Load monitoring" icon="pi pi-chart-bar" severity="secondary" outlined onClick={() => monitoring.refetch()} />
          <Button label="Refresh saved reports" icon="pi pi-refresh" severity="secondary" outlined
            onClick={() => queryClient.invalidateQueries({ queryKey: ["bi-workspace-reports"] })} />
        </div>
        {message && <Message severity="success" text={message} className="mt-3" />}
        {error && <Message severity="error" text={error} className="mt-3" />}
        {reports.isError && <Message severity="warn" text={reports.error.message} className="mt-3" />}
        <div className="flex flex-column gap-2 mt-3">
          {(reports.data ?? []).filter((report: any) => report.resourceSlug === slug).map((report: any) => (
            <div key={report.id} className="flex flex-wrap align-items-center justify-content-between gap-2 p-2 border-1 surface-border border-round">
              <div>
                <strong>{report.title}</strong><div className="text-500 text-sm">{report.resourceSlug} · owner {report.owner}
                  {report.scheduleIntervalMinutes ? " · every " + report.scheduleIntervalMinutes + " min" : ""}</div>
              </div>
              <div className="flex flex-wrap gap-2"><Button label="Edit" icon="pi pi-pencil" size="small" severity="secondary" outlined onClick={() => editReport(report)} /><Button label="Queue run" icon="pi pi-play" size="small" onClick={() => executeReport(report.id)} /><Button label="Delete" icon="pi pi-trash" size="small" severity="danger" outlined onClick={() => deleteReport(report.id)} /></div>
            </div>
          ))}
          {!reports.isPending && (reports.data ?? []).filter((report: any) => report.resourceSlug === slug).length === 0 && <span className="text-500 text-sm">No saved reports for this resource yet.</span>}
        {monitoring.data && <Card title="BI workspace monitoring" className="mt-3"><pre style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>{JSON.stringify(monitoring.data, null, 2)}</pre></Card>}
        {monitoring.isError && <Message severity="warn" text={monitoring.error.message} className="mt-3" />}
        </div>
        {execution.data && <Message severity={execution.data.status === "FAILED" ? "error" : "info"}
          text={"Execution " + execution.data.id + ": " + execution.data.status
            + (execution.data.error ? " — " + execution.data.error : "")} className="mt-3" />}
      </Card>
    </>
  );
}
