// A5 #5 — Enveloppe CI du scénario de charge EXISTANT (k6-load-test.js).
//
// Pourquoi une enveloppe et pas le fichier brut ?
//   * k6-load-test.js conserve ses seuils internes STRICTS (p95<500, p99<1000)
//     pour les campagnes manuelles de pré-release — on ne les affaiblit pas.
//   * La porte exigée par l'orchestration pour la CI est : P95 > 2000 ms →
//     échec. Ce wrapper réimporte le scénario (aucune duplication de logique,
//     le scénario testé reste celui du dépôt) et redéclare des stages courts
//     + le seuil de la porte, pour tenir dans les minutes gratuites d'un run.
//
// Exécution (CI ou local) :
//   BASE_URL=https://discipolat-beta-api.onrender.com JWT_TOKEN=... \
//     k6 run performance-tests/k6-ci-gate.js --summary-export=k6-summary.json
import scenario from './k6-load-test.js';

export const options = {
  stages: [
    { duration: '15s', target: 5 },   // montée
    { duration: '45s', target: 20 },  // palier de tenue (20 VU ≈ 1 % du pic applicatif)
    { duration: '10s', target: 0 },   // redescente
  ],
  thresholds: {
    // LA porte bloquante de l'orchestration : si P95 > 2000 ms → échec.
    http_req_duration: ['p(95)<2000'],
    // Garde-fou minimal : une cible qui renvoie surtout des erreurs n'est
    // pas « rapide », elle est morte. 5 % toléré (certains endpoints
    // d'admin comme push-broadcast peuvent être restreints selon la cible).
    http_req_failed: ['rate<0.05'],
  },
};

export default scenario;
