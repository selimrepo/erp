import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card } from "primereact/card";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Button } from "primereact/button";
import { Message } from "primereact/message";
import { ProgressSpinner } from "primereact/progressspinner";
import { Tag } from "primereact/tag";
import { useAuth } from "../auth";
import { api } from "../api";
import type { AccountView } from "../types";

export function AccountsPage() {
  const { credentials } = useAuth();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [selected, setSelected] = useState<AccountView | null>(null);
  const accounts = useQuery({
    queryKey: ["accounts", page, size, credentials?.username],
    queryFn: () => api.accounts(page, size, credentials!),
    enabled: !!credentials
  });

  if (accounts.isPending) return <ProgressSpinner />;
  if (accounts.isError) return <Message severity="error" text={accounts.error.message} />;
  const rows = accounts.data ?? [];

  return <>
    <div className="section-heading">
      <div><div className="eyebrow">FINANCE & ACCOUNTING</div><h2>Chart of accounts</h2>
        <p>Browse the accounts exposed by the accounting API. Select a row to inspect its full response.</p></div>
      <Button label="Refresh" icon="pi pi-refresh" outlined onClick={() => accounts.refetch()} />
    </div>
    <Card>
      <DataTable value={rows} dataKey="id" selectionMode="single" selection={selected}
        onSelectionChange={e => setSelected(e.value as AccountView)}
        paginator rows={size} first={page * size} rowsPerPageOptions={[10, 20, 50, 100]}
        onPage={e => { setPage(e.page ?? 0); setSize(e.rows); setSelected(null); }}
        emptyMessage="No accounts returned for this page." stripedRows scrollable>
        <Column field="code" header="Code" sortable />
        <Column field="name" header="Account" sortable />
        <Column field="accountType" header="Type" sortable />
        <Column field="balanceType" header="Balance" />
        <Column field="level" header="Level" />
        <Column header="Posting" body={(row: AccountView) => <Tag value={row.postingAllowed ? "Allowed" : "Blocked"} severity={row.postingAllowed ? "success" : "secondary"} />} />
        <Column header="Status" body={(row: AccountView) => <Tag value={row.blocked ? "Blocked" : "Active"} severity={row.blocked ? "danger" : "success"} />} />
      </DataTable>
    </Card>
    {selected && <Card title={`Account details · ${selected.code ?? selected.id ?? ""}`} className="mt-3">
      <pre style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere", maxHeight: "480px", overflow: "auto" }}>{JSON.stringify(selected, null, 2)}</pre>
      <Button label="Close details" icon="pi pi-times" text onClick={() => setSelected(null)} />
    </Card>}
  </>;
}
