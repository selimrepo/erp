import {useMemo,useState,type FormEvent} from "react";
import {QueryClient,QueryClientProvider,useMutation,useQuery,useQueryClient} from "@tanstack/react-query";
import {AuthProvider,useAuth} from "../auth";
import {api} from "../api";
import {WorkflowPanel} from "./WorkflowPanel";
type Config={id:string;title:string;description:string;match:string[]};
const client=new QueryClient();
export function ModuleApp({config}:{config:Config}){return <QueryClientProvider client={client}><AuthProvider><Workspace config={config}/></AuthProvider></QueryClientProvider>}
function Workspace({config}:{config:Config}){
 const {credentials}=useAuth();
 return credentials ? <AuthenticatedWorkspace config={config} credentials={credentials}/> : <Login config={config}/>;
}
function AuthenticatedWorkspace({config,credentials}:{config:Config;credentials:{username:string;password:string}}){
 const [selected,setSelected]=useState("");const [page,setPage]=useState(0);const [search,setSearch]=useState("");const [draft,setDraft]=useState<{id?:string;values:Record<string,string>}|null>(null);
 const resourcesQ=useQuery({queryKey:["resources"],queryFn:()=>api.resources(credentials)});
 const catalogQ=useQuery({queryKey:["catalog"],queryFn:()=>api.catalog(credentials)});
 const dashboardQ=useQuery({queryKey:["dashboard"],queryFn:()=>api.dashboard(credentials)});
 const resources=useMemo(()=> (resourcesQ.data??[]).filter(r=>config.match.some(m=>`${r.module} ${r.entity} ${r.slug}`.toLowerCase().includes(m))),[resourcesQ.data,config]);
 const slug=resources.some(r=>r.slug===selected)?selected:resources[0]?.slug??"";
 const schema=catalogQ.data?.resources.find(r=>r.slug===slug);
 const rowsQ=useQuery({queryKey:["rows",slug,page],queryFn:()=>api.rows(slug,page,25,credentials),enabled:!!slug});
 const rows=rowsQ.data?.content??[];
 const columns=schema?.fields.map(f=>f.name).filter(n=>n.toLowerCase()!=="password")??(rows[0]?Object.keys(rows[0].values??rows[0]).filter(k=>k!=="values"):[]);
 const qc=useQueryClient();
 const save=useMutation({mutationFn:(v:{id?:string;values:Record<string,string>})=>v.id?api.update(slug,v.id,v.values,credentials):api.create(slug,v.values,credentials),onSuccess:()=>{setDraft(null);qc.invalidateQueries({queryKey:["rows",slug]});qc.invalidateQueries({queryKey:["dashboard"]})}});
 const del=useMutation({mutationFn:(id:string)=>api.remove(slug,id,credentials),onSuccess:()=>{qc.invalidateQueries({queryKey:["rows",slug]});qc.invalidateQueries({queryKey:["dashboard"]})}});
 const openNew=()=>setDraft({values:Object.fromEntries(columns.filter(k=>!["id","createdAt","updatedAt","version"].includes(k)).map(k=>[k,""]))});
 const openEdit=(row:any)=>{const v=row.values??row;setDraft({id:String(row.id??v.id),values:Object.fromEntries(columns.filter(k=>!["id","createdAt","updatedAt","version"].includes(k)).map(k=>[k,v[k]==null?"":typeof v[k]==="object"?JSON.stringify(v[k]):String(v[k])]))})};
 const submit=(e:FormEvent)=>{e.preventDefault();if(draft)save.mutate(draft)};
 const filtered=rows.filter(r=>JSON.stringify(r.values??r).toLowerCase().includes(search.toLowerCase()));
 return <main style={{minHeight:"100vh",background:"#f4f6fa",color:"#172033",fontFamily:"Inter,system-ui,sans-serif"}}>
 <header style={{background:"#111c32",color:"white",padding:"20px 28px",display:"flex",justifyContent:"space-between",gap:16,flexWrap:"wrap"}}><div><small style={{letterSpacing:2,color:"#9aacc9"}}>ERPLANCERS · INDEPENDENT MODULE</small><h1 style={{margin:"5px 0"}}>{config.title}</h1><span>{config.description}</span></div><small>{credentials.username}</small></header>
 <section style={{padding:24,maxWidth:1500,margin:"auto"}}>
 <WorkflowPanel moduleId={config.id} credentials={credentials}/>
 <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(180px,1fr))",gap:12,marginBottom:18}}><Metric label="Module resources" value={resources.length}/><Metric label="ERP records" value={dashboardQ.data?.totalRecords??0}/><Metric label="ERP modules" value={dashboardQ.data?.moduleCount??0}/></div>
 <div style={{display:"grid",gridTemplateColumns:"minmax(220px,280px) minmax(0,1fr)",gap:16,alignItems:"start"}}>
 <aside style={panel}><h3>Resources</h3><input placeholder="Search resources" value={search} onChange={e=>setSearch(e.target.value)} style={input}/>{resources.map(r=><button key={r.slug} onClick={()=>{setSelected(r.slug);setPage(0);setDraft(null)}} style={{...nav,background:slug===r.slug?"#edf3ff":"white"}}><b>{r.entity||r.name}</b><small style={{display:"block",color:"#718096"}}>{r.slug}</small></button>)}</aside>
 <section style={panel}><div style={{display:"flex",justifyContent:"space-between",gap:12,alignItems:"center",flexWrap:"wrap"}}><div><small style={{color:"#718096"}}>{resources.find(r=>r.slug===slug)?.module??config.id}</small><h2 style={{margin:"4px 0"}}>{resources.find(r=>r.slug===slug)?.entity??config.title}</h2><small style={{color:"#718096"}}>{rowsQ.data?.totalElements??0} records</small></div><button style={primary} disabled={!slug||!columns.length} onClick={openNew}>＋ New record</button></div>
 {draft&&<form onSubmit={submit} style={{background:"#f8faff",padding:14,borderRadius:10,margin:"16px 0"}}><div style={{display:"flex",justifyContent:"space-between"}}><b>{draft.id?"Edit record":"Create record"}</b><button type="button" onClick={()=>setDraft(null)}>Close</button></div><div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(190px,1fr))",gap:10,marginTop:12}}>{Object.entries(draft.values).map(([k,v])=><label key={k} style={{fontSize:12}}>{k}<input style={input} value={v} onChange={e=>setDraft(d=>d?{...d,values:{...d.values,[k]:e.target.value}}:d)}/></label>)}</div><button style={{...primary,marginTop:12}} disabled={save.isPending}>{save.isPending?"Saving…":"Save"}</button>{save.isError&&<Error message={(save.error as Error).message}/>}</form>}
 {rowsQ.isPending?<p>Loading…</p>:rowsQ.isError?<Error message={(rowsQ.error as Error).message}/>:!filtered.length?<p>No records found.</p>:<div style={{overflowX:"auto",marginTop:14}}><table style={{width:"100%",borderCollapse:"collapse",fontSize:12}}><thead><tr><th style={th}>Actions</th>{columns.map(c=><th key={c} style={th}>{c}</th>)}</tr></thead><tbody>{filtered.map((row:any,i:number)=>{const v=row.values??row;const id=String(row.id??v.id??i);return <tr key={id}><td style={td}><button onClick={()=>openEdit(row)}>Edit</button> <button disabled={del.isPending} onClick={()=>{if(window.confirm("Delete this record?"))del.mutate(id)}}>Delete</button></td>{columns.map(c=><td key={c} style={td}>{format(v[c])}</td>)}</tr>})}</tbody></table></div>}
 {del.isError&&<Error message={(del.error as Error).message}/>}<div style={{display:"flex",justifyContent:"space-between",marginTop:16}}><small>{rowsQ.data?.totalElements??0} total · page {page+1}/{Math.max(1,rowsQ.data?.totalPages??1)}</small><span><button disabled={!page} onClick={()=>setPage(p=>p-1)}>Previous</button> <button disabled={!rowsQ.data||page+1>=rowsQ.data.totalPages} onClick={()=>setPage(p=>p+1)}>Next</button></span></div>
 </section></div></section></main>
}
function Login({config}:{config:Config}){const{login}=useAuth();const[u,setU]=useState("");const[p,setP]=useState("");const[e,setE]=useState("");const[b,setB]=useState(false);async function submit(ev:FormEvent){ev.preventDefault();setB(true);setE("");try{const c={username:u.trim(),password:p};await api.resources(c);login(c)}catch{setE("Login failed. Check credentials and API connection.")}finally{setB(false)}}return <main style={{minHeight:"100vh",display:"grid",placeItems:"center",background:"#111c32",fontFamily:"Inter,system-ui"}}><form onSubmit={submit} style={{background:"white",padding:30,borderRadius:14,width:"min(400px,90vw)"}}><small>ERPLANCERS · MODULE LOGIN</small><h1>{config.title}</h1><p>{config.description}</p><input required placeholder="Username" value={u} onChange={e=>setU(e.target.value)} style={input}/><input required type="password" placeholder="Password" value={p} onChange={e=>setP(e.target.value)} style={{...input,marginTop:10}}/>{e&&<Error message={e}/>}<button disabled={b} style={{...primary,width:"100%",marginTop:14}}>{b?"Connecting…":"Sign in"}</button></form></main>}
function Metric({label,value}:{label:string;value:unknown}){return <div style={panel}><small>{label}</small><h2>{Number(value).toLocaleString()}</h2></div>}
function Error({message}:{message:string}){return <p role="alert" style={{color:"#b42318"}}>{message}</p>}
function format(v:unknown){return v==null?"—":typeof v==="object"?JSON.stringify(v):String(v)}
const panel={background:"white",border:"1px solid #e5eaf1",borderRadius:12,padding:16,boxShadow:"0 4px 18px #12234008"};
const input={display:"block",width:"100%",boxSizing:"border-box" as const,padding:10,border:"1px solid #d6dce6",borderRadius:7,marginTop:6};
const nav={display:"block",width:"100%",textAlign:"left" as const,padding:10,marginTop:7,border:"1px solid #e5eaf1",borderRadius:8,cursor:"pointer"};
const primary={background:"#293b5d",color:"white",border:0,borderRadius:8,padding:"10px 14px",cursor:"pointer"};
const th={textAlign:"left" as const,padding:9,borderBottom:"1px solid #e5eaf1",whiteSpace:"nowrap" as const};
const td={padding:9,borderBottom:"1px solid #edf0f5",whiteSpace:"nowrap" as const,maxWidth:260,overflow:"hidden",textOverflow:"ellipsis" as const};
