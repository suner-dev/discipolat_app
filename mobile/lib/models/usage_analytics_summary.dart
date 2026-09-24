class UsageAnalyticsSummary {
  UsageAnalyticsSummary({
    required this.totalEvents,
    required this.pageViews,
    required this.activeUsers,
    required this.averageDurationSeconds,
    required Map<String, num> topPages,
    required Map<String, num> byDevice,
  })  : topPages = Map.unmodifiable(topPages),
        byDevice = Map.unmodifiable(byDevice);

  final num? totalEvents;
  final num? pageViews;
  final num? activeUsers;
  final num? averageDurationSeconds;
  final Map<String, num> topPages;
  final Map<String, num> byDevice;

  factory UsageAnalyticsSummary.fromJson(dynamic json) {
    final map = _mapOrThrow(json);
    return UsageAnalyticsSummary(
      totalEvents: _number(map, const ['totalEvenements', 'totalEvents']),
      pageViews: _number(map, const ['pagesVues', 'pageViews']),
      activeUsers: _number(map, const ['utilisateursUniques', 'activeUsers']),
      averageDurationSeconds: _number(
        map,
        const [
          'dureeMoyenneSec',
          'averageDurationSeconds',
          'avgDurationSeconds'
        ],
      ),
      topPages: _topPages(_firstValue(map, const ['topPages', 'top_pages'])),
      byDevice: _numbers(
        _firstMap(map, const ['parAppareil', 'byDevice']) ?? const {},
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (totalEvents != null) 'totalEvents': totalEvents,
      if (pageViews != null) 'pageViews': pageViews,
      if (activeUsers != null) 'activeUsers': activeUsers,
      if (averageDurationSeconds != null)
        'averageDurationSeconds': averageDurationSeconds,
      'topPages': topPages,
      'byDevice': byDevice,
    };
  }

  double? percentageForDevice(String device) {
    if (!byDevice.containsKey(device)) return null;
    final total = byDevice.values.fold<num>(0, (sum, value) => sum + value);
    if (total <= 0 || byDevice[device] == null) return null;
    return byDevice[device]! / total * 100;
  }

  static Map<String, dynamic> _mapOrThrow(dynamic value) {
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    throw const FormatException('Usage analytics response must be an object');
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

  static num? _number(Map<String, dynamic> json, List<String> keys) {
    for (final key in keys) {
      if (!json.containsKey(key)) continue;
      final value = json[key];
      if (value is num) return value;
      if (value is String) return num.tryParse(value);
      return null;
    }
    return null;
  }

  static Map<String, num> _topPages(dynamic value) {
    if (value is Map) {
      return _numbers(Map<String, dynamic>.from(value));
    }
    if (value is! List) return const {};
    final result = <String, num>{};
    for (final item in value) {
      if (item is! Map) continue;
      final map = Map<String, dynamic>.from(item);
      final page = map['page'] ?? map['name'] ?? map['path'];
      final views = _number(map, const ['views', 'count', 'value']);
      if (page is String && page.isNotEmpty && views != null) {
        result[page] = views;
      }
    }
    return result;
  }

  static Map<String, num> _numbers(Map<String, dynamic> json) {
    final result = <String, num>{};
    for (final entry in json.entries) {
      final value = _number(json, [entry.key]);
      if (value != null) result[entry.key] = value;
    }
    return result;
  }
}
