import {
  EXTRACTION_FIELDS,
  fieldValue,
  type Extraction,
  type ExtractionFieldKey,
} from "@/lib/extraction";

export type ValueSource = {
  value: string;
  documents: string[];
};

export type ConsolidatedField = {
  key: ExtractionFieldKey;
  label: string;
  values: ValueSource[];
  agreed: boolean;
  missing: boolean;
};

export type Consolidated = {
  fields: ConsolidatedField[];
  sources: number;
  conflicts: number;
};

type Source = {
  filename: string;
  extraction: Extraction | null;
};

export function consolidate(documents: Source[]): Consolidated {
  const completed = documents.filter(
    (document): document is { filename: string; extraction: Extraction } =>
      document.extraction?.status === "COMPLETED",
  );

  const fields = EXTRACTION_FIELDS.map(({ key, label }) => {
    const grouped = new Map<string, string[]>();

    for (const { filename, extraction } of completed) {
      const value = fieldValue(extraction, key).trim();
      if (!value) {
        continue;
      }
      const existing = grouped.get(value);
      if (existing) {
        existing.push(filename);
      } else {
        grouped.set(value, [filename]);
      }
    }

    const values = [...grouped.entries()]
      .map(([value, docs]) => ({ value, documents: docs }))
      .sort((a, b) => b.documents.length - a.documents.length);

    return {
      key,
      label,
      values,
      agreed: values.length <= 1,
      missing: values.length === 0,
    };
  });

  return {
    fields,
    sources: completed.length,
    conflicts: fields.filter((field) => !field.agreed).length,
  };
}
