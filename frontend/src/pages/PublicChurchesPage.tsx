import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import LandingNavbar from '@/components/landing/LandingNavbar';
import { CTAFinal, Footer } from '@/components/landing/CTAFooter';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import { Building2, Globe, MapPin, Search } from 'lucide-react';

interface PublicChurch { name: string; city?: string; country?: string; denomination?: string; website?: string; description?: string; }
const API_BASE = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_URL ?? '';

/** G6.9 — Annuaire public « Eglises sur Discipolat » (opt-in individuel, aucune PII). */
export default function PublicChurchesPage() {
  const [churches, setChurches] = useState<PublicChurch[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [q, setQ] = useState('');
  const [country, setCountry] = useState('');
  const load = async () => {
    try {
      setLoading(true); setError('');
      const p = new URLSearchParams();
      if (q.trim()) p.set('q', q.trim());
      if (country.trim()) p.set('country', country.trim());
      const res = await fetch(`${API_BASE}/api/v1/public/churches${p.toString() ? `?${p}` : ''}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setChurches(data.content ?? []); setTotal(data.total ?? 0);
    } catch { setError('Annuaire indisponible. Reessayez plus tard.'); setChurches([]); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);
  const go = (id: string) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  return (
    <div className="min-h-screen">
      <LandingNavbar onNavigate={go} onDemo={() => {}} />
      <main className="max-w-6xl mx-auto px-4 pt-24 pb-16">
        <div className="text-center mb-10">
          <h1 className="text-3xl font-bold">Eglises sur Discipolat</h1>
          <p className="mt-3 text-sm text-gray-500 max-w-2xl mx-auto">
            Chaque eglise choisit d&apos;apparaitre ici (opt-in reversible).{' '}
            <Link to="/register" className="underline font-medium">Rejoignez le reseau</Link>.
          </p>
          <p className="mt-2 text-xs text-gray-400">{total} eglise(s) referencee(s)</p>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 mb-8">
          <label className="flex items-center gap-2 flex-1 border rounded-xl px-3 py-2">
            <Search className="w-4 h-4" />
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Rechercher…" className="w-full bg-transparent outline-none text-sm" />
          </label>
          <label className="flex items-center gap-2 border rounded-xl px-3 py-2 sm:w-64">
            <MapPin className="w-4 h-4" />
            <input value={country} onChange={(e) => setCountry(e.target.value)} placeholder="Pays" className="w-full bg-transparent outline-none text-sm" />
          </label>
          <button onClick={load} className="px-5 py-2 rounded-xl bg-primary-600 text-white text-sm">Filtrer</button>
        </div>
        {loading ? <SkeletonLoader lines={6} /> : error ? <EmptyState title="Indisponible" message={error} />
        : churches.length === 0 ? <EmptyState title="Aucune eglise publiee" message="Soyez la premiere eglise du reseau." action={{ label: "Rejoindre", onClick: () => { window.location.href = '/register'; }} } />
        : <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {churches.map((c, i) => (
            <article key={`${c.name}-${i}`} className="rounded-2xl border p-5 flex flex-col gap-2">
              <h2 className="font-bold flex items-center gap-2"><Building2 className="w-5 h-5" /> {c.name}</h2>
              {(c.city || c.country) && <p className="text-xs text-gray-500">{[c.city, c.country].filter(Boolean).join(', ')}</p>}
              {c.description && <p className="text-sm line-clamp-4">{c.description}</p>}
              <div className="mt-auto pt-3 flex gap-3">
                {c.website && <a href={c.website} target="_blank" rel="noreferrer" className="text-xs underline flex gap-1"><Globe className="w-3.5 h-3.5" /> Site</a>}
                <Link to="/register" className="text-xs underline">Rejoindre</Link>
              </div>
            </article>
          ))}
        </div>}
      </main>
      <CTAFinal onDemo={() => {}} />
      <Footer />
    </div>
  );
}
