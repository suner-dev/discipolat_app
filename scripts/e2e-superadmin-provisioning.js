/**
 * E2E REEL — Flux Super Admin (API) : provisionnement guidé + isolation.
 *
 * Preuve que le flux fonctionne contre une VRAIE base et une VRAIE API en
 * conteneur (et non des mocks) :
 *   1. Connexion du Super Admin plateforme (compte seedé).
 *   2. Création d'un tenant via l'API plateforme.
 *   3. Église racine → département (nouveau responsable) → famille.
 *   4. Le Super Admin retrouve son tenant dans la liste.
 *   5. Isolation : un ADMIN d'église reçoit 403 sur l'API des tenants.
 *
 * Prérequis : stack docker up (db, api). Usage :
 *   node scripts/e2e-superadmin-provisioning.js
 */
const BASE = process.env.E2E_API || 'http://localhost:8081/api/v1';
const SUPER = {
  email: process.env.E2E_SUPERMAIL || 'superadmin@discipolat.com',
  password: process.env.E2E_SUPERPWD || 'password123',
};
const CHURCH_ADMIN = {
  email: process.env.E2E_ADMINMAIL || 'admin@discipolat.com',
  password: process.env.E2E_ADMINPWD || 'password123',
};
const STAMP = Date.now().toString().slice(-6);

let failures = 0;
const ok = (m) => console.log(`  ✅ ${m}`);
const fail = (m, d) => { failures++; console.log(`  ❌ ${m} — ${d || ''}`); };

async function call(path, { method = 'GET', token, body } = {}) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  let data = null;
  const text = await res.text();
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  return { status: res.status, data };
}

async function login(who) {
  const res = await call('/auth/login', { method: 'POST', body: { email: who.email, password: who.password } });
  if (res.status !== 200 || !res.data?.accessToken) {
    throw new Error(`login ${who.email} -> ${res.status} ${JSON.stringify(res.data).slice(0, 160)}`);
  }
  return res.data.accessToken;
}

(async () => {
  console.log('\n== 1. Connexion Super Admin plateforme ==');
  let superToken;
  try {
    superToken = await login(SUPER);
    ok(`login ${SUPER.email}`);
  } catch (e) {
    fail('login Super Admin', e.message);
    console.log('\n💥 Impossible de poursuivre sans compte Super Admin.');
    process.exit(1);
  }

  console.log('\n== 2. Création du tenant ==');
  const tenantName = `Eglise E2E ${STAMP}`;
  const tenantSlug = `e2e-${STAMP}`;
  const tenant = await call('/platform/admin/tenants', {
    method: 'POST', token: superToken,
    body: { name: tenantName, slug: tenantSlug, plan: 'free', country: 'CM', currency: 'XAF', timezone: 'Africa/Douala', locale: 'fr' },
  });
  if (tenant.status !== 201 || !tenant.data?.id) {
    fail('création tenant', `${tenant.status} ${JSON.stringify(tenant.data).slice(0, 200)}`);
    process.exit(1);
  }
  const tenantId = tenant.data.id;
  ok(`tenant créé (${tenantId})`);

  console.log('\n== 3. Église racine ==');
  const church = await call('/platform/admin/provisioning/church', {
    method: 'POST', token: superToken, body: { tenantId, name: `Eglise E2E ${STAMP} principale` },
  });
  church.status === 201 && church.data?.id
    ? ok(`église créée (${church.data.id})`)
    : fail('création église', `${church.status} ${JSON.stringify(church.data).slice(0, 200)}`);

  console.log('\n== 4. Département (nouveau responsable) ==');
  const dept = await call('/platform/admin/provisioning/department', {
    method: 'POST', token: superToken,
    body: {
      tenantId, nom: `Dept E2E ${STAMP}`, description: 'Département de recette E2E',
      createNewResponsable: true,
      newRespFirstName: 'Resp', newRespLastName: 'E2E',
      newRespEmail: `resp.e2e.${STAMP}@discipolat.com`, newRespPhone: '+237600000000',
    },
  });
  dept.status === 201 && dept.data?.id
    ? ok(`département créé (${dept.data.id})`)
    : fail('création département', `${dept.status} ${JSON.stringify(dept.data).slice(0, 200)}`);

  console.log('\n== 5. Famille (nouveau chef) ==');
  const fam = await call('/platform/admin/provisioning/family', {
    method: 'POST', token: superToken,
    body: {
      tenantId, nom: `Famille E2E ${STAMP}`,
      createNewChef: true,
      newChefFirstName: 'Chef', newChefLastName: 'E2E',
      newChefEmail: `chef.e2e.${STAMP}@discipolat.com`, newChefPhone: '+237600000001',
    },
  });
  fam.status === 201 && fam.data?.id
    ? ok(`famille créée (${fam.data.id})`)
    : fail('création famille', `${fam.status} ${JSON.stringify(fam.data).slice(0, 200)}`);

  console.log('\n== 6. Le Super Admin voit son tenant dans la liste ==');
  const list = await call('/platform/admin/tenants?size=200', { token: superToken });
  const found = Array.isArray(list.data?.content) && list.data.content.some((t) => t.id === tenantId);
  list.status === 200 && found
    ? ok('le nouveau tenant apparaît dans /platform/admin/tenants')
    : fail('liste des tenants', `status=${list.status} found=${found}`);

  console.log('\n== 7. Isolation — un ADMIN d’église ne voit PAS les tenants ==');
  try {
    const adminToken = await login(CHURCH_ADMIN);
    ok(`login ${CHURCH_ADMIN.email} (rôle d'église)`);
    const forbidden = await call('/tenants', { token: adminToken });
    forbidden.status === 403
      ? ok('ADMIN d’église → 403 sur /tenants (isolation confirmée sur API réelle)')
      : fail('isolation admin d’église', `attendu 403, reçu ${forbidden.status}`);

    const forbiddenList = await call('/platform/admin/tenants?size=200', { token: adminToken });
    forbiddenList.status === 403
      ? ok('ADMIN d’église → 403 sur /platform/admin/tenants')
      : fail('isolation plateforme', `attendu 403, reçu ${forbiddenList.status}`);
  } catch (e) {
    fail('login admin d’église', e.message);
  }

  console.log('\n================ RESULTAT ================');
  if (failures === 0) console.log('🎉 Flow Super Admin E2E : TOUT PASSE');
  else console.log(`💥 ${failures} échec(s)`);
  console.log(`Tenant de recette: ${tenantId} (${tenantSlug})`);
  process.exit(failures ? 1 : 0);
})();

