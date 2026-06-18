/**
 * Case-insensitive substring match across all serialized fields of a table row.
 * An empty keyword matches everything, so a blank search box shows the full list.
 * Used by the high-volume log pages (audit events, risk alerts, integration events,
 * ticket orders) to give a consistent client-side search/filter without each page
 * re-implementing the matcher.
 */
export function rowMatchesKeyword(row: unknown, keyword: string): boolean {
  const trimmed = keyword.trim().toLowerCase();
  if (!trimmed) return true;
  try {
    return JSON.stringify(row ?? {}).toLowerCase().includes(trimmed);
  } catch {
    return false;
  }
}
