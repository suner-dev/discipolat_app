/**
 * SPEC_ORGANISATION_MODULABLE_V3 §6.3 / T-W11 — `<ProgressionSparkline>`.
 * Mini-courbe SVG de progression (nombre de fidèles dans le temps). Composant
 * pur (aucune donnée nominative : uniquement des séries de compteurs, D7).
 *
 * <p>Extraite de la fiche nœud pour être réutilisable (web + console).
 */
export interface ProgressionPoint {
  snapshotAt: string;
  memberCount: number;
  churchCount?: number;
  leaderCount?: number;
}

export default function ProgressionSparkline({
  series,
  label,
  width = 300,
  height = 60,
}: {
  series: ProgressionPoint[];
  label?: string;
  width?: number;
  height?: number;
}) {
  if (!series || series.length < 2) {
    return <p className="text-sm text-gray-400">—</p>;
  }
  const vals = series.map((s) => s.memberCount);
  const max = Math.max(...vals, 1);
  const pts = vals
    .map((v, i) => `${(i / (vals.length - 1)) * width},${height - (v / max) * height}`)
    .join(' ');
  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="w-full h-16"
      role="img"
      aria-label={label ?? 'progression'}
    >
      <polyline
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
        points={pts}
        className="text-primary-500"
      />
    </svg>
  );
}
