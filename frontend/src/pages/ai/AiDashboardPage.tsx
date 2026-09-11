import { useEffect, useState } from "react";
import api from "@/lib/api";

interface AiSummary {
  tenantId?: string;
  totalSouls?: number;
  totalFamilies?: number;
  growthPrediction?: { value: number; unit: string; explanation: string };
}

interface KpiNarrative {
  headline?: string;
  summary?: Record<string, number>;
  highlights?: string[];
  concerns?: string[];
  recommendations?: string[];
}

export default function AiDashboardPage() {
  const [summary, setSummary] = useState<AiSummary | null>(null);
  const [narrative, setNarrative] = useState<KpiNarrative | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => { fetchAiData(); }, []);

  const fetchAiData = async () => {
    try {
      const [s, n] = await Promise.all([
        api.get("/ai/module/summary").catch(() => null),
        api.get("/ai/module/kpi-narrative").catch(() => null),
      ]);
      if (s?.data) setSummary(s.data);
      if (n?.data) setNarrative(n.data);
    } catch (e) { console.error("IA error:", e); }
    finally { setLoading(false); }
  };

  if (loading) return <div className="flex justify-center py-16"><div className="animate-spin h-12 w-12 border-b-2 border-indigo-600 rounded-full"></div></div>;

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center gap-3">
        <div className="p-2 rounded-lg bg-gradient-to-br from-purple-500 to-indigo-600 text-white">🤖</div>
        <div>
          <h1 className="text-2xl font-bold">Intelligence Artificielle</h1>
          <p className="text-sm text-gray-500">Analyses et recommandations pour votre ministère</p>
        </div>
      </div>

      {narrative?.headline && (
        <div className="rounded-xl bg-gradient-to-r from-indigo-500 to-purple-600 p-6 text-white">
          <p className="text-lg font-semibold">{narrative.headline}</p>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="rounded-xl bg-white dark:bg-gray-800 border p-5">
          <p className="text-xs font-medium text-gray-500 uppercase">Âmes</p>
          <p className="text-2xl font-bold mt-1">{summary?.totalSouls ?? "—"}</p>
        </div>
        <div className="rounded-xl bg-white dark:bg-gray-800 border p-5">
          <p className="text-xs font-medium text-gray-500 uppercase">Familles</p>
          <p className="text-2xl font-bold mt-1">{summary?.totalFamilies ?? "—"}</p>
        </div>
        <div className="rounded-xl bg-white dark:bg-gray-800 border p-5">
          <p className="text-xs font-medium text-gray-500 uppercase">Prédiction Croissance</p>
          <p className="text-2xl font-bold mt-1">{summary?.growthPrediction?.value ?? "—"}</p>
        </div>
      </div>

      {narrative && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="rounded-xl bg-white dark:bg-gray-800 border p-5">
            <h3 className="font-semibold mb-3">Points forts</h3>
            <ul className="space-y-2">{(narrative.highlights ?? []).map((h,i) => <li key={i} className="text-sm">{h}</li>)}</ul>
          </div>
          <div className="rounded-xl bg-white dark:bg-gray-800 border p-5">
            <h3 className="font-semibold mb-3">Vigilances</h3>
            <ul className="space-y-2">{(narrative.concerns ?? []).map((c,i) => <li key={i} className="text-sm text-red-600">{c}</li>)}</ul>
          </div>
        </div>
      )}

      {narrative?.recommendations && (
        <div className="rounded-xl bg-white dark:bg-gray-800 border p-5">
          <h3 className="font-semibold mb-3">Recommandations IA</h3>
          <ul className="space-y-2">{(narrative.recommendations ?? []).map((r,i) => <li key={i} className="text-sm">→ {r}</li>)}</ul>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <a href="/ai/predictions" className="rounded-xl bg-white dark:bg-gray-800 border p-5 hover:shadow-md">
          <h3 className="font-semibold">Prédictions</h3>
          <p className="text-sm text-gray-500 mt-1">Croissance, risque, engagement</p>
        </a>
        <a href="/ai/family-cohesion" className="rounded-xl bg-white dark:bg-gray-800 border p-5 hover:shadow-md">
          <h3 className="font-semibold">Cohésion Familiale</h3>
          <p className="text-sm text-gray-500 mt-1">Analyse de la santé des familles</p>
        </a>
        <a href="/ai/sermon" className="rounded-xl bg-white dark:bg-gray-800 border p-5 hover:shadow-md">
          <h3 className="font-semibold">Assistant Sermon</h3>
          <p className="text-sm text-gray-500 mt-1">Génération de plans de sermon</p>
        </a>
      </div>
    </div>
  );
}
