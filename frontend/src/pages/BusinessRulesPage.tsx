import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { AutoComplete } from "primereact/autocomplete";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Dropdown } from "primereact/dropdown";
import { InputNumber } from "primereact/inputnumber";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Message } from "primereact/message";
import { Tag } from "primereact/tag";
import { TabView, TabPanel } from "primereact/tabview";
import { useAuth } from "../auth";
import { api } from "../api";
import type { BusinessRuleSchema, BusinessRuleSet, BusinessRule, BusinessRuleAction, BusinessRuleCondition } from "../types";

const OPERATORS = [
  "EXISTS", "NOT_NULL", "NULL", "IS_NULL", "EQUALS", "NOT_EQUALS",
  "GT", "GTE", "LT", "LTE", "CONTAINS", "IN"
].map(value => ({ label: value, value }));

const ACTIONS = ["VALIDATE", "SET", "CALCULATE", "REJECT", "LOG"]
  .map(value => ({ label: value, value }));

const TRIGGERS = [
  "BEFORE_SAVE", "AFTER_SAVE", "BEFORE_CREATE", "AFTER_CREATE",
  "BEFORE_UPDATE", "AFTER_UPDATE", "BEFORE_DELETE", "AFTER_DELETE",
  "BEFORE_STATUS_CHANGE", "AFTER_STATUS_CHANGE"
].map(value => ({ label: value, value }));

const FUNCTIONS = ["SUM()", "AVG()", "MIN()", "MAX()", "COUNT()"];

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

function newRule(priority = 10): BusinessRule {
  return {
    name: "Inventory validation",
    priority,
    condition: { path: "", operator: "NOT_NULL", value: "" },
    actions: [{ type: "VALIDATE", target: "", expression: "", message: "" }]
  };
}

function suggestionList(paths: string[], query: string) {
  const q = query.toLowerCase().trim();
  return paths.filter(path => !q || path.toLowerCase().includes(q)).slice(0, 50);
}

