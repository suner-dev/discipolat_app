import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { MapContainer, TileLayer, CircleMarker, Popup, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import api from '@/lib/api';
import { Loader2, Flame, MapPin, Compass, Filter } from 'lucide-react';

interface HeatCell {
  lat: number;
  lng: number;
  count: number;
  intensity: number;
}

interface Sector {
  zone: string;
  souls: number;
  activeSouls: number;
  activityPercent: number;
  priority: string;
}

const CIRCLE_COLOR = (intensity: number) =>
  intensity > 0.75 ? '#dc2626' : intensity > 0.5 ? '#f97316' : intensity > 0.25 ? '#eab308' : '#22c55e';

const PRIORITY_STYLES: Record<string, string> = {
  CRITIQUE: 'badge-danger',
  HAUTE: 'badge-warning',
  MOYENNE: 'badge-info',
};

/** Simple client-side clustering — groups nearby points at low zoom. */
function clusterPoints(cells: HeatCell[], zoomLevel: number): { lat: number; lng: number; count: number; intensity: number; points: HeatCell[] }[] {
  // Cluster radius shrinks with zoom
  const clusterRadius = Math.max(0.001, 0.05 / Math.pow(2, zoomLevel - 10));
  const used = new Set<number>();
  const clusters: { lat: number; lng: number; count: number; intensity: number; points: HeatCell[] }[] = [];
  for (let i = 0; i < cells.length; i++) {
    if (used.has(i)) continue;
    const group: HeatCell[] = [cells[i]];
    used.add(i);
    for (let j = i + 1; j < cells.length; j++) {
      if (used.has(j)) continue;
      const dLat = cells[i].lat - cells[j].lat;
      const dLng = cells[i].lng - cells[j].lng;
      if (Math.sqrt(dLat * dLat + dLng * dLng) < clusterRadius) {
        group.push(cells[j]);
        used.add(j);
      }
    }
    clusters.push({
      lat: group.reduce((s, c) => s + c.lat, 0) / group.length,
      lng: group.reduce((s, c) => s + c.lng, 0) / group.length,
      count: group.reduce((s, c) => s + c.count, 0),
      intensity: group.reduce((s, c) => s + c.intensity, 0) / group.length,
      points: group,
    });
  }
  return clusters;
}

/** Kingdom Mapping — heatmap géographique de l'implantation & secteurs prioritaires. */
export default function KingdomMappingPage() {
  const [view, setView] = useState<'heatmap' | 'sectors'>('heatmap');
  const [densityFilter, setDensityFilter] = useState<'all' | 'high' | 'medium' | 'low'>('all');

  const heatQuery = useQuery({
    queryKey: ['map-heatmap'],
    queryFn: async () => (await api.get<HeatCell[]>('/map/heatmap')).data,
  });

  const sectorsQuery = useQuery({
    queryKey: ['map-sectors'],
    queryFn: async () => (await api.get<Sector[]>('/map/sectors')).data,
  });

  const allCells = heatQuery.data ?? [];

  // P16 — Density filter
  const cells = useMemo(() => {
    if (densityFilter === 'all') return allCells;
    if (densityFilter === 'high') return allCells.filter((c) => c.intensity > 0.75);
    if (densityFilter === 'medium') return allCells.filter((c) => c.intensity > 0.25 && c.intensity <= 0.75);
    return allCells.filter((c) => c.intensity <= 0.25);
  }, [allCells, densityFilter]);

  // P16 — Client-side clustering
  const [mapZoom, setMapZoom] = useState(12);
  const clustered = useMemo(() => clusterPoints(cells, mapZoom), [cells, mapZoom]);

  function ZoomTracker() {
    useMapEvents({ zoomend: (e) => setMapZoom(e.target.getZoom()) });
    return null;
  }

  const center: [number, number] =
    cells.length > 0
      ? [
          cells.reduce((s, c) => s + c.lat, 0) / cells.length,
          cells.reduce((s, c) => s + c.lng, 0) / cells.length,
        ]
      : [5.348, -4.027]; // Abidjan par défaut

  const [, forceRender] = useState(0);
  useEffect(() => forceRender(1), []); // fix Leaflet container resize

  if (heatQuery.isLoading || sectorsQuery.isLoading) {
    return <Loader2 className="w-8 h-8 animate-spin text-primary-500 mx-auto mt-20" />;
  }

  const maxCount = Math.max(...cells.map((c) => c.count), 1);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-green-500 to-emerald-600 text-white shadow-lg">
          <Compass className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Kingdom Mapping</h1>
          <p className="page-subtitle">Où l'œuvre porte-t-elle du fruit ? Cartes et secteurs prioritaires</p>
        </div>            <div className="ml-auto flex gap-2">
          <button
            onClick={() => setView('heatmap')}
            className={`btn-sm px-4 py-2 rounded-lg font-medium ${view === 'heatmap' ? 'btn-primary' : 'glass-card'}`}
          >
            <Flame className="inline w-4 h-4 mr-1" /> Heatmap
          </button>
          <button
            onClick={() => setView('sectors')}
            className={`btn-sm px-4 py-2 rounded-lg font-medium ${view === 'sectors' ? 'btn-primary' : 'glass-card'}`}
          >
            <MapPin className="inline w-4 h-4 mr-1" /> Secteurs
          </button>
        </div>
      </div>

      {view === 'heatmap' && (
        <>
          {cells.length === 0 ? (
            <div className="glass-card p-12 text-center">
              <MapPin className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
              <p className="text-gray-500 dark:text-gray-400 max-w-md mx-auto">
                Aucune position GPS enregistrée. Ajoutez des coordonnées aux âmes pour visualiser la carte
                d'implantation.
              </p>
            </div>
          ) : (
            <div className="glass-card overflow-hidden rounded-2xl animate-slide-up" style={{ height: 480 }}>
              <MapContainer center={center} zoom={12} style={{ height: '100%', width: '100%' }}>
                <ZoomTracker />
                <TileLayer
                  attribution='&copy; OpenStreetMap'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                {clustered.map((cl, i) => (
                  <CircleMarker
                    key={i}
                    center={[cl.lat, cl.lng]}
                    radius={cl.points.length > 1 ? 16 + Math.min(cl.count, 20) * 1.5 : 8 + (cl.count / maxCount) * 24}
                    pathOptions={{
                      color: CIRCLE_COLOR(cl.intensity),
                      fillColor: CIRCLE_COLOR(cl.intensity),
                      fillOpacity: 0.45,
                      weight: cl.points.length > 1 ? 2 : 1,
                    }}
                  >
                    <Popup>
                      {cl.points.length > 1 ? (
                        <>
                          <strong>Cluster de {cl.points.length} points</strong> — {cl.count} âme(s) total
                          <br />Densité moyenne : {(cl.intensity * 100).toFixed(0)}%
                          <br /><em className="text-xs">Zoomez pour détailler</em>
                        </>
                      ) : (
                        <>
                          <strong>{cl.count} âme(s)</strong> dans ce secteur
                          <br />Intensité : {(cl.intensity * 100).toFixed(0)}%
                        </>
                      )}
                    </Popup>
                  </CircleMarker>
                ))}
              </MapContainer>
            </div>
          )}
          <div className="flex items-center gap-4 mt-3 text-xs text-gray-500 dark:text-gray-400 flex-wrap">
            <span className="flex items-center gap-1"><Filter className="w-3 h-3" /> Densité :</span>
            {([['all', 'Tous'], ['#22c55e', 'Faible'], ['#eab308', 'Moyenne'], ['#f97316', 'Forte'], ['#dc2626', 'Très forte']] as const).map(
              ([color, label]) => (
                <button
                  key={label}
                  onClick={() => setDensityFilter(color === 'all' ? 'all' : label === 'Faible' ? 'low' : label === 'Moyenne' ? 'medium' : 'high')}
                  className={`flex items-center gap-1 px-2 py-0.5 rounded-full transition ${
                    densityFilter === (color === 'all' ? 'all' : label === 'Faible' ? 'low' : label === 'Moyenne' ? 'medium' : 'high')
                      ? 'bg-primary-500/20 text-primary-400 ring-1 ring-primary-500/50' : ''
                  }`}
                >
                  {color !== 'all' && <span className="w-3 h-3 rounded-full inline-block" style={{ background: color }} />}{' '}{label}
                </button>
              ),
            )}
            <span className="ml-2 text-gray-400">Clustering auto : {clustered.length} points affichés (zoom {mapZoom})</span>
          </div>
        </>
      )}

      {view === 'sectors' && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {(sectorsQuery.data ?? []).map((s) => (
            <div key={s.zone} className="glass-card p-5 animate-slide-up">
              <div className="flex items-center justify-between mb-2">
                <h3 className="font-semibold text-gray-900 dark:text-gray-100">{s.zone}</h3>
                <span className={`badge ${PRIORITY_STYLES[s.priority] ?? 'badge-info'}`}>{s.priority}</span>
              </div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">
                {s.activeSouls} âmes actives sur {s.souls}
              </p>
              <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    s.activityPercent >= 60
                      ? 'bg-green-500'
                      : s.activityPercent >= 35
                        ? 'bg-amber-500'
                        : 'bg-red-500'
                  }`}
                  style={{ width: `${Math.min(100, s.activityPercent)}%` }}
                />
              </div>
              <p className="text-xs text-gray-400 mt-1">Activité : {Math.round(s.activityPercent)}%</p>
            </div>
          ))}
          {!sectorsQuery.isLoading && (sectorsQuery.data ?? []).length === 0 && (
            <p className="text-center text-sm text-gray-500 col-span-full py-8">
              Aucune donnée sectorielle — renseignez les zones géographiques des âmes.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
