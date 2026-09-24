class QuotaMetric {
  const QuotaMetric({
    this.used,
    this.limit,
    this.percent,
    this.unlimited,
    this.hasUsed = false,
    this.hasLimit = false,
    this.hasPercent = false,
  });

  final num? used;
  final num? limit;
  final num? percent;
  final bool? unlimited;
  final bool hasUsed;
  final bool hasLimit;
  final bool hasPercent;

  factory QuotaMetric.fromJson(
    Map<String, dynamic> json, {
    num divisor = 1,
  }) {
    final rawLimit = _valueForKeys(json, const [
      'limit',
      'max',
      'quota',
      'limitMb',
      'maxMb',
    ]);
    final explicitUnlimited = _booleanForKeys(
      json,
      const ['unlimited', 'isUnlimited'],
    );
    return QuotaMetric(
      used: _scaledNumberForKeys(
          json,
          const [
            'used',
            'current',
            'currentValue',
            'value',
            'usedMb',
            'currentMb',
          ],
          divisor),
      limit: _scaledNumberForKeys(
          json,
          const [
            'limit',
            'max',
            'quota',
            'limitMb',
            'maxMb',
          ],
          divisor),
      percent:
          _numberForKeys(json, const ['percent', 'percentage', 'usagePercent']),
      unlimited: explicitUnlimited ??
          (rawLimit is String && rawLimit.toLowerCase() == 'unlimited'
              ? true
              : null),
      hasUsed: _containsAny(json, const [
        'used',
        'current',
        'currentValue',
        'value',
        'usedMb',
        'currentMb',
      ]),
      hasLimit: _containsAny(json, const [
        'limit',
        'max',
        'quota',
        'limitMb',
        'maxMb',
      ]),
      hasPercent:
          _containsAny(json, const ['percent', 'percentage', 'usagePercent']),
    );
  }

  bool get hasData => hasUsed || hasLimit || hasPercent;

  bool get isUnlimited => unlimited == true;

  double? get usagePercent {
    if (isUnlimited) return null;
    final limitValue = limit;
    if (limitValue == 0) return null;
    final value = percent;
    if (value != null) return value.toDouble();
    final usedValue = used;
    if (usedValue == null || limitValue == null || limitValue < 0) {
      return null;
    }
    return usedValue / limitValue * 100;
  }

  Map<String, dynamic> toJson() {
    return {
      if (hasUsed) 'used': used,
      if (hasLimit) 'limit': limit,
      if (unlimited != null) 'unlimited': unlimited,
      if (hasPercent || usagePercent != null) 'percent': usagePercent,
    };
  }

  static bool _containsAny(Map<String, dynamic> json, List<String> keys) {
    return keys.any(json.containsKey);
  }

  static num? _scaledNumberForKeys(
    Map<String, dynamic> json,
    List<String> keys,
    num divisor,
  ) {
    final value = _numberForKeys(json, keys);
    if (value == null || divisor == 1) return value;
    return value / divisor;
  }

  static num? _numberForKeys(Map<String, dynamic> json, List<String> keys) {
    final value = _valueForKeys(json, keys);
    if (value is num) return value;
    if (value is String) return num.tryParse(value);
    return null;
  }

  static bool? _booleanForKeys(
    Map<String, dynamic> json,
    List<String> keys,
  ) {
    final value = _valueForKeys(json, keys);
    if (value is bool) return value;
    if (value is num) return value != 0;
    if (value is String) {
      if (value.toLowerCase() == 'true' || value == '1') return true;
      if (value.toLowerCase() == 'false' || value == '0') return false;
    }
    return null;
  }

  static dynamic _valueForKeys(Map<String, dynamic> json, List<String> keys) {
    for (final key in keys) {
      if (json.containsKey(key)) return json[key];
    }
    return null;
  }
}

class QuotaUsage {
  QuotaUsage({required Map<String, QuotaMetric> metrics, this.analyticsEnabled})
      : metrics = Map.unmodifiable(metrics);

  static const resourceKeys = <String>[
    'users',
    'churches',
    'departments',
    'campuses',
    'groups',
    'storage',
    'aiRequests',
    'courses',
    'messages',
  ];

  final Map<String, QuotaMetric> metrics;
  final bool? analyticsEnabled;

