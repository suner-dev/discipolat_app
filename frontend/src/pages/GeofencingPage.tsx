import { useState, useEffect } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { MapPin, Navigation, Clock, RefreshCw, Loader2, CheckCircle, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface GeofencePing {
  id: string;
  userId: string;
  latitude: number;
  longitude: number;
  distanceMeters: number;
  inZone: boolean;
  kind: string;
  powerMode: string;
  createdAt: string;
}

interface GeofenceConfig {
  enabled: boolean;
  latitude: number;
  longitude: number;
  radiusMeters: number;
  churchName: string;
}

export default function GeofencingPage() {
  const [location, setLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const [tracking, setTracking] = useState(false);

  const { data: history = [], isLoading, refetch } = useQuery({
    queryKey: ['geofencing-history-all'],
    queryFn: async () => (await api.get<GeofencePing[]>('/geofencing/history/all')).data,
  });

  const { data: config } = useQuery({
    queryKey: ['geofencing-config'],
    queryFn: async () => (await api.get<GeofenceConfig>('/geofencing/config')).data,
  });

  const autoCheckInMutation = useMutation({
    mutationFn: async (coords: { latitude: number; longitude: number; accuracy: number }) =>
      (await api.post('/geofencing/auto-check-in', { ...coords, powerMode: 'NORMAL' })).data,
    onSuccess: (data: any) => {
      if (data.inZone) toast.success(data.message || 'Présence enregistrée');
      else toast(data.message || 'Hors zone', { icon: '📍' });
    },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const startTracking = () => {
    if (!navigator.geolocation) { toast.error('Géolocalisation non supportée'); return; }
    setTracking(true);
    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        const coords = { latitude: pos.coords.latitude, longitude: pos.coords.longitude, accuracy: pos.coords.accuracy };
        setLocation({ latitude: coords.latitude, longitude: coords.longitude });
        autoCheckInMutation.mutate(coords);
      },
      (err) => { toast.error('Erreur de géolocalisation: ' + err.message); setTracking(false); },
      { enableHighAccuracy: true, maximumAge: 30000, timeout: 10000 }
    );
    // Stop after 5 minutes
    setTimeout(() => { navigator.geolocation.clearWatch(watchId); setTracking(false); }, 300000);
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg">
          <MapPin className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Géorepérage</h1>
          <p className="page-subtitle">Pointage automatique par GPS</p>
        </div>
        <div className="ml-auto flex gap-2">
          <button onClick={() => refetch()} className="p-2 rounded-xl bg-white/10 hover:bg-white/20 transition">
            <RefreshCw className="w-4 h-4 text-gray-500" />
          </button>
          <button onClick={startTracking} disabled={tracking}
            className={`px-4 py-2 rounded-xl text-sm font-medium flex items-center gap-2 transition-all ${tracking ? 'bg-red-500 text-white' : 'bg-emerald-500 text-white hover:bg-emerald-600'}`}>
            {tracking ? <Loader2 className="w-4 h-4 animate-spin" /> : <Navigation className="w-4 h-4" />}
            {tracking ? 'Suivi actif...' : 'Activer le suivi GPS'}
          </button>
        </div>
      </div>

      {config && (
        <div className="glass-card p-4 mb-6 flex flex-wrap gap-6 text-sm text-gray-600 dark:text-gray-400">
          <div className="flex items-center gap-2"><MapPin className="w-4 h-4" /> {config.churchName}</div>
          <div>Rayon: {config.radiusMeters}m</div>
          <div>Statut: {config.enabled ? <span className="text-green-600">Activé</span> : <span className="text-red-600">Désactivé</span>}</div>
          {location && <div>Position: {location.latitude.toFixed(5)}, {location.longitude.toFixed(5)}</div>}
        </div>
      )}

      {isLoading ? <SkeletonLoader lines={5} variant="table" /> :
        history.length === 0 ? (
          <EmptyState icon={<MapPin className="w-8 h-8 text-gray-400" />}
            title="Aucun historique"
            message="Activez le suivi GPS pour enregistrer vos présences" />
        ) : (
          <div className="space-y-2">
            {history.map(ping => (
              <div key={ping.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 flex items-center gap-4">
                {ping.inZone ? <CheckCircle className="w-5 h-5 text-green-500 flex-shrink-0" /> : <XCircle className="w-5 h-5 text-red-400 flex-shrink-0" />}
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-gray-900 dark:text-white">{ping.inZone ? 'Dans la zone' : 'Hors zone'}</div>
                  <div className="text-xs text-gray-500 truncate">
                    {ping.kind} · {ping.distanceMeters}m · {ping.latitude.toFixed(5)}, {ping.longitude.toFixed(5)}
                  </div>
                </div>
                <div className="text-xs text-gray-400 flex items-center gap-1 flex-shrink-0">
                  <Clock className="w-3 h-3" />
                  {new Date(ping.createdAt).toLocaleString('fr-FR')}
                </div>
              </div>
            ))}
          </div>
        )}
    </div>
  );
}
