import 'dart:async';
import 'package:flutter/foundation.dart';

/// Debounce utility for search inputs and API calls.
/// Prevents excessive API calls when the user is typing.
class Debouncer {
  final int milliseconds;
  Timer? _timer;

  Debouncer({this.milliseconds = 400});

  void run(VoidCallback action) {
    _timer?.cancel();
    _timer = Timer(Duration(milliseconds: milliseconds), action);
  }

  void dispose() => _timer?.cancel();
}

/// Simple in-memory cache for API responses.
/// Avoids redundant network calls for the same endpoint within [ttl].
class ApiCache {
  final Map<String, _CacheEntry> _cache = {};
  final Duration ttl;

  ApiCache({this.ttl = const Duration(minutes: 5)});

  T? get<T>(String key) {
    final entry = _cache[key];
    if (entry == null) return null;
    if (DateTime.now().difference(entry.timestamp) > ttl) {
      _cache.remove(key);
      return null;
    }
    return entry.value as T?;
  }

  void put<T>(String key, T value) {
    _cache[key] = _CacheEntry(value, DateTime.now());
  }

  void invalidate(String key) => _cache.remove(key);
  void invalidateAll() => _cache.clear();

  /// Invalidate all entries whose key starts with [prefix].
  void invalidatePrefix(String prefix) {
    _cache.keys.where((k) => k.startsWith(prefix)).toList().forEach(_cache.remove);
  }
}

class _CacheEntry {
  final dynamic value;
  final DateTime timestamp;
  _CacheEntry(this.value, this.timestamp);
}
