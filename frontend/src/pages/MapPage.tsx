import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  CircleMarker,
  Tooltip,
} from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import api from '@/lib/api';
import { Map as MapIcon, Users, Home, Layers, MapPin, TrendingUp, Heart } from 'lucide-react';
import type { MapPoint, MapPointType } from '@/types';

// Icônes Leaflet personnalisées (évite les images cassées des chemins par défaut)
const soulIcon = L.divIcon({
  className: '',
  html: `<div class="map-pin soul-pin"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg></div>`,
  iconSize: [26, 26],
  iconAnchor: [13, 13],
});

const familyIcon = L.divIcon({
  className: '',
  html: `<div class="map-pin family-pin"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg></div>`,
  iconSize: [26, 26],
  iconAnchor: [13, 13],
});

const STATUT_LABEL: Record<string, string> = {
  ACTIF: 'Actif',
  EN_INTEGRATION: 'En intégration',
  NOUVEAU_CONVERTI: 'Nouveau converti',
  NOUVEL_ARRIVANT: 'Nouvel arrivant',
  EN_VEILLE: 'En veille',
  DECROCHE: 'Décroché',
  ACTIVE: 'Active',
};

export default function MapPage() {
  const [typeFilter, setTypeFilter] = useState<MapPointType | 'ALL'>('ALL');
  const [zoneFilter, setZoneFilter] = useState<string>('ALL');

  const pointsQuery = useQuery({
    queryKey: ['map', 'points'],
    queryFn: async () => {
      const res = await api.get('/map/points');
      return res.data as MapPoint[];
    },
  });

  const allPoints = pointsQuery.data ?? [];

  const zones = useMemo(() => {
    const s = new Set<string>();
    allPoints.forEach(p => p.zone && s.add(p.zone));
    return Array.from(s).sort();
  }, [allPoints]);

  const points = useMemo(() => allPoints.filter(p =>
    (typeFilter === 'ALL' || p.type === typeFilter) &&
    (zoneFilter === 'ALL' || p.zone === zoneFilter)
  ), [allPoints, typeFilter, zoneFilter]);

  const stats = useMemo(() => {
    const souls = allPoints.filter(p => p.type === 'SOUL');
    const families = allPoints.filter(p => p.type === 'FAMILY');
    const byZone: Record<string, number> = {};
    souls.forEach(p => { if (p.zone) byZone[p.zone] = (byZone[p.zone] ?? 0) + 1; });
    const topZone = Object.entries(byZone).sort((a, b) => b[1] - a[1])[0];
    return {
      souls: souls.length,
      families: families.length,
      byZone,
      topZoneName: topZone?.[0] ?? '—',
      topZoneCount: topZone?.[1] ?? 0,
    };
  }, [allPoints]);

  const center: [number, number] = [-4.3217, 15.3121]; // Kinshasa

  return (
    <div className="animate-fade-in space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Cartographie</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Visualisez les disciples et familles par zone géographique
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 dark:text-gray-400 bg-white dark:bg-gray-800 rounded-xl px-3 py-2 border border-gray-200 dark:border-gray-700">
          <MapPin className="w-3.5 h-3.5 text-primary-500" />
          {stats.souls} disciples · {stats.families} familles
        </div>
      </div>

      {/* Stats par zone */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        <div className="glass-card p-3 flex items-center gap-3">
          <span className="p-2 rounded-lg bg-red-500/10 text-red-500"><Heart className="w-4 h-4" /></span>
          <div>
            <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{stats.souls}</p>
            <p className="text-[10px] font-medium text-gray-400 uppercase tracking-wide">Disciples</p>
          </div>
        </div>
        <div className="glass-card p-3 flex items-center gap-3">
          <span className="p-2 rounded-lg bg-primary-500/10 text-primary-500"><Home className="w-4 h-4" /></span>
          <div>
            <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{stats.families}</p>
            <p className="text-[10px] font-medium text-gray-400 uppercase tracking-wide">Familles</p>
          </div>
        </div>
        <div className="glass-card p-3 flex items-center gap-3">
          <span className="p-2 rounded-lg bg-amber-500/10 text-amber-500"><Layers className="w-4 h-4" /></span>
          <div>
            <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{stats.souls + stats.families}</p>
            <p className="text-[10px] font-medium text-gray-400 uppercase tracking-wide">Points totaux</p>
          </div>
        </div>
        <div className="glass-card p-3 flex items-center gap-3">
          <span className="p-2 rounded-lg bg-emerald-500/10 text-emerald-500"><Users className="w-4 h-4" /></span>
          <div className="min-w-0">
            <p className="text-sm font-bold text-gray-900 dark:text-gray-100 truncate">{stats.topZoneName}</p>
            <p className="text-[10px] font-medium text-gray-400 uppercase tracking-wide">Zone la + peuplée · {stats.topZoneCount} disciples</p>
          </div>
        </div>
        <div className="glass-card p-3 flex items-center gap-3">
          <span className="p-2 rounded-lg bg-blue-500/10 text-blue-500"><TrendingUp className="w-4 h-4" /></span>
          <div>
            <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{zones.length}</p>
            <p className="text-[10px] font-medium text-gray-400 uppercase tracking-wide">Zones actives</p>
          </div>
        </div>
      </div>

      {/* Filtres */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-1">
          {(['ALL', 'SOUL', 'FAMILY'] as const).map(t => (
            <button
              key={t}
              onClick={() => setTypeFilter(t)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                typeFilter === t
                  ? 'bg-primary-500 text-white shadow-sm'
                  : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
              }`}
            >
              {t === 'ALL' ? 'Tout' : t === 'SOUL' ? 'Disciples' : 'Familles'}
            </button>
          ))}
        </div>
        <select
          value={zoneFilter}
          onChange={e => setZoneFilter(e.target.value)}
          className="input w-auto text-xs"
        >
          <option value="ALL">Toutes les zones</option>
          {zones.map(z => <option key={z} value={z}>{z}</option>)}
        </select>
      </div>

      {/* Carte */}
      <div className="glass-card p-2">
        {pointsQuery.isLoading ? (
          <div className="h-[520px] flex items-center justify-center text-sm text-gray-400">Chargement de la carte…</div>
        ) : (
          <MapContainer
            center={center}
            zoom={11}
            className="w-full h-[520px] rounded-xl z-0"
            scrollWheelZoom
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {points.map(p => (
              p.type === 'FAMILY' ? (
                <CircleMarker
                  key={p.id}
                  center={[p.latitude, p.longitude]}
                  radius={14}
                  pathOptions={{ color: '#7c3aed', fillColor: '#7c3aed', fillOpacity: 0.25, weight: 2 }}
                >
                  <Tooltip direction="top" offset={[0, -10]}>
                    <span className="font-semibold">{p.nom}</span> — Famille
                  </Tooltip>
                  <Popup>
                    <div className="text-xs space-y-1 min-w-[180px]">
                      <p className="font-bold text-sm text-gray-900">{p.nom}</p>
                      <p className="text-gray-500 flex items-center gap-1">
                        <Home className="w-3 h-3" /> Famille de disciples
                      </p>
                      {p.statut && (
                        <span className="inline-block bg-primary-500/10 text-primary-600 font-semibold px-2 py-0.5 rounded-full text-[10px]">
                          {STATUT_LABEL[p.statut] ?? p.statut}
                        </span>
                      )}
                      {p.zone && <p className="text-gray-400 flex items-center gap-1"><MapPin className="w-3 h-3" /> {p.zone}</p>}
                      {p.departementNom && <p className="text-gray-500">Département : {p.departementNom}</p>}
                    </div>
                  </Popup>
                </CircleMarker>
              ) : (
                <Marker key={p.id} position={[p.latitude, p.longitude]} icon={soulIcon}>
                  <Tooltip direction="top" offset={[0, -14]}>
                    <span className="font-semibold">{p.nom}</span>
                  </Tooltip>
                  <Popup>
                    <div className="text-xs space-y-1 min-w-[180px]">
                      <p className="font-bold text-sm text-gray-900">{p.nom}</p>
                      <p className="text-gray-500 flex items-center gap-1">
                        <Users className="w-3 h-3" /> Disciple
                      </p>
                      {p.statut && (
                        <span className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                          p.statut === 'ACTIF' ? 'bg-emerald-500/10 text-emerald-600'
                          : p.statut === 'DECROCHE' ? 'bg-red-500/10 text-red-600'
                          : 'bg-amber-500/10 text-amber-600'
                        }`}>
                          {STATUT_LABEL[p.statut] ?? p.statut}
                        </span>
                      )}
                      {p.niveauCroissance && (
                        <p className="text-gray-500 flex items-center gap-1">
                          <TrendingUp className="w-3 h-3" /> Croissance : niveau {p.niveauCroissance}
                        </p>
                      )}
                      {p.familleNom && <p className="text-gray-500 flex items-center gap-1"><Home className="w-3 h-3" /> {p.familleNom}</p>}
                      {p.departementNom && <p className="text-gray-500">Département : {p.departementNom}</p>}
                      {p.zone && <p className="text-gray-400 flex items-center gap-1"><MapPin className="w-3 h-3" /> {p.zone}</p>}
                    </div>
                  </Popup>
                </Marker>
              )
            ))}
          </MapContainer>
        )}
      </div>

      {/* Légende */}
      <div className="flex flex-wrap items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-full bg-red-500 inline-block" /> Disciple
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-full bg-primary-500 inline-block" /> Famille
        </span>
        <span className="flex items-center gap-1.5">
          <MapIcon className="w-3.5 h-3.5" /> Fond de carte OpenStreetMap — localisation des disciples et familles
        </span>
      </div>
    </div>
  );
}