  factory QuotaUsage.fromJson(dynamic json) {
    final root = _mapOrThrow(json);
    final unwrapped =
        _firstMap(root, const ['data', 'usage', 'quotas']) ?? root;
    final values = <String, QuotaMetric>{};
    final snapshot = _firstMap(unwrapped, const ['snapshot']) ??
        _firstMap(root, const ['snapshot']);

    for (final key in resourceKeys) {
      final snapshotMetric = _snapshotMetric(snapshot, key);
      if (snapshotMetric != null) {
        values[key] = snapshotMetric;
        continue;
      }
      final raw = _firstValue(unwrapped, _aliasesFor(key));
      if (raw is Map) {
        values[key] = QuotaMetric.fromJson(Map<String, dynamic>.from(raw));
      }
    }

    _readLegacy(unwrapped, values);
    if (!identical(unwrapped, root)) {
      _readLegacy(root, values);
    }

    return QuotaUsage(
      metrics: values,
      analyticsEnabled:
          _readAnalyticsEnabled(unwrapped) ?? _readAnalyticsEnabled(root),
    );
  }

  QuotaMetric? metric(String key) => metrics[key];

  Map<String, dynamic> toJson() {
    return {
      for (final entry in metrics.entries) entry.key: entry.value.toJson(),
      if (analyticsEnabled != null) 'analyticsEnabled': analyticsEnabled,
    };
  }

  static List<String> _aliasesFor(String key) {
    switch (key) {
      case 'aiRequests':
        return const ['aiRequests', 'ai_requests'];
      case 'storage':
        return const ['storage', 'storageMb', 'storage_mb'];
      case 'campuses':
        return const ['campuses', 'campus'];
      case 'groups':
        return const ['groups', 'group'];
      default:
        return [key, _snakeCase(key)];
    }
  }

  static List<String> _snapshotAliasesFor(String key) {
    switch (key) {
      case 'aiRequests':
        return const ['aiCredits', 'aiRequests', 'ai_requests'];
      case 'storage':
        return const ['storageBytes', 'storage'];
      default:
        return [key, _snakeCase(key)];
    }
  }

  static QuotaMetric? _snapshotMetric(
    Map<String, dynamic>? snapshot,
    String key,
  ) {
    if (snapshot == null) return null;
    for (final alias in _snapshotAliasesFor(key)) {
      final value = snapshot[alias];
      if (value is! Map) continue;
      final map = Map<String, dynamic>.from(value);
      final divisor =
          alias == 'storageBytes' || map['unit'] == 'BYTES' ? 1024 * 1024 : 1;
      return QuotaMetric.fromJson(map, divisor: divisor);
    }
    return null;
  }

  static void _readLegacy(
      Map<String, dynamic> json, Map<String, QuotaMetric> values) {
    const legacy = <String, List<String>>{
      'users': ['maxUsers', 'currentUsers'],
      'churches': ['maxChurches', 'currentChurches'],
      'departments': ['maxDepartments', 'currentDepartments'],
      'campuses': ['maxCampuses', 'currentCampuses'],
      'groups': ['maxGroups', 'currentGroups'],
      'storage': ['maxStorageMb', 'currentStorageMb'],
      'aiRequests': ['maxAiRequestsMonth', 'currentAiRequestsMonth'],
      'courses': ['maxCourses', 'currentCourses'],
      'messages': ['maxMessagesMonth', 'currentMessagesMonth'],
    };

    for (final entry in legacy.entries) {
      if (values.containsKey(entry.key)) continue;
      final hasMax = json.containsKey(entry.value[0]);
      final hasCurrent = json.containsKey(entry.value[1]);
      if (!hasMax && !hasCurrent) continue;
      final metric = <String, dynamic>{};
      if (hasCurrent) metric['used'] = json[entry.value[1]];
      if (hasMax) metric['limit'] = json[entry.value[0]];
      values[entry.key] = QuotaMetric.fromJson(metric);
    }
  }

  static bool? _readAnalyticsEnabled(Map<String, dynamic> json) {
    final direct =
        _firstValue(json, const ['analyticsEnabled', 'analytics_enabled']);
    if (direct is bool) return direct;
    if (direct is String) {
      if (direct.toLowerCase() == 'true') return true;
      if (direct.toLowerCase() == 'false') return false;
    }
    final settings = _firstMap(json, const ['settings']);
    if (settings == null) return null;
    return _readAnalyticsEnabled(settings);
  }

  static Map<String, dynamic> _mapOrThrow(dynamic value) {
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    throw const FormatException('Quota usage response must be an object');
  }

  static Map<String, dynamic>? _firstMap(
    Map<String, dynamic> json,
    List<String> keys,
  ) {
    for (final key in keys) {
      final value = json[key];
      if (value is Map) return Map<String, dynamic>.from(value);
    }
    return null;
  }

  static dynamic _firstValue(Map<String, dynamic> json, List<String> keys) {
    for (final key in keys) {
      if (json.containsKey(key)) return json[key];
    }
    return null;
  }

  static String _snakeCase(String value) {
    return value.replaceAllMapped(
      RegExp('[A-Z]'),
      (match) => '_${match.group(0)!.toLowerCase()}',
    );
  }
}