export function BusinessRulesPage() {
  const [pathSuggestions, setPathSuggestions] = useState<string[]>([]);
  const [expressionSuggestions, setExpressionSuggestions] = useState<string[]>([]);
  const { credentials } = useAuth();
  const [resource, setResource] = useState("");
  const [relationDepth, setRelationDepth] = useState(3);
  const [name, setName] = useState("Order Inventory Control");
  const [trigger, setTrigger] = useState("BEFORE_SAVE");
  const [rules, setRules] = useState<BusinessRule[]>([newRule()]);
  const [entityId, setEntityId] = useState<number | null>(null);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState("");

  const resources = useQuery({
    queryKey: ["business-rule-resources", credentials?.username],
    queryFn: () => api.resources(credentials!),
    enabled: Boolean(credentials),
    staleTime: 30_000
  });

  const schema = useQuery<BusinessRuleSchema>({
    queryKey: ["business-rule-schema", credentials?.username, resource, relationDepth],
    queryFn: () => api.businessRuleSchema(resource, credentials!, relationDepth),
    enabled: Boolean(credentials && resource),
    staleTime: 30_000
  });

  const paths = useMemo(
    () => (schema.data?.paths ?? []).map(p => p.path),
    [schema.data]
  );

  const relationPaths = useMemo(
    () => (schema.data?.paths ?? []).filter(p => p.relation),
    [schema.data]
  );

  const expressionTokens = useMemo(
    () => [...paths, ...FUNCTIONS, "true", "false", "null"],
    [paths]
  );

  function updateRule(index: number, patch: Partial<BusinessRule>) {
    setRules(previous => previous.map((rule, i) => i === index ? { ...rule, ...patch } : rule));
  }

  function updateCondition(index: number, patch: Partial<BusinessRuleCondition>) {
    setRules(previous => previous.map((rule, i) =>
      i === index ? { ...rule, condition: { ...rule.condition, ...patch } } : rule
    ));
  }

  function updateAction(ruleIndex: number, actionIndex: number, patch: Partial<BusinessRuleAction>) {
    setRules(previous => previous.map((rule, i) => {
      if (i !== ruleIndex) return rule;
      return {
        ...rule,
        actions: rule.actions.map((action, j) => j === actionIndex ? { ...action, ...patch } : action)
      };
    }));
  }

  function addRule() {
    setRules(previous => [...previous, newRule((previous.length + 1) * 10)]);
  }

  function removeRule(index: number) {
    setRules(previous => previous.filter((_, i) => i !== index));
  }

  function addAction(ruleIndex: number) {
    setRules(previous => previous.map((rule, i) =>
      i === ruleIndex
        ? { ...rule, actions: [...rule.actions, { type: "VALIDATE", target: "", expression: "", message: "" }] }
        : rule
    ));
  }

  function removeAction(ruleIndex: number, actionIndex: number) {
    setRules(previous => previous.map((rule, i) =>
      i === ruleIndex
        ? { ...rule, actions: rule.actions.filter((_, j) => j !== actionIndex) }
        : rule
    ));
  }

  const ruleset: BusinessRuleSet = {
    name,
    entity: schema.data?.entity ?? resource,
    trigger,
    rules
  };

  async function run(mode: "evaluate" | "execute") {
    if (!credentials || !resource || entityId == null) {
      setError("Select a resource and entity ID before evaluating a rule.");
      return;
    }
    setError("");
    setResult(null);
    try {
      const response = mode === "evaluate"
        ? await api.evaluateBusinessRule(resource, entityId, ruleset, credentials)
        : await api.executeBusinessRule(resource, entityId, ruleset, credentials);
      setResult(response);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  function reset() {
    setName("Order Inventory Control");
    setTrigger("BEFORE_SAVE");
    setRules([newRule()]);
    setResult(null);
    setError("");
  }

  if (resources.isPending) return <div className="p-4">Loading rule resources…</div>;
  if (resources.isError) return <Message severity="error" text={errorMessage(resources.error)} />;

  return (
    <div style={{ width: "100%", maxWidth: 1500, margin: "0 auto", paddingBottom: "2rem" }}>
      <div className="section-heading">
        <div>
          <div className="eyebrow">AUTOMATION / RULES</div>
          <h2>Business Rule Studio</h2>
          <p>Build relation-aware ERP rules with JPA metadata, path autocomplete and safe expressions.</p>
        </div>
        <div className="flex gap-2 align-items-center">
          <Tag value={schema.data ? `${paths.length} paths` : "No schema"} icon="pi pi-sitemap" />
          {relationPaths.length > 0 && <Tag value={`${relationPaths.length} relations`} severity="success" />}
        </div>
      </div>

      <TabView>
        <TabPanel header="Rule designer" leftIcon="pi pi-sliders-h">
          <div className="grid">
            <div className="col-12 lg:col-4">
              <Card title="Rule context">
                <div className="flex flex-column gap-3">
                  <label className="flex flex-column gap-2">
                    Resource
                    <Dropdown
                      value={resource}
                      options={(resources.data ?? []).map(r => ({ label: `${r.module} / ${r.entity}`, value: r.slug }))}
                      onChange={e => { setResource(e.value ?? ""); setResult(null); }}
                      placeholder="Select entity/resource"
                      filter
                      showClear
                      className="w-full"
                    />
                  </label>
                  <label className="flex flex-column gap-2">
                    Rule set name
                    <InputText value={name} onChange={e => setName(e.target.value)} />
                  </label>
                  <label className="flex flex-column gap-2">
                    Trigger
                    <Dropdown value={trigger} options={TRIGGERS} onChange={e => setTrigger(e.value)} />
                  </label>
                  <label className="flex flex-column gap-2">
                    Relation depth
                    <InputNumber value={relationDepth} min={0} max={6} showButtons onValueChange={e => setRelationDepth(e.value ?? 3)} />
                  </label>
                  <label className="flex flex-column gap-2">
                    Test entity ID
                    <InputNumber value={entityId} min={1} onValueChange={e => setEntityId(e.value ?? null)} />
                  </label>
                  {schema.data && (
                    <Message
                      severity="info"
                      text={`${schema.data.entity}: ${paths.length} fields/relations discovered at depth ${relationDepth}.`}
                    />
                  )}
                </div>
              </Card>
            </div>

            <div className="col-12 lg:col-8">
              <Card title="Rules">
                <div className="flex flex-column gap-3">
                  {rules.map((rule, ruleIndex) => (
                    <Card key={ruleIndex} className="surface-50">
                      <div className="flex justify-content-between align-items-center mb-3">
                        <div className="flex gap-2 align-items-center">
                          <Tag value={`Rule ${ruleIndex + 1}`} />
                          <InputText
                            value={rule.name}
                            onChange={e => updateRule(ruleIndex, { name: e.target.value })}
                            placeholder="Rule name"
                          />
                          <InputNumber
                            value={rule.priority}
                            onValueChange={e => updateRule(ruleIndex, { priority: e.value ?? 10 })}
                            min={0}
                            max={9999}
                            inputStyle={{ width: "6rem" }}
                          />
                        </div>
                        <Button icon="pi pi-trash" text severity="danger" disabled={rules.length === 1} onClick={() => removeRule(ruleIndex)} aria-label="Remove rule" />
                      </div>

                      <div className="grid">
                        <div className="col-12 xl:col-5">
                          <label className="flex flex-column gap-2">
                            Condition path
                            <AutoComplete
                              value={rule.condition.path}
                              suggestions={pathSuggestions}
                              completeMethod={e => setPathSuggestions(suggestionList(paths, e.query))}
                              onChange={e => updateCondition(ruleIndex, { path: e.value })}
                              dropdown
                              forceSelection={false}
                              placeholder="status, customer.creditLimit, lines.product.cost"
                              className="w-full"
                            />
                          </label>
                        </div>
                        <div className="col-12 md:col-4 xl:col-3">
                          <label className="flex flex-column gap-2">
                            Operator
                            <Dropdown value={rule.condition.operator} options={OPERATORS} onChange={e => updateCondition(ruleIndex, { operator: e.value })} />
                          </label>
                        </div>
                        <div className="col-12 md:col-8 xl:col-4">
                          <label className="flex flex-column gap-2">
                            Value
                            <InputText value={String(rule.condition.value ?? "")} onChange={e => updateCondition(ruleIndex, { value: e.target.value })} placeholder="CONFIRMED / 100 / true" />
                          </label>
                        </div>
                      </div>

                      <div className="flex flex-column gap-2 mt-2">
                        <div className="flex justify-content-between align-items-center">
                          <span className="font-semibold">Actions</span>
                          <Button label="Add action" icon="pi pi-plus" text size="small" onClick={() => addAction(ruleIndex)} />
                        </div>
                        {rule.actions.map((action, actionIndex) => (
                          <div key={actionIndex} className="grid align-items-end p-2 border-1 border-round surface-border">
                            <div className="col-12 md:col-3">
                              <label className="flex flex-column gap-2">Type
                                <Dropdown value={action.type} options={ACTIONS} onChange={e => updateAction(ruleIndex, actionIndex, { type: e.value })} />
                              </label>
                            </div>
                            <div className="col-12 md:col-4">
                              <label className="flex flex-column gap-2">Target
                                <AutoComplete
                                  value={action.target}
                                  suggestions={pathSuggestions}
                                  completeMethod={e => setPathSuggestions(suggestionList(paths, e.query))}
                                  onChange={e => updateAction(ruleIndex, actionIndex, { target: e.value })}
                                  dropdown
                                  placeholder="Root writable field"
                                />
                              </label>
                            </div>
                            <div className="col-12 md:col-5">
                              <label className="flex flex-column gap-2">Expression
                                <AutoComplete
                                  value={action.expression}
                                  suggestions={expressionSuggestions}
                                  completeMethod={e => setExpressionSuggestions(suggestionList(expressionTokens, e.query))}
                                  onChange={e => updateAction(ruleIndex, actionIndex, { expression: e.value })}
                                  dropdown
                                  placeholder="SUM(lines.quantity * lines.unitPrice)"
                                />
                              </label>
                            </div>
                            <div className="col-12 md:col-11">
                              <label className="flex flex-column gap-2">Message
                                <InputTextarea value={action.message} onChange={e => updateAction(ruleIndex, actionIndex, { message: e.target.value })} autoResize />
                              </label>
                            </div>
                            <div className="col-12 md:col-1">
                              <Button icon="pi pi-times" text severity="danger" onClick={() => removeAction(ruleIndex, actionIndex)} aria-label="Remove action" />
                            </div>
                          </div>
                        ))}
                      </div>
                    </Card>
                  ))}
                  <Button label="Add rule" icon="pi pi-plus" outlined onClick={addRule} />
                </div>
              </Card>
            </div>
          </div>

          {error && <Message className="mt-3 w-full" severity="error" text={error} />}

          <div className="flex flex-wrap gap-2 mt-3">
            <Button label="Evaluate" icon="pi pi-check-circle" onClick={() => run("evaluate")} disabled={!resource || entityId == null} />
            <Button label="Execute" icon="pi pi-bolt" severity="success" outlined onClick={() => run("execute")} disabled={!resource || entityId == null} />
            <Button label="Reset" icon="pi pi-refresh" text onClick={reset} />
          </div>
        </TabPanel>

        <TabPanel header="JSON / API" leftIcon="pi pi-code">
          <Card title="Generated ruleset">
            <pre style={{ maxHeight: 520, overflow: "auto", margin: 0 }}>{JSON.stringify(ruleset, null, 2)}</pre>
          </Card>
          {result && (
            <Card title="Evaluation result" className="mt-3">
              <pre style={{ maxHeight: 400, overflow: "auto", margin: 0 }}>{JSON.stringify(result, null, 2)}</pre>
            </Card>
          )}
        </TabPanel>
      </TabView>
    </div>
  );
}
