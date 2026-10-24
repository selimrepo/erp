import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Dropdown } from "primereact/dropdown";
import { MultiSelect } from "primereact/multiselect";
import { InputNumber } from "primereact/inputnumber";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Message } from "primereact/message";
import { ProgressSpinner } from "primereact/progressspinner";
import { Tag } from "primereact/tag";
import { Divider } from "primereact/divider";
import { useAuth } from "../auth";
import { api } from "../api";

type Values = Record<string, string>;

type NestedCollection = {
  name: string;
  type: string;
  slug: string;
  fields: Record<string, string>;
  references: Array<{ name: string; type: string; slug: string }>;
  rows: Array<{
    id: number | string;
    values?: Record<string, unknown>;
    selected?: Record<string, unknown>;
  }>;
};

function scalarInput(
  name: string,
  type: string,
  value: string,
  setValue: (value: string) => void
) {
  if (/Boolean/i.test(type)) {
    return (
      <Dropdown
        value={value}
        options={[
          { label: "true", value: "true" },
          { label: "false", value: "false" }
        ]}
        onChange={(event) => setValue(event.value ?? "")}
        showClear
        className="field-control"
      />
    );
  }

  if (/Number|Long|Integer|Double|Float|BigDecimal|Short/i.test(type)) {
    return (
      <InputNumber
        value={value === "" ? null : Number(value)}
        onValueChange={(event) =>
          setValue(event.value == null ? "" : String(event.value))
        }
        className="field-control"
      />
    );
  }

  if (/String|Char|Text/i.test(type)) {
    return /description|note|comment/i.test(name) ? (
      <InputTextarea
        value={value}
        onChange={(event) => setValue(event.target.value)}
        autoResize
        className="field-control"
      />
    ) : (
      <InputText
        value={value}
        onChange={(event) => setValue(event.target.value)}
        className="field-control"
      />
    );
  }

  return (
    <InputText
      value={value}
      onChange={(event) => setValue(event.target.value)}
      className="field-control"
    />
  );
}

