import 'dart:async';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import '../../../data/services/api_service.dart';

/// Geofencing attendance screen — GPS-based check-in/out.
/// Shows distance to church, check-in status, and real-time GPS tracking.
class GeofencingScreen extends StatefulWidget {
  const GeofencingScreen({super.key});

  @override
  State<GeofencingScreen> createState() => _GeofencingScreenState();
}

class _GeofencingScreenState extends State<GeofencingScreen> {
  final _api = ApiService();
  Position? _currentPosition;
  Map<String, dynamic>? _geofenceConfig;
  bool _isWithinGeofence = false;
  bool _isCheckedIn = false;
  bool _isLoading = true;
  bool _isTracking = false;
  StreamSubscription<Position>? _positionStream;
  Timer? _updateTimer;
  double _distanceToChurch = 0;

  @override
  void initState() {
    super.initState();
    _loadConfig();
  }

  @override
  void dispose() {
    _positionStream?.cancel();
    _updateTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadConfig() async {
    setState(() => _isLoading = true);
    try {
      final res = await _api.get('/geofencing/config');
      _geofenceConfig = res.data;
      await _getCurrentLocation();
    } catch (e) {
      // Use default config
      _geofenceConfig = {
        'enabled': true,
        'latitude': 48.8566,
        'longitude': 2.3522,
        'radiusMeters': 200,
        'churchName': 'Église',
      };
      await _getCurrentLocation();
    }
  }

  Future<void> _getCurrentLocation() async {
    try {
      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
        if (permission == LocationPermission.denied) {
          setState(() => _isLoading = false);
          return;
        }
      }

      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
        ),
      );

      setState(() {
        _currentPosition = position;
        _isLoading = false;
      });

      _checkGeofence();
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  void _checkGeofence() {
    if (_currentPosition == null || _geofenceConfig == null) return;

    final churchLat = (_geofenceConfig!['latitude'] ?? 0).toDouble();
    final churchLng = (_geofenceConfig!['longitude'] ?? 0).toDouble();
    final radius = (_geofenceConfig!['radiusMeters'] ?? 200).toDouble();

    final distance = Geolocator.distanceBetween(
      _currentPosition!.latitude,
      _currentPosition!.longitude,
      churchLat,
      churchLng,
    );

    setState(() {
      _distanceToChurch = distance;
      _isWithinGeofence = distance <= radius;
    });
  }

  void _startTracking() {
    setState(() => _isTracking = true);
    _positionStream = Geolocator.getPositionStream(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: 50, // Update every 50 meters
      ),
    ).listen((position) {
      setState(() => _currentPosition = position);
      _checkGeofence();

      // Auto check-in when entering geofence
      if (_isWithinGeofence && !_isCheckedIn) {
        _checkIn();
      }
      // Auto check-out when leaving
      if (!_isWithinGeofence && _isCheckedIn) {
        _checkOut();
      }
    });

