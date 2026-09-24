export interface QuotaUsageMetric {
  key: string;
  label: string;
  usage: number | null;
  limit: number | null;
  percent: number | null;
  source: string | null;
  enforced: boolean | null;
  unit: string | null;
  available: boolean;
  unlimited: boolean;
}

export interface QuotaUsage {
  tenantId: string | null;
  planKey: string | null;
  planName: string | null;
  metrics: QuotaUsageMetric[];
}

type UnknownRecord = Record<string, unknown>;

type MetricDefinition = {
  label: string;
};

const METRIC_DEFINITIONS: Record<string, MetricDefinition> = {
  users: { label: 'Utilisateurs' },
  churches: { label: 'Églises' },
  departments: { label: 'Départements' },
  campuses: { label: 'Campus' },
  groups: { label: 'Groupes' },
  storage: { label: 'Stockage' },
  aiRequests: { label: 'Crédits IA' },
  courses: { label: 'Cours' },
  messages: { label: 'Messages / mois' },
};

const LEGACY_PLACEHOLDER_METRICS = new Set(['storage', 'aiRequests', 'courses', 'messages']);

const isRecord = (value: unknown): value is UnknownRecord => (
  typeof value === 'object' && value !== null && !Array.isArray(value)
);

const firstDefined = (record: UnknownRecord, keys: string[]): unknown => {
  for (const key of keys) {
    if (record[key] !== undefined && record[key] !== null) {
      return record[key];
    }
  }
  return undefined;
};

const toNumber = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '') {
    const normalized = value.trim().replace('%', '');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
};

const toBoolean = (value: unknown): boolean | null => {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  if (typeof value === 'string') {
    if (value.toLowerCase() === 'true' || value === '1') return true;
    if (value.toLowerCase() === 'false' || value === '0') return false;
  }
  return null;
};

const toStringValue = (value: unknown): string | null => {
  if (typeof value === 'string' && value.trim() !== '') return value;
  if (typeof value === 'number' && Number.isFinite(value)) return String(value);
  if (isRecord(value)) {
    const nested = firstDefined(value, ['name', 'key', 'label', 'type']);
    if (nested !== undefined) return toStringValue(nested);
  }
  return null;
};

const parseRecord = (value: unknown): UnknownRecord | null => {
  if (isRecord(value)) return value;
  if (typeof value !== 'string' || value.trim() === '') return null;
  try {
    const parsed: unknown = JSON.parse(value);
    return isRecord(parsed) ? parsed : null;
  } catch {
    return null;
  }
};

const canonicalMetricKey = (key: string): string | null => {
  const compact = key.toLowerCase().replace(/[^a-z0-9]/g, '');
  if (compact.includes('user')) return 'users';
  if (compact.includes('church')) return 'churches';
  if (compact.includes('department')) return 'departments';
  if (compact.includes('campus')) return 'campuses';
  if (compact.includes('group')) return 'groups';
  if (compact.includes('storage')) return 'storage';
  if (compact.includes('ai') && (compact.includes('request') || compact.includes('credit'))) return 'aiRequests';
  if (compact.includes('course')) return 'courses';
  if (compact.includes('message')) return 'messages';
  return null;
};

const metricLabel = (key: string, raw: unknown): string => {
  if (key === 'aiRequests') return METRIC_DEFINITIONS[key].label;
  if (isRecord(raw)) {
    const label = toStringValue(raw.label);
    if (label) return label;
  }
  return METRIC_DEFINITIONS[key]?.label ?? key;
};

const metricValue = (raw: unknown): Omit<QuotaUsageMetric, 'key' | 'label'> => {
  const record = isRecord(raw) ? raw : { usage: raw };
  const usageRecord = isRecord(record.usage) ? record.usage : undefined;
  const sourceRecord = isRecord(record.source) ? record.source : undefined;
  const usage = toNumber(firstDefined(record, ['used', 'usedMb', 'usage', 'current', 'currentMb', 'count', 'value']));
  const nestedUsage = usage === null && usageRecord
    ? toNumber(firstDefined(usageRecord, ['used', 'usedMb', 'usage', 'current', 'currentMb', 'count', 'value']))
    : usage;
  const rawLimit = firstDefined(record, ['limit', 'max', 'limitMb', 'maxMb', 'maximum']);
  const nestedLimit = rawLimit === undefined && usageRecord
    ? firstDefined(usageRecord, ['limit', 'max', 'limitMb', 'maxMb', 'maximum'])
    : rawLimit;
  const limitValue = toNumber(nestedLimit);
  const rawPercent = firstDefined(record, ['percent', 'percentage', 'utilization', 'utilizationPercent']);
  const nestedPercent = rawPercent === undefined && usageRecord
    ? firstDefined(usageRecord, ['percent', 'percentage', 'utilization', 'utilizationPercent'])
    : rawPercent;
  const rawEnforced = firstDefined(record, ['enforced', 'isEnforced', 'enforcement', 'serverEnforced']);
  const nestedEnforced = rawEnforced === undefined && usageRecord
    ? firstDefined(usageRecord, ['enforced', 'isEnforced', 'enforcement', 'serverEnforced'])
    : rawEnforced;
  const rawAvailable = firstDefined(record, ['available', 'isAvailable']);
  const nestedAvailable = rawAvailable === undefined && usageRecord
    ? firstDefined(usageRecord, ['available', 'isAvailable'])
    : rawAvailable;
  const rawUnlimited = firstDefined(record, ['unlimited', 'isUnlimited']);
  const source = toStringValue(sourceRecord ?? record.source ?? record.dataSource ?? record.origin);
  const percentValue = toNumber(nestedPercent);
  const calculatedPercent = percentValue !== null
    ? percentValue
    : nestedUsage !== null && limitValue !== null && limitValue > 0
      ? (nestedUsage * 100) / limitValue
      : null;
  const unlimited = toBoolean(rawUnlimited) ?? (toStringValue(rawLimit)?.toLowerCase() === 'unlimited');

  return {
    usage: nestedUsage,
    limit: limitValue,
    percent: unlimited ? null : calculatedPercent,
    source,
    enforced: toBoolean(nestedEnforced),
    unit: toStringValue(firstDefined(record, ['unit', 'metricUnit'])),
    available: nestedUsage !== null && toBoolean(nestedAvailable) !== false,
    unlimited,
  };
};