export function ResourceEditorPage() {
  const { slug = "", id } = useParams();
  const edit = !!id;
  const { credentials } = useAuth();
  const nav = useNavigate();
  const qc = useQueryClient();

  const [values, setValues] = useState<Values>({});
  const [nestedRows, setNestedRows] = useState<Record<string, Values[]>>({});
  const [openAggregates, setOpenAggregates] = useState<Record<string, boolean>>({});

  const meta = useQuery({
    queryKey: ["metadata", slug, id],
    queryFn: () => api.metadata(slug, id, credentials!),
    enabled: !!credentials && !!slug
  });

  useEffect(() => {
    if (!meta.data) return;

    const next: Values = { ...((meta.data.values || {}) as Values) };
    const selected = meta.data.selected || {};
    Object.entries(selected).forEach(([key, value]) => {
      next[key] = Array.isArray(value) ? value.join(",") : String(value ?? "");
    });
    setValues(next);

    const collections = (meta.data.nestedCollections || []) as NestedCollection[];
    const initial: Record<string, Values[]> = {};
    collections.forEach((collection) => {
      initial[collection.name] = collection.rows.map((row) => ({
        _id: String(row.id),
        _key: `existing:${String(row.id)}`,
        ...(Object.fromEntries(
          Object.entries(row.values || {}).map(([key, value]) => [
            key,
            value == null ? "" : String(value)
          ])
        ) as Values),
        ...(Object.fromEntries(
          Object.entries(row.selected || {}).map(([key, value]) => [
            `ref.${key}`,
            value == null ? "" : String(value)
          ])
        ) as Values)
      }));
    });
    setNestedRows(initial);
  }, [meta.data]);

  const fields = (meta.data?.fields || {}) as Record<string, string>;
  const refs = (meta.data?.references || []) as Array<{
    name: string;
    type: string;
    slug: string;
  }>;
  const collections = (meta.data?.collections || []) as Array<{
    name: string;
    type: string;
    slug: string;
  }>;
  const nestedCollections = (meta.data?.nestedCollections || []) as NestedCollection[];
  const options = (meta.data?.options || {}) as Record<
    string,
    Array<{ id: string | number; label: string }>
  >;
  const nestedOptions = (meta.data?.nestedOptions || {}) as Record<
    string,
    Array<{ id: string | number; label: string }>
  >;

  const refMap = useMemo(
    () => new Map(refs.map((reference) => [reference.name, reference])),
    [refs]
  );
  const collectionMap = useMemo(
    () => new Map(collections.map((collection) => [collection.name, collection])),
    [collections]
  );

  const buildPayload = () => {
    const payload: Record<string, string> = {};

    Object.entries(values).forEach(([name, value]) => {
      if (refMap.has(name)) {
        if (value) payload[`ref.${name}`] = value;
      } else if (collectionMap.has(name)) {
        if (value) payload[`collection.${name}`] = value;
      } else {
        payload[name] = value;
      }
    });

    Object.entries(nestedRows).forEach(([collectionName, rows]) => {
      rows.forEach((row, index) => {
        Object.entries(row).forEach(([name, value]) => {
          payload[`nested.${collectionName}.${index}.${name}`] = value;
        });
      });
    });

    return payload;
  };

  const mutation = useMutation({
    mutationFn: () => {
      const payload = buildPayload();
      return edit
        ? api.update(slug, id!, payload, credentials!)
        : api.create(slug, payload, credentials!);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rows", slug] });
      nav(`/resources/${slug}`);
    }
  });

  if (meta.isPending) return <ProgressSpinner />;
  if (meta.isError)
    return <Message severity="error" text={meta.error.message} />;

  function updateValue(name: string, value: string) {
    setValues((current) => ({ ...current, [name]: value }));
  }

  function addAggregate(collectionName: string) {
    const key = `new:${crypto.randomUUID()}`;
    setNestedRows((current) => {
      const index = current[collectionName]?.length || 0;
      const row = { _id: "", _key: key, _new: "true" };
      setOpenAggregates((open) => ({
        ...open,
        [`${collectionName}:${index}`]: true
      }));
      return {
        ...current,
        [collectionName]: [...(current[collectionName] || []), row]
      };
    });
  }

  function updateAggregate(
    collectionName: string,
    index: number,
    name: string,
    value: string
  ) {
    setNestedRows((current) => ({
      ...current,
      [collectionName]: (current[collectionName] || []).map((row, rowIndex) =>
        rowIndex === index ? { ...row, [name]: value } : row
      )
    }));
  }

  function removeAggregate(collectionName: string, index: number) {
    setNestedRows((current) => ({
      ...current,
      [collectionName]: (current[collectionName] || []).map((row, rowIndex) =>
        rowIndex === index ? { ...row, _delete: "true" } : row
      )
    }));
    setOpenAggregates((current) => {
      const next = { ...current };
      delete next[`${collectionName}:${index}`];
      return next;
    });
  }

  function restoreAggregate(collectionName: string, index: number) {
    setNestedRows((current) => ({
      ...current,
      [collectionName]: (current[collectionName] || []).map((row, rowIndex) =>
        rowIndex === index ? { ...row, _delete: "" } : row
      )
    }));
  }

  function renderReference(
    referenceName: string,
    value: string,
    setValue: (value: string) => void,
    aggregate?: NestedCollection
  ) {
    const opts = aggregate
      ? nestedOptions[`${aggregate.name}.${referenceName}`] || []
      : options[referenceName] || [];

    return (
      <Dropdown
        value={value}
        options={opts.map((option) => ({
          label: option.label,
          value: String(option.id)
        }))}
        onChange={(event) => setValue(event.value ?? "")}
        placeholder={referenceName}
        filter
        showClear
        className="field-control"
      />
    );
  }

  function renderField(name: string, type: string) {
    const value = values[name] ?? "";
    if (refMap.has(name)) {
      return renderReference(name, value, (next) => updateValue(name, next));
    }
    if (collectionMap.has(name)) {
      const opts = (options[name] || []).map((option) => ({
        label: option.label,
        value: String(option.id)
      }));
      return (
        <MultiSelect
          value={value ? value.split(",") : []}
          options={opts}
          onChange={(event) => updateValue(name, (event.value || []).join(","))}
          filter
          showClear
          maxSelectedLabels={2}
          className="field-control"
        />
      );
    }
    return scalarInput(name, type, value, (next) => updateValue(name, next));
  }

  function renderAggregateEditor(collection: NestedCollection, index: number) {
    const row = nestedRows[collection.name]?.[index] || {};
    if (row._delete === "true") {
      return (
        <div className="flex align-items-center justify-content-between gap-2 p-2">
          <span className="text-500">This aggregate will be deleted when saved.</span>
          <Button
            label="Restore"
            icon="pi pi-undo"
            text
            size="small"
            onClick={() => restoreAggregate(collection.name, index)}
          />
        </div>
      );
    }

    return (
      <div className="form-grid">
        {Object.entries(collection.fields)
          .filter(([name]) => name !== "id" && name !== "version")
          .map(([name, type]) => (
            <label key={name}>
              <span>{name}</span>
              {scalarInput(
                name,
                type,
                row[name] ?? "",
                (value) => updateAggregate(collection.name, index, name, value)
              )}
            </label>
          ))}

        {collection.references.map((reference) => (
          <label key={reference.name}>
            <span>{reference.name}</span>
            {renderReference(
              reference.name,
              row[`ref.${reference.name}`] ?? "",
              (value) =>
                updateAggregate(
                  collection.name,
                  index,
                  `ref.${reference.name}`,
                  value
                ),
              collection
            )}
          </label>
        ))}

        <div className="flex align-items-end gap-2">
          <Button
            label="Remove"
            icon="pi pi-trash"
            severity="danger"
            outlined
            onClick={() => removeAggregate(collection.name, index)}
          />
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="section-heading">
        <div>
          <div className="eyebrow">CRUD</div>
          <h2>{edit ? "Edit" : "New"} {slug}</h2>
          {nestedCollections.length > 0 && (
            <p>
              Manage the aggregate root and its child records in one transaction.
            </p>
          )}
        </div>
        <Button
          label="Back"
          icon="pi pi-arrow-left"
          outlined
          onClick={() => nav(`/resources/${slug}`)}
        />
      </div>

      <Card title="Aggregate root">
        <div className="form-grid">
          {Object.entries(fields)
            .filter(([name]) => name !== "id" && name !== "version")
            .map(([name, type]) => (
              <label key={name}>
                <span>{name}</span>
                {renderField(name, type)}
              </label>
            ))}
        </div>
      </Card>

      {nestedCollections.map((collection) => {
        const allRows = nestedRows[collection.name] || [];
        const activeRows = allRows.filter((row) => row._delete !== "true");
        const deletedRows = allRows
          .map((row, index) => ({ row, index }))
          .filter(({ row }) => row._delete === "true");
        return (
          <Card key={collection.name} className="mt-3">
            <div className="flex align-items-center justify-content-between gap-2 flex-wrap mb-3">
              <div>
                <div className="flex align-items-center gap-2">
                  <h3 className="m-0">{collection.name}</h3>
                  <Tag value={`${activeRows.length} items`} severity="info" />
                </div>
                <small className="text-500">
                  {collection.type} · edit child records as part of this aggregate
                </small>
              </div>
              <Button
                label={`Add ${collection.type}`}
                icon="pi pi-plus"
                onClick={() => addAggregate(collection.name)}
              />
            </div>

            {activeRows.length === 0 ? (
              <Message severity="info" text={`No ${collection.name} records yet.`} />
            ) : (
              <DataTable
                value={activeRows.map((row) => ({
                  ...row,
                  __index: nestedRows[collection.name].indexOf(row),
                  _key: row._key || `existing:${String(row._id)}`
                }))}
                dataKey="_key"
                stripedRows
                responsiveLayout="scroll"
              >
                <Column
                  header="#"
                  body={(_, options) => options.rowIndex + 1}
                  style={{ width: "4rem" }}
                />
                {Object.entries(collection.fields)
                  .filter(([name]) => name !== "id" && name !== "version")
                  .slice(0, 5)
                  .map(([name]) => (
                    <Column
                      key={name}
                      field={name}
                      header={name}
                      body={(row) => row[name] || "—"}
                    />
                  ))}
                {collection.references.slice(0, 2).map((reference) => (
                  <Column
                    key={reference.name}
                    header={reference.name}
                    body={(row) => {
                      const selected = row[`ref.${reference.name}`];
                      const option = (
                        nestedOptions[`${collection.name}.${reference.name}`] ||
                        []
                      ).find((item) => String(item.id) === String(selected));
                      return option?.label || "—";
                    }}
                  />
                ))}
                <Column
                  header="Actions"
                  body={(row) => (
                    <div className="flex gap-1">
                      <Button
                        icon="pi pi-pencil"
                        text
                        aria-label={`Edit ${collection.type}`}
                        onClick={() =>
                          setOpenAggregates((current) => ({
                            ...current,
                            [`${collection.name}:${row.__index}`]: true
                          }))
                        }
                      />
                      <Button
                        icon="pi pi-trash"
                        text
                        severity="danger"
                        aria-label={`Delete ${collection.type}`}
                        onClick={() =>
                          removeAggregate(collection.name, row.__index)
                        }
                      />
                    </div>
                  )}
                />
              </DataTable>
            )}

            {deletedRows.length > 0 && (
              <div className="mt-3">
                <div className="flex align-items-center gap-2 mb-2">
                  <span className="font-semibold">Pending deletion</span>
                  <Tag value={String(deletedRows.length)} severity="danger" />
                </div>
                <DataTable
                  value={deletedRows.map(({ row, index }) => ({ ...row, __index: index }))}
                  dataKey="_key"
                  size="small"
                  stripedRows
                >
                  <Column header="#" body={(_, options) => options.rowIndex + 1} style={{ width: "4rem" }} />
                  {Object.entries(collection.fields)
                    .filter(([name]) => name !== "id" && name !== "version")
                    .slice(0, 3)
                    .map(([name]) => (
                      <Column key={name} field={name} header={name} body={(row) => row[name] || "—"} />
                    ))}
                  <Column
                    header="Actions"
                    body={(row) => (
                      <Button
                        label="Restore"
                        icon="pi pi-undo"
                        text
                        size="small"
                        onClick={() => restoreAggregate(collection.name, row.__index)}
                      />
                    )}
                  />
                </DataTable>
              </div>
            )}

            {activeRows.map((row) => {
              const index = nestedRows[collection.name].indexOf(row);
              const key = `${collection.name}:${index}`;
              if (!openAggregates[key]) return null;
              return (
                <Card key={key} className="mt-3">
                  <div className="flex align-items-center justify-content-between">
                    <div className="font-semibold">
                      {collection.type} {row._id ? `#${row._id}` : "(new)"}
                    </div>
                    <Button
                      icon="pi pi-times"
                      text
                      aria-label="Close aggregate editor"
                      onClick={() =>
                        setOpenAggregates((current) => {
                          const next = { ...current };
                          delete next[key];
                          return next;
                        })
                      }
                    />
                  </div>
                  <Divider />
                  {renderAggregateEditor(collection, index)}
                </Card>
              );
            })}
          </Card>
        );
      })}

      {mutation.isError && (
        <Message severity="error" text={mutation.error.message} className="mt-3" />
      )}

      <div className="form-actions">
        <Button
          label={edit ? "Save aggregate" : "Create aggregate"}
          icon="pi pi-check"
          onClick={() => mutation.mutate()}
          loading={mutation.isPending}
        />
        <Button
          label="Cancel"
          outlined
          onClick={() => nav(`/resources/${slug}`)}
        />
      </div>
    </>
  );
}