    // Periodic updates
    _updateTimer = Timer.periodic(const Duration(seconds: 30), (_) => _getCurrentLocation());
  }

  void _stopTracking() {
    _positionStream?.cancel();
    _updateTimer?.cancel();
    setState(() => _isTracking = false);
  }

  Future<void> _checkIn() async {
    try {
      await _api.post('/geofencing/check-in', data: {
        'latitude': _currentPosition?.latitude,
        'longitude': _currentPosition?.longitude,
        'accuracy': _currentPosition?.accuracy,
      });
      setState(() => _isCheckedIn = true);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('✅ Présence enregistrée'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _checkOut() async {
    try {
      await _api.post('/geofencing/check-out');
      setState(() => _isCheckedIn = false);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final churchName = _geofenceConfig?['churchName'] ?? 'Église';
    final radius = (_geofenceConfig?['radiusMeters'] ?? 200).toDouble();

    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('📍 Géofencing', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : ListView(
              padding: const EdgeInsets.all(20),
              children: [
                // Status card
                _buildStatusCard(),
                const SizedBox(height: 20),

                // Distance indicator
                _buildDistanceIndicator(radius),
                const SizedBox(height: 20),

                // Church info
                _buildChurchInfo(churchName, radius),
                const SizedBox(height: 20),

                // Action buttons
                _buildActionButtons(),
                const SizedBox(height: 20),

                // GPS info
                _buildGPSInfo(),
              ],
            ),
    );
  }

  Widget _buildStatusCard() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _isCheckedIn
            ? Colors.green.withAlpha(15)
            : _isWithinGeofence
                ? Colors.amber.withAlpha(15)
                : Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: _isCheckedIn
              ? Colors.green.withAlpha(60)
              : _isWithinGeofence
                  ? Colors.amber.withAlpha(60)
                  : Colors.white.withAlpha(15),
        ),
      ),
      child: Column(
        children: [
          Container(
            width: 80,
            height: 80,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: _isCheckedIn ? Colors.green.withAlpha(30) : Colors.white.withAlpha(10),
              border: Border.all(
                color: _isCheckedIn ? Colors.green : Colors.white.withAlpha(30),
                width: 3,
              ),
            ),
            child: Icon(
              _isCheckedIn ? Icons.check : Icons.location_on,
              color: _isCheckedIn ? Colors.green : Colors.white.withAlpha(100),
              size: 40,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            _isCheckedIn
                ? 'Présent ✓'
                : _isWithinGeofence
                    ? 'Dans la zone'
                    : 'Hors de la zone',
            style: TextStyle(
              color: _isCheckedIn ? Colors.green : _isWithinGeofence ? Colors.amber : Colors.white,
              fontSize: 22,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            _isCheckedIn
                ? 'Votre présence a été enregistrée'
                : _isWithinGeofence
                    ? 'Appuyez sur "Pointer" pour enregistrer votre présence'
                    : 'Rapprochez-vous de l\'église',
            style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 13),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildDistanceIndicator(double radius) {
    final progress = _distanceToChurch > 0
        ? (1 - (_distanceToChurch / (radius * 3))).clamp(0.0, 1.0)
        : 0.0;
    final distanceText = _distanceToChurch < 1000
        ? '${_distanceToChurch.toInt()} m'
        : '${(_distanceToChurch / 1000).toStringAsFixed(1)} km';

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Distance à l\'église', style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 13)),
              Text(distanceText, style: const TextStyle(color: Colors.cyanAccent, fontSize: 16, fontWeight: FontWeight.bold)),
            ],
          ),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: progress,
              backgroundColor: Colors.white.withAlpha(15),
              valueColor: AlwaysStoppedAnimation(
                _isWithinGeofence ? Colors.green : Colors.amber,
              ),
              minHeight: 8,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildChurchInfo(String churchName, double radius) {
    if (_currentPosition == null || _geofenceConfig == null) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.church, color: Colors.amber.withAlpha(200), size: 20),
              const SizedBox(width: 8),
              Text(churchName, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
            ],
          ),
          const SizedBox(height: 8),
          _buildInfoRow('Rayon', '${radius.toInt()} m'),
          _buildInfoRow('Position GPS', '${_currentPosition!.latitude.toStringAsFixed(5)}, ${_currentPosition!.longitude.toStringAsFixed(5)}'),
          _buildInfoRow('Précision', '${_currentPosition!.accuracy.toStringAsFixed(0)} m'),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 13)),
          Text(value, style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 13)),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    return Row(
      children: [
        Expanded(
          child: ElevatedButton.icon(
            onPressed: _isCheckedIn ? null : _checkIn,
            icon: const Icon(Icons.login, size: 18),
            label: const Text('Pointer'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: ElevatedButton.icon(
            onPressed: !_isCheckedIn ? null : _checkOut,
            icon: const Icon(Icons.logout, size: 18),
            label: const Text('Sortir'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.orange,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildGPSInfo() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _isTracking ? Colors.cyanAccent.withAlpha(10) : Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: _isTracking ? Colors.cyanAccent.withAlpha(40) : Colors.white.withAlpha(10),
        ),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(Icons.satellite_alt, color: _isTracking ? Colors.cyanAccent : Colors.white.withAlpha(100), size: 20),
                  const SizedBox(width: 8),
                  Text(_isTracking ? 'Suivi GPS actif' : 'Suivi GPS inactif',
                      style: TextStyle(color: _isTracking ? Colors.cyanAccent : Colors.white.withAlpha(150), fontSize: 14)),
                ],
              ),
              Switch(
                value: _isTracking,
                onChanged: (v) => v ? _startTracking() : _stopTracking(),
                activeThumbColor: Colors.cyanAccent,
              ),
            ],
          ),
          if (_isTracking) ...[
            const SizedBox(height: 8),
            Text('Le suivi GPS automatique déclenche le pointage en entrant/sortant de la zone.',
                style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 11)),
          ],
        ],
      ),
    );
  }
}
