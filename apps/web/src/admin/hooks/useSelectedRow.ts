import React from "react";

export interface SelectedRowState<T> {
  selectedId: string;
  setSelectedId: (id: string) => void;
  selected: T | undefined;
}

/**
 * Keeps a single "selected row" id in sync with a list: when the list changes,
 * an out-of-range selection collapses to the first row, and an empty list clears
 * the selection. Mirrors the convergence effect previously duplicated verbatim
 * across many admin pages.
 */
export function useSelectedRow<T extends { id: string }>(rows: T[]): SelectedRowState<T> {
  const [selectedId, setSelectedId] = React.useState("");

  React.useEffect(() => {
    if (rows.length === 0) {
      setSelectedId("");
      return;
    }
    if (!rows.some((row) => row.id === selectedId)) setSelectedId(rows[0]!.id);
  }, [rows, selectedId]);

  const selected = rows.find((row) => row.id === selectedId) ?? rows[0];
  return { selectedId, setSelectedId, selected };
}
