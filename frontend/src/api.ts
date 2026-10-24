import type { AggregateReport, AdvancedAnalysis, Dashboard, EntityCatalog, PageResponse, ReportResource, ModuleReport, ResourceInfo, BiReport, OlapCatalog } from "./types";

const API_BASE = "/api";
export interface Credentials { username: string; password: string; }
function authHeader(c: Credentials) { return "Basic " + btoa(unescape(encodeURIComponent(c.username + ":" + c.password))); }

export async function apiFetch<T>(path:string, credentials:Credentials, init:RequestInit={}):Promise<T>{
  const headers=new Headers(init.headers); headers.set("Authorization",authHeader(credentials)); headers.set("Accept","application/json");
  if(init.body && !headers.has("Content-Type")) headers.set("Content-Type","application/json");
  const response=await fetch(API_BASE+path,{...init,headers,credentials:"same-origin"});
  if(response.status===401) throw new Error("AUTH_REQUIRED"); if(response.status===403) throw new Error("ACCESS_DENIED");
  if(!response.ok){
    const body=await response.text();
    let message = body || `Request failed: ${response.status}`;
    try {
      const parsed = JSON.parse(body);
      message = parsed.message || parsed.error || parsed.detail || message;
      if (parsed.path) message += ` (${parsed.path})`;
    } catch { /* keep plain-text response */ }
    throw new Error(message);
  }
  if(response.status===204)return undefined as T; return response.json() as Promise<T>;
}
export const api={
 resources:(c:Credentials)=>apiFetch<ResourceInfo[]>("/resources",c),
 registerAdmin:(body:{username:string;password:string;adminToken:string})=>fetch(API_BASE+"/auth/register-admin",{method:"POST",headers:{"Content-Type":"application/json","Accept":"application/json"},body:JSON.stringify(body)}).then(async r=>{if(!r.ok)throw new Error((await r.text())||`Admin registration failed: ${r.status}`);return r.json();}),
 register:(body:{username:string;password:string})=>fetch(API_BASE+"/auth/register",{method:"POST",headers:{"Content-Type":"application/json","Accept":"application/json"},body:JSON.stringify(body)}).then(async r=>{if(!r.ok)throw new Error((await r.text())||`Registration failed: ${r.status}`);return r.json();}),
 dashboard:(c:Credentials)=>apiFetch<Dashboard>("/analytics/dashboard",c),
 catalog:(c:Credentials)=>apiFetch<EntityCatalog>("/analytics/catalog",c),
 rows:(slug:string,page:number,size:number,c:Credentials)=>apiFetch<PageResponse>(`/resources/${encodeURIComponent(slug)}?page=${page}&size=${size}`,c),
 get:(slug:string,id:string,c:Credentials)=>apiFetch<any>(`/resources/${encodeURIComponent(slug)}/${id}`,c),
 metadata:(slug:string,id:string|undefined,c:Credentials)=>apiFetch<any>(`/resources/${encodeURIComponent(slug)}/metadata${id?"/"+id:""}`,c),
 create:(slug:string,values:Record<string,string>,c:Credentials)=>apiFetch<any>(`/resources/${encodeURIComponent(slug)}`,c,{method:"POST",body:JSON.stringify({values})}),
 update:(slug:string,id:string,values:Record<string,string>,c:Credentials)=>apiFetch<any>(`/resources/${encodeURIComponent(slug)}/${id}`,c,{method:"PUT",body:JSON.stringify({values})}),
 remove:(slug:string,id:string,c:Credentials)=>apiFetch<void>(`/resources/${encodeURIComponent(slug)}/${id}`,c,{method:"DELETE"}),
 aggregate:(slug:string,body:object,c:Credentials)=>apiFetch<AggregateReport>(`/analytics/resources/${encodeURIComponent(slug)}/aggregate`,c,{method:"POST",body:JSON.stringify(body)}),
 aggregateGet:(slug:string,params:{groupBy?:string[];metrics?:string[];orderBy?:string;limit?:number;filters?:Record<string,string>},c:Credentials)=>{const q=new URLSearchParams();if(params.groupBy?.length)q.set("groupBy",params.groupBy.join(","));if(params.metrics?.length)q.set("metrics",params.metrics.join(","));if(params.orderBy)q.set("orderBy",params.orderBy);if(params.limit!=null)q.set("limit",String(params.limit));Object.entries(params.filters??{}).forEach(([k,v])=>q.set("filter."+k,v));return apiFetch<AggregateReport>(`/analytics/resources/${encodeURIComponent(slug)}/aggregate?${q}`,c);},
 profile:(slug:string,c:Credentials)=>apiFetch<any>(`/analytics/resources/${encodeURIComponent(slug)}/profile`,c),
 quality:(slug:string,c:Credentials,topValues=10)=>apiFetch<import("./types").DataQualityReport>(`/analytics/resources/${encodeURIComponent(slug)}/quality?topValues=${topValues}`,c),
 advanced:(slug:string,body:object,c:Credentials)=>apiFetch<AdvancedAnalysis>(`/analytics/resources/${encodeURIComponent(slug)}/advanced`,c,{method:"POST",body:JSON.stringify(body)}),
 advancedGet:(slug:string,fields:string[],c:Credentials,sampleSize=10000,zScoreThreshold=3)=>{const q=new URLSearchParams({fields:fields.join(","),sampleSize:String(sampleSize),zScoreThreshold:String(zScoreThreshold)});return apiFetch<AdvancedAnalysis>(`/analytics/resources/${encodeURIComponent(slug)}/advanced?${q}`,c);},
 reportResources:(c:Credentials)=>apiFetch<ReportResource[]>("/reports/resources",c),
 report:(slug:string,c:Credentials)=>apiFetch<any>(`/reports/resources/${encodeURIComponent(slug)}`,c),
 modules:(c:Credentials)=>apiFetch<Record<string,ModuleReport>>("/reports/modules",c),
 module:(module:string,c:Credentials)=>apiFetch<ModuleReport>(`/reports/modules/${encodeURIComponent(module)}`,c),
 biQuery:(slug:string,body:object,c:Credentials)=>apiFetch<BiReport>(`/bi/resources/${encodeURIComponent(slug)}/query`,c,{method:"POST",body:JSON.stringify(body)}),
 biWorkspaceReports:(c:Credentials)=>apiFetch<any[]>("/bi/workspace/reports",c),
 saveBiWorkspaceReport:(body:object,c:Credentials)=>apiFetch<any>("/bi/workspace/reports",c,{method:"POST",body:JSON.stringify(body)}),
 biWorkspaceReport:(id:string,c:Credentials)=>apiFetch<any>(`/bi/workspace/reports/${encodeURIComponent(id)}`,c),
 updateBiWorkspaceReport:(id:string,body:object,c:Credentials)=>apiFetch<any>(`/bi/workspace/reports/${encodeURIComponent(id)}`,c,{method:"PUT",body:JSON.stringify(body)}),
 deleteBiWorkspaceReport:(id:string,c:Credentials)=>apiFetch<void>(`/bi/workspace/reports/${encodeURIComponent(id)}`,c,{method:"DELETE"}),
 executeBiWorkspaceReport:(id:string,c:Credentials)=>apiFetch<any>(`/bi/workspace/reports/${encodeURIComponent(id)}/execute`,c,{method:"POST"}),
 biWorkspaceExecution:(id:string,c:Credentials)=>apiFetch<any>(`/bi/workspace/executions/${encodeURIComponent(id)}`,c),
 biWorkspaceMonitoring:(c:Credentials)=>apiFetch<Record<string,number>>("/bi/workspace/monitoring",c),
 biAnalyze:(slug:string,body:object,c:Credentials)=>apiFetch<any>(`/bi/resources/${encodeURIComponent(slug)}/analyze`,c,{method:"POST",body:JSON.stringify(body)}),
 olapCatalog:(c:Credentials)=>apiFetch<OlapCatalog>("/bi/olap/catalog",c),
 olapProfiles:(c:Credentials)=>apiFetch<any>("/bi/olap/profiles",c),
 olapCube:(slug:string,body:object,c:Credentials)=>apiFetch<any>(`/bi/olap/resources/${encodeURIComponent(slug)}/cube`,c,{method:"POST",body:JSON.stringify(body)}),
 olapDrilldown:(slug:string,body:object,c:Credentials)=>apiFetch<any>(`/bi/olap/resources/${encodeURIComponent(slug)}/drilldown`,c,{method:"POST",body:JSON.stringify(body)}),
 autoOlapCubes:(c:Credentials,relationDepth=2)=>apiFetch<import("./types").AutoOlapCatalog>(`/bi/olap/cubes?relationDepth=${relationDepth}`,c),
 autoOlapCube:(slug:string,c:Credentials,relationDepth=2)=>apiFetch<import("./types").AutoOlapCube>(`/bi/olap/cubes/${encodeURIComponent(slug)}?relationDepth=${relationDepth}`,c),
 autoOlapRelations:(slug:string,relation:string,c:Credentials,relationDepth=2)=>apiFetch<import("./types").AutoOlapRelationDiscovery>(`/bi/olap/cubes/${encodeURIComponent(slug)}/relations?relation=${encodeURIComponent(relation)}&relationDepth=${relationDepth}`,c),
 autoOlapQuery:(slug:string,body:object,c:Credentials)=>apiFetch<any>(`/bi/olap/cubes/${encodeURIComponent(slug)}/query`,c,{method:"POST",body:JSON.stringify(body)}),
 accounts:(page:number,size:number,c:Credentials)=>apiFetch<import("./types").AccountView[]>(`/accounts?page=${page}&size=${size}`,c),
 account:(id:string|number,c:Credentials)=>apiFetch<import("./types").AccountView>(`/accounts/${encodeURIComponent(String(id))}`,c),
 businessRuleSchema:(resource:string,c:Credentials,relationDepth=3)=>apiFetch<import("./types").BusinessRuleSchema>(`/business-rules/schema/${encodeURIComponent(resource)}?relationDepth=${relationDepth}`,c),
 evaluateBusinessRule:(resource:string,id:number,ruleset:import("./types").BusinessRuleSet,c:Credentials)=>apiFetch<any>(`/business-rules/resources/${encodeURIComponent(resource)}/${id}/evaluate`,c,{method:"POST",body:JSON.stringify(ruleset)}),
 executeBusinessRule:(resource:string,id:number,ruleset:import("./types").BusinessRuleSet,c:Credentials)=>apiFetch<any>(`/business-rules/resources/${encodeURIComponent(resource)}/${id}/execute`,c,{method:"POST",body:JSON.stringify(ruleset)}),
 submitAnalyticsJob:(request:import("./types").AnalyticsJobSubmitRequest,c:Credentials)=>apiFetch<import("./types").AnalyticsJobView>("/analytics/jobs",c,{method:"POST",body:JSON.stringify(request)}),
 analyticsJobs:(tenantId:string,c:Credentials,status?:string)=>apiFetch<import("./types").AnalyticsJobView[]>(`/analytics/jobs?tenantId=${encodeURIComponent(tenantId)}${status?`&status=${encodeURIComponent(status)}`:""}`,c),
 analyticsJob:(id:string,tenantId:string,c:Credentials)=>apiFetch<import("./types").AnalyticsJobView>(`/analytics/jobs/${encodeURIComponent(id)}?tenantId=${encodeURIComponent(tenantId)}`,c),
 cancelAnalyticsJob:(id:string,tenantId:string,c:Credentials)=>apiFetch<import("./types").AnalyticsJobView>(`/analytics/jobs/${encodeURIComponent(id)}/cancel?tenantId=${encodeURIComponent(tenantId)}`,c,{method:"POST"}),
 analyticsModels:(tenantId:string,c:Credentials,modelName?:string)=>apiFetch<import("./types").AnalyticsModelView[]>(`/analytics/models?tenantId=${encodeURIComponent(tenantId)}${modelName?`&modelName=${encodeURIComponent(modelName)}`:""}`,c),
 analyticsExperiments:(tenantId:string,c:Credentials)=>apiFetch<import("./types").AnalyticsExperimentView[]>(`/analytics/experiments?tenantId=${encodeURIComponent(tenantId)}`,c),
 analyticsDrift:(tenantId:string,c:Credentials,modelName?:string)=>apiFetch<import("./types").AnalyticsDriftView[]>(`/analytics/drift?tenantId=${encodeURIComponent(tenantId)}${modelName?`&modelName=${encodeURIComponent(modelName)}`:""}`,c),
 reviewAnalyticsModel:(tenantId:string,name:string,version:number,approved:boolean,note:string,c:Credentials)=>apiFetch<import("./types").AnalyticsModelView>(`/analytics/models/${encodeURIComponent(name)}/${version}/review?tenantId=${encodeURIComponent(tenantId)}`,c,{method:"POST",body:JSON.stringify({approved,note})}),
 promoteAnalyticsModel:(tenantId:string,name:string,version:number,note:string,c:Credentials)=>apiFetch<import("./types").AnalyticsModelView>(`/analytics/models/${encodeURIComponent(name)}/${version}/promote?tenantId=${encodeURIComponent(tenantId)}`,c,{method:"POST",body:JSON.stringify({note})}),
 rollbackAnalyticsModel:(tenantId:string,name:string,version:number,note:string,c:Credentials)=>apiFetch<import("./types").AnalyticsModelView>(`/analytics/models/${encodeURIComponent(name)}/${version}/rollback?tenantId=${encodeURIComponent(tenantId)}`,c,{method:"POST",body:JSON.stringify({note})})
};