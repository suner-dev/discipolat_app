import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TENANT_ID = __ENV.TENANT_ID || '00000000-0000-0000-0000-000000000001';
const JWT_TOKEN = __ENV.JWT_TOKEN || '';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up to 10 users
    { duration: '1m', target: 50 },    // Ramp up to 50 users
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '3m', target: 100 },   // Stay at 100 users
    { duration: '1m', target: 200 },   // Spike to 200 users
    { duration: '2m', target: 200 },   // Stay at 200 users
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // Latency budgets
    http_req_failed: ['rate<0.01'],                  // Error rate < 1%
    errors: ['rate<0.05'],                           // Custom error rate
  },
};

// Helper function for authenticated requests
function authHeaders() {
  return {
    'Authorization': `Bearer ${JWT_TOKEN}`,
    'Content-Type': 'application/json',
    'X-Tenant-ID': TENANT_ID,
  };
}

// Generate test data
function generatePerson() {
  const id = Math.random().toString(36).substring(7);
  return {
    email: `test${id}@perf.com`,
    firstName: 'Perf',
    lastName: `User${id}`,
    phone: `+331${Math.floor(Math.random() * 100000000).toString().padStart(8, '0')}`,
  };
}

// Main test function
export default function () {
  // Test 1: Church OS Dashboard - Global view
  testChurchOSDashboard();

  // Test 2: Space bootstrap (Department/Family OS)
  testSpaceBootstrap();

  // Test 3: Global search
  testGlobalSearch();

  // Test 4: People directory with pagination
  testPeopleDirectory();

  // Test 4: Virtual list scrolling (large dataset)
  testVirtualList();

  // Test 5: Calendar with events
  testCalendar();

  // Test 6: Mobile sync simulation
  testMobileSync();

  // Test 7: Push notification load
  testPushNotifications();

  sleep(1);
}

function testChurchOSDashboard() {
  const res = http.get(`${BASE_URL}/api/v1/dashboard/church`, { headers: authHeaders() });
  const success = check(res, {
    'Church OS Dashboard: status 200': (r) => r.status === 200,
    'Church OS Dashboard: has KPIs': (r) => r.json().kpis !== undefined,
    'Church OS Dashboard: response time < 500ms': (r) => r.timings.duration < 500,
  });
  errorRate.add(!success);
}

function testSpaceBootstrap() {
  // First get list of spaces
  const spacesRes = http.get(`${BASE_URL}/api/v1/spaces?type=DEPARTMENT`, { headers: authHeaders() });
  if (spacesRes.status !== 200) {
    errorRate.add(true);
    return;
  }

  const spaces = spacesRes.json();
  if (spaces.length === 0) {
    errorRate.add(true);
    return;
  }

  // Test bootstrap for first space
  const spaceId = spaces[0].id;
  const bootstrapRes = http.get(`${BASE_URL}/api/v1/spaces/${spaceId}/bootstrap`, { headers: authHeaders() });
  const success = check(bootstrapRes, {
    'Space Bootstrap: status 200': (r) => r.status === 200,
    'Space Bootstrap: has config': (r) => r.json().configuration !== undefined,
    'Space Bootstrap: has modules': (r) => r.json().modules !== undefined,
    'Space Bootstrap: response time < 1000ms': (r) => r.timings.duration < 1000,
  });
  errorRate.add(!success);
}

function testGlobalSearch() {
  const queries = ['Jean', 'Marie', 'Pierre', 'Accueil', 'Culte', 'Réunion'];
  const query = queries[Math.floor(Math.random() * queries.length)];

  const res = http.get(`${BASE_URL}/api/v1/search?q=${encodeURIComponent(query)}&size=20`, { headers: authHeaders() });
  const success = check(res, {
    'Global Search: status 200': (r) => r.status === 200,
    'Global Search: has results': (r) => Array.isArray(r.json().results),
    'Global Search: response time < 300ms': (r) => r.timings.duration < 300,
  });
  errorRate.add(!success);
}

function testPeopleDirectory() {
  const res = http.get(`${BASE_URL}/api/v1/people?page=0&size=50&sansEspace=false`, { headers: authHeaders() });
  const success = check(res, {
    'People Directory: status 200': (r) => r.status === 200,
    'People Directory: has content': (r) => Array.isArray(r.json().content),
    'People Directory: pagination works': (r) => r.json().totalElements !== undefined,
    'People Directory: response time < 300ms': (r) => r.timings.duration < 300,
  });
  errorRate.add(!success);
}

function testVirtualList() {
  // Test large paginated list
  const res = http.get(`${BASE_URL}/api/v1/people?page=0&size=100&sort=lastName,asc`, { headers: authHeaders() });
  const success = check(res, {
    'Virtual List: status 200': (r) => r.status === 200,
    'Virtual List: 100 items returned': (r) => r.json().content.length <= 100,
    'Virtual List: response time < 400ms': (r) => r.timings.duration < 400,
  });
  errorRate.add(!success);
}

function testCalendar() {
  const now = new Date();
  const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0];

  const res = http.get(`${BASE_URL}/api/v1/events/calendar?from=${from}&to=${to}`, { headers: authHeaders() });
  const success = check(res, {
    'Calendar: status 200': (r) => r.status === 200,
    'Calendar: has events': (r) => Array.isArray(r.json()),
    'Calendar: response time < 500ms': (r) => r.timings.duration < 500,
  });
  errorRate.add(!success);
}

function testMobileSync() {
  // Simulate mobile sync payload
  const payload = JSON.stringify({
    lastSync: new Date(Date.now() - 3600000).toISOString(),
    deviceId: `mobile-${__VU}`,
    changes: [
      { type: 'PERSON', action: 'CREATE', data: generatePerson() },
      { type: 'EVENT', action: 'UPDATE', data: { id: 'test', title: 'Updated' } },
    ],
  });

  const res = http.post(`${BASE_URL}/api/v1/sync/push`, payload, { headers: authHeaders() });
  const success = check(res, {
    'Mobile Sync Push: status 200': (r) => r.status === 200,
    'Mobile Sync Push: has syncResult': (r) => r.json().synced !== undefined,
    'Mobile Sync Push: response time < 2000ms': (r) => r.timings.duration < 2000,
  });
  errorRate.add(!success);

  // Test pull
  const pullRes = http.get(`${BASE_URL}/api/v1/sync/pull?since=${new Date(Date.now() - 3600000).toISOString()}`, { headers: authHeaders() });
  check(pullRes, {
    'Mobile Sync Pull: status 200': (r) => r.status === 200,
  });
}

function testPushNotifications() {
  // Simulate sending push to 1000 users (test endpoint)
  const res = http.post(`${BASE_URL}/api/v1/admin/test/push-broadcast`, JSON.stringify({
    title: 'Test Notification',
    body: 'Performance test broadcast',
    tenantId: TENANT_ID,
    targetAll: true,
  }), { headers: authHeaders() });

  check(res, {
    'Push Broadcast: status 200': (r) => r.status === 200,
    'Push Broadcast: response time < 5000ms': (r) => r.timings.duration < 5000,
  });
}

// Teardown - cleanup test data
export function teardown(data) {
  // Cleanup if needed
  console.log('Performance test completed');
}