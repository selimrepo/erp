import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Card } from "primereact/card";
import { InputText } from "primereact/inputtext";
import { useMemo, useState } from "react";
import { ProgressSpinner } from "primereact/progressspinner";
import { useAuth } from "../auth";
import { api } from "../api";

export function ResourcesPage() {
  const { credentials } = useAuth();
  const [filter, setFilter] = useState("");
  const query = useQuery({ queryKey: ["resources"], queryFn: () => api.resources(credentials!), enabled: !!credentials });
  const resources = useMemo(() => (query.data ?? []).filter(r => `${r.module} ${r.entity} ${r.slug}`.toLowerCase().includes(filter.toLowerCase())), [query.data, filter]);

  if (query.isPending) return <ProgressSpinner />;
  return <>
    <div className="section-heading"><div><div className="eyebrow">DATA MODEL</div><h2>Resources</h2><p>Every authorized ERP repository exposed through the unified API.</p></div><span className="p-input-icon-left"><i className="pi pi-search" /><InputText placeholder="Search resources" value={filter} onChange={e=>setFilter(e.target.value)} /></span></div>
    <div className="resource-grid">{resources.map(r =>
      <Card key={r.slug} className="resource-card">
        <div className="resource-icon"><i className="pi pi-table" /></div>
        <span className="module-label">{r.module}</span><h3>{r.entity || r.name}</h3><code>{r.slug}</code>
        <div className="resource-actions"><Link className="p-button p-component" to={`/resources/${r.slug}`}>Browse</Link><Link className="p-button p-component p-button-outlined" to={`/bi/${r.slug}`}>BI</Link></div>
      </Card>
    )}</div>
  </>;
}
