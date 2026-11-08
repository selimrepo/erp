import { useMemo, useState } from "react";
import { QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import { AuthProvider, useAuth } from "../auth";
import { api } from "../api";
import { LoginPage } from "../pages/LoginPage";

type ModuleConfig = { id: string; title: string; description: string; match: string[] };
const queryClient = new QueryClient();

export function ModuleApp({ config }: { config: ModuleConfig }) {
  return <QueryClientProvider client={queryClient}><AuthProvider><ModuleWorkspace config={config} /></AuthProvider></QueryClientProvider>;
}

function ModuleWorkspace({ config }: { config: ModuleConfig }) {
  const { credentials, logout } = useAuth();
  if (!credentials) return <LoginPage />;
  return <Workspace config={config} credentials={credentials} onLogout={logout} />;
}

function Workspace({ config, credentials, onLogout }: { config: ModuleConfig; credentials: {username:string;password:string}; onLogout:()=>void }) {
  const [selected, setSelected] = useState("");
  const resourcesQuery = useQuery({ queryKey: ["module-resources", config.id], queryFn: () => api.resources(credentials) });
  const dashboardQuery = useQuery({ queryKey: ["module-dashboard"], queryFn: () => api.dashboard(credentials) });
  const resources = useMemo(() => (resourcesQuery.data ?? []).filter(r => {
    const value = `${r.module} ${r.entity} ${r.slug}`.toLowerCase();
    return config.match.some(token => value.includes(token));
  }), [resourcesQuery.data, config]);
  const activeSlug = resources.some(r => r.slug === selected) ? selected : resources[0]?.slug ?? "";
  const rowsQuery = useQuery({ queryKey: ["module-rows", activeSlug], queryFn: () => api.rows(activeSlug, 0, 50, credentials), enabled: !!activeSlug });
  const rows = rowsQuery.data?.content ?? [];
  const columns = rows.length ? Object.keys(rows[0].values ?? rows[0]).filter(k => k !== "values").slice(0, 7) : [];

  return <main style={{minHeight:"100vh",background:"#f4f6fa",color:"#172033",fontFamily:"Inter,system-ui,sans-serif"}}>
    <header style={{background:"#111c32",color:"white",padding:"18px 28px",display:"flex",alignItems:"center",justifyContent:"space-between"}}>
      <div><div style={{fontSize:11,letterSpacing:2,opacity:.7}}>ERPLANCERS · INDEPENDENT MODULE</div><h1 style={{margin:"5px 0",fontSize:24}}>{config.title}</h1><div style={{opacity:.75,fontSize:13}}>{config.description}</div></div>
      <button onClick={onLogout} style={buttonStyle}>Sign out · {credentials.username}</button>
    </header>
    <section style={{padding:24,maxWidth:1440,margin:"auto"}}>
      <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(180px,1fr))",gap:14,marginBottom:22}}>
        <Metric label="Module resources" value={String(resources.length)} />
        <Metric label="ERP total records" value={(dashboardQuery.data?.totalRecords ?? 0).toLocaleString()} />
        <Metric label="ERP modules" value={String(dashboardQuery.data?.moduleCount ?? 0)} />
      </div>
      <div style={{display:"grid",gridTemplateColumns:"minmax(220px,280px) 1fr",gap:18,alignItems:"start"}}>
        <aside style={panelStyle}><h2 style={headingStyle}>Resources</h2>{resources.map(r=><button key={r.slug} onClick={()=>setSelected(r.slug)} style={{...resourceButton,background:activeSlug===r.slug?"#e8efff":"white",borderColor:activeSlug===r.slug?"#7c9ce8":"#e3e8f0"}}><strong>{r.entity || r.name}</strong><small>{r.slug}</small></button>)}{resourcesQuery.isError&&<p>{resourcesQuery.error.message}</p>}</aside>
        <section style={panelStyle}><div style={{display:"flex",justifyContent:"space-between",gap:12,alignItems:"center"}}><div><h2 style={headingStyle}>{resources.find(r=>r.slug===activeSlug)?.entity ?? config.title}</h2><p style={{margin:"4px 0 16px",color:"#68758b",fontSize:13}}>Live data from the existing ERP REST API · read-only explorer</p></div><span style={{fontSize:12,color:"#68758b"}}>{rowsQuery.data?.totalElements ?? 0} records</span></div>
          {rowsQuery.isPending?<p>Loading records…</p>:rowsQuery.isError?<p style={{color:"#b42318"}}>{rowsQuery.error.message}</p>:!rows.length?<p>No records found for this module yet.</p>:<div style={{overflowX:"auto"}}><table style={{width:"100%",borderCollapse:"collapse",fontSize:13}}><thead><tr>{columns.map(c=><th key={c} style={cellHead}>{c}</th>)}</tr></thead><tbody>{rows.map((row,i)=>{const data=(row.values ?? row) as Record<string,unknown>;return <tr key={String(row.id ?? i)}>{columns.map(c=><td key={c} style={cellStyle}>{formatValue(data[c])}</td>)}</tr>})}</tbody></table></div>}
        </section>
      </div>
    </section>
  </main>;
}

function Metric({label,value}:{label:string;value:string}){return <div style={{...panelStyle,padding:18}}><div style={{fontSize:12,color:"#68758b"}}>{label}</div><strong style={{fontSize:26,display:"block",marginTop:7}}>{value}</strong></div>}
function formatValue(value:unknown){if(value==null)return "—";if(typeof value==="object")return JSON.stringify(value);return String(value)}
const panelStyle:React.CSSProperties={background:"white",border:"1px solid #e5eaf1",borderRadius:12,padding:18,boxShadow:"0 4px 18px rgba(18,35,64,.04)"};
const headingStyle:React.CSSProperties={fontSize:17,margin:"0 0 14px"};
const buttonStyle:React.CSSProperties={border:0,borderRadius:8,padding:"10px 14px",background:"#293b5d",color:"white",cursor:"pointer"};
const resourceButton:React.CSSProperties={display:"block",width:"100%",textAlign:"left",border:"1px solid",borderRadius:8,padding:11,marginBottom:8,cursor:"pointer",color:"#172033"};
const cellHead:React.CSSProperties={textAlign:"left",padding:10,borderBottom:"1px solid #e5eaf1",color:"#68758b",whiteSpace:"nowrap"};
const cellStyle:React.CSSProperties={padding:10,borderBottom:"1px solid #edf0f5",maxWidth:260,overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"};
