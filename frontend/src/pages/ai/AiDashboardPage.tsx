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

interface ProviderStatus {
  groq: boolean;
  gemini: boolean;
  mistral: boolean;
  huggingface: boolean;
  fallback: boolean;
}

export default function AiDashboardPage() {
  const [summary, setSummary] = useState<AiSummary | null>(null);
  const [narrative, setNarrative] = useState<KpiNarrative | null>(null);
  const [providers, setProviders] = useState<ProviderStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [chatMessage, setChatMessage] = useState("");
  const [chatResponse, setChatResponse] = useState("");
  const [chatLoading, setChatLoading] = useState(false);

  useEffect(() => { fetchAiData(); }, []);

  const fetchAiData = async () => {
    try {
      const [s, n, p] = await Promise.all([
        api.get("/ai/module/summary").catch(() => null),
        api.get("/ai/module/kpi-narrative").catch(() => null),
        api.get("/ai/module/providers").catch(() => null),
      ]);
      if (s?.data) setSummary(s.data);
      if (n?.data) setNarrative(n.data);
      if (p?.data?.providers) setProviders(p.data.providers);
    } catch (e) { console.error("IA error:", e); }
    finally { setLoading(false); }
  };

  const sendChat = async () => {
    if (!chatMessage.trim() || chatLoading) return;
    setChatLoading(true);
    setChatResponse("");
    try {
      const res = await api.post("/ai/module/chat", { message: chatMessage });
      setChatResponse(res.data?.response || "Pas de réponse");
      setChatMessage("");
    } catch (e) {
      setChatResponse("Erreur: " + (e as Error).message);
    } finally {
      setChatLoading(false);
    }
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

      {/* Provider Status */}
      <div className="rounded-xl bg-white dark:bg-gray-800 border p-5">
        <h3 className="text-sm font-semibold mb-3">Modèles IA connectés (gratuits)</h3>
        <div className="flex flex-wrap gap-2">
          {providers && Object.entries(providers).map(([key, val]) => (
            <span key={key} className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium ${
              val ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"
            }`}>
              <span className={`w-2 h-2 rounded-full ${val ? "bg-green-500" : "bg-gray-400"}`} />
              {key === "groq" ? "Groq (Llama 3.1 70B)" :
               key === "gemini" ? "Gemini 1.5 Flash" :
               key === "mistral" ? "Mistral 7B" :
               key === "huggingface" ? "HuggingFace (Llama 3/Qwen)" :
               "Mode local (gratuit)"}
              {val && " ✓"}
            </span>
          ))}
        </div>
        <p className="text-xs text-gray-400 mt-3">
          Tous les modèles sont gratuits sans carte bancaire. Configurez une clé via les variables d'environnement
          GROQ_API_KEY, GEMINI_API_KEY, MISTRAL_API_KEY, HUGGINGFACE_API_KEY.
        </p>
      </div>

      {/* AI Chat */}
      <div className="rounded-xl bg-white dark:bg-gray-800 border p-5">
        <h3 className="text-sm font-semibold mb-3">Assistant IA (chat)</h3>
        <div className="flex gap-2">
          <input
            value={chatMessage}
            onChange={(e) => setChatMessage(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && sendChat()}
            placeholder="Posez une question à l'assistant IA..."
            className="flex-1 px-4 py-2 border rounded-lg bg-gray-50 dark:bg-gray-900 dark:border-gray-700 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <button
            onClick={sendChat}
            disabled={chatLoading || !chatMessage.trim()}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
          >
            {chatLoading ? "..." : "Envoyer"}
          </button>
        </div>
        {chatResponse && (
          <div className="mt-4 p-4 rounded-lg bg-indigo-50 dark:bg-indigo-900/20 whitespace-pre-wrap text-sm">
            {chatResponse}
          </div>
        )}
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
