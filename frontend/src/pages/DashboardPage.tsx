import { useQuery } from "@tanstack/react-query";
import { Card } from "primereact/card";
import { ProgressSpinner } from "primereact/progressspinner";
import { Message } from "primereact/message";
import { useAuth } from "../auth";
import { api } from "../api";

export function DashboardPage() {
  const { credentials } = useAuth();
  const query = useQuery({ queryKey: ["dashboard"], queryFn: () => api.dashboard(credentials!), enabled: !!credentials });

  if (query.isPending) return <ProgressSpinner />;
  if (query.isError) return <Message severity="error" text={query.error.message} />;
  const d = query.data;

  return <>
    <div className="section-heading"><div><div className="eyebrow">EXECUTIVE OVERVIEW</div><h2>System health at a glance</h2></div></div>
    <div className="stat-grid">
      <Stat icon="pi-chart-bar" label="Total records" value={d.totalRecords.toLocaleString()} />
      <Stat icon="pi-database" label="Resources" value={d.resourceCount.toLocaleString()} />
      <Stat icon="pi-sitemap" label="Modules" value={d.moduleCount.toLocaleString()} />
    </div>
    <div className="dashboard-grid">
      <Card title="Records by module">
        {Object.entries(d.recordsByModule).sort((a,b)=>b[1]-a[1]).map(([name,count]) =>
          <div className="bar-row" key={name}><span>{name}</span><strong>{count.toLocaleString()}</strong><div className="bar"><i style={{width: `${Math.min(100, count / Math.max(...Object.values(d.recordsByModule),1) * 100)}%`}} /></div></div>
        )}
      </Card>
      <Card title="Largest resources">
        {Object.entries(d.recordsByResource).sort((a,b)=>b[1]-a[1]).slice(0,8).map(([name,count]) =>
          <div className="list-row" key={name}><span>{name}</span><strong>{count.toLocaleString()}</strong></div>
        )}
      </Card>
    </div>
  </>;
}

function Stat({ icon, label, value }: { icon: string; label: string; value: string }) {
  return <Card className="stat-card"><i className={`pi ${icon}`} /><span>{label}</span><strong>{value}</strong></Card>;
}