const collectMetricEntries = (value: unknown, target: Map<string, unknown>): void => {
  if (Array.isArray(value)) {
    value.forEach((entry) => {
      if (!isRecord(entry)) return;
      const key = toStringValue(firstDefined(entry, ['key', 'metric', 'resource', 'name']));
      if (!key) return;
      const canonical = canonicalMetricKey(key);
      if (canonical) target.set(canonical, entry);
    });
    return;
  }
  if (!isRecord(value)) return;
  Object.entries(value).forEach(([key, raw]) => {
    const canonical = canonicalMetricKey(key);
    if (canonical) target.set(canonical, raw);
  });
};

const collectLimitEntries = (value: unknown, target: Map<string, unknown>): void => {
  const record = parseRecord(value);
  if (!record) return;
  Object.entries(record).forEach(([key, raw]) => {
    const canonical = canonicalMetricKey(key);
    if (canonical) target.set(canonical, raw);
  });
};

const readMetadata = (root: UnknownRecord): { tenantId: string | null; planKey: string | null; planName: string | null } => {
  const plan = isRecord(root.plan) ? root.plan : undefined;
  const tenant = isRecord(root.tenant) ? root.tenant : undefined;
  const subscription = isRecord(root.subscription) ? root.subscription : undefined;
  const subscriptionPlan = subscription && isRecord(subscription.plan) ? subscription.plan : undefined;
  return {
    tenantId: toStringValue(root.tenantId ?? tenant?.id),
    planKey: toStringValue(root.planKey ?? plan?.key ?? plan?.canonicalKey ?? subscription?.planKey),
    planName: toStringValue(root.planName ?? (plan ? plan.name : root.plan) ?? subscriptionPlan?.name),
  };
};

export function normalizeQuotaUsage(payload: unknown): QuotaUsage {
  const root = isRecord(payload) ? payload : {};
  const metricValues = new Map<string, unknown>();
  const limitValues = new Map<string, unknown>();
  const containers = [root.snapshot, root.metrics, root.usage, root.usages, root.quotas, root.resources, root.data];
  const hasStructuredUsage = containers.some((value) => Array.isArray(value) || isRecord(value));
  const dataRecord = isRecord(root.data) ? root.data : undefined;
  const snapshot = isRecord(root.snapshot) ? root.snapshot : undefined;
  const configuration = isRecord(root.configuration) ? root.configuration : undefined;
  const policy = isRecord(root.policy) ? root.policy : undefined;
  const plan = isRecord(root.plan) ? root.plan : undefined;

  collectMetricEntries(root, metricValues);
  containers.forEach((container) => {
    collectMetricEntries(container, metricValues);
    if (isRecord(container)) collectLimitEntries(container.limits, limitValues);
  });
  collectLimitEntries(root.limits, limitValues);
  collectLimitEntries(root.planLimits, limitValues);
  collectLimitEntries(configuration?.limits, limitValues);
  collectLimitEntries(policy?.limits, limitValues);
  if (plan) collectLimitEntries(plan.limits, limitValues);
  if (snapshot) collectLimitEntries(snapshot.limits, limitValues);
  if (dataRecord) {
    collectMetricEntries(dataRecord, metricValues);
    collectLimitEntries(dataRecord.limits, limitValues);
  }

  const keys = new Set<string>([
    ...Object.keys(METRIC_DEFINITIONS),
    ...metricValues.keys(),
    ...limitValues.keys(),
  ]);
  const metrics = Array.from(keys).reduce<QuotaUsageMetric[]>((result, key) => {
    const rawMetric = metricValues.get(key);
    const limitOnly = limitValues.get(key);
    if (rawMetric === undefined && limitOnly === undefined) return result;
    const metric = metricValue(rawMetric);
    const limitOnlyValue = isRecord(limitOnly) ? metricValue(limitOnly).limit : toNumber(limitOnly);
    const limit = metric.limit ?? limitOnlyValue;
    const legacyPlaceholder = !hasStructuredUsage && LEGACY_PLACEHOLDER_METRICS.has(key);
    const sourceSignal = metric.source !== null || metric.enforced !== null;
    const sourceUnavailable = metric.source !== null && /unavailable|not[_-]?tracked|unknown/i.test(metric.source);
    const available = !legacyPlaceholder && !sourceUnavailable && (metric.available || sourceSignal) && metric.usage !== null;

    result.push({
      key,
      label: metricLabel(key, rawMetric),
      usage: metric.usage,
      limit,
      percent: available ? metric.percent : null,
      source: metric.source,
      enforced: metric.enforced,
      unit: metric.unit,
      available,
      unlimited: metric.unlimited,
    });
    return result;
  }, []);

  const metadata = readMetadata(root);
  const snapshotMetadata = snapshot ? readMetadata(snapshot) : null;

  return {
    tenantId: metadata.tenantId ?? snapshotMetadata?.tenantId ?? null,
    planKey: metadata.planKey ?? snapshotMetadata?.planKey ?? null,
    planName: metadata.planName ?? snapshotMetadata?.planName ?? null,
    metrics,
  };
}
