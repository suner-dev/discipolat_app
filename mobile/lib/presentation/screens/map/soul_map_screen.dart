import 'dart:async';
import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import '../../../data/services/api_service.dart';

/// Carte Vivante des Âmes — interactive map showing members by location.
/// Each soul appears as a marker; tap for detail card.
class SoulMapScreen extends StatefulWidget {
  const SoulMapScreen({super.key});

  @override
  State<SoulMapScreen> createState() => _SoulMapScreenState();
}

class _SoulMapScreenState extends State<SoulMapScreen> {
  final _api = ApiService();
  final Completer<GoogleMapController> _mapController = Completer();
  Set<Marker> _markers = {};
  List<dynamic> _souls = [];
  bool _isLoading = true;
  String _filter = 'all'; // all, disciples, new, inactive


  // Default center: Paris
  static const CameraPosition _defaultPosition = CameraPosition(
    target: LatLng(48.8566, 2.3522),
    zoom: 12,
  );

  @override
  void initState() {
    super.initState();
    _loadSouls();
  }

  Future<void> _loadSouls() async {
    setState(() => _isLoading = true);
    try {
      final res = await _api.get('/souls?size=500');
      final data = res.data;
      List<dynamic> souls = [];
      if (data is Map && data.containsKey('content')) {
        souls = data['content'] as List<dynamic>;
      } else if (data is List) {
        souls = data;
      }
      if (mounted) {
        setState(() {
          _souls = souls;
          _isLoading = false;
        });
        _buildMarkers();
      }
    } catch (e) {
      if (mounted) setState(() { _isLoading = false; });
    }
  }

  void _buildMarkers() {
    final markers = <Marker>{};


    for (int i = 0; i < _souls.length; i++) {
      final soul = _souls[i];

      // In production: use real lat/lng from soul data
      // For demo: generate spread around Paris
      final lat = 48.8566 + (((soul['id'].hashCode % 1000) / 1000.0) - 0.5) * 0.1;
      final lng = 2.3522 + (((soul['id'].hashCode % 700) / 700.0) - 0.5) * 0.1;

      final type = soul['typeDisciple']?.toString() ?? '';
      final statut = soul['statut']?.toString() ?? '';
      final isInactive = statut == 'INACTIF' || statut == 'ABSENT';

      // Filter
      if (_filter == 'disciples' && type != 'DISCIPLE') continue;
      if (_filter == 'new' && soul['dateIntegration'] == null) continue;
      if (_filter == 'inactive' && !isInactive) continue;

      // Color by type
      BitmapDescriptor icon;
      if (isInactive) {
        icon = BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRed);
      } else if (type == 'DISCIPLE') {
        icon = BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueCyan);
      } else if (type == 'NOUVEAU') {
        icon = BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueGreen);
      } else {
        icon = BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueViolet);
      }

      markers.add(Marker(
        markerId: MarkerId(soul['id']?.toString() ?? '$i'),
        position: LatLng(lat, lng),
        icon: icon,
        infoWindow: InfoWindow(
          title: '${soul['prenom'] ?? ''} ${soul['nom'] ?? ''}',
          snippet: type,
          onTap: () => _showSoulDetail(soul),
        ),
        onTap: () =>    setState(() {}),
      ));
    }

    setState(() => _markers = markers);
  }

  void _showSoulDetail(Map<String, dynamic> soul) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF12122A),
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => DraggableScrollableSheet(
        initialChildSize: 0.4,
        maxChildSize: 0.7,
        minChildSize: 0.3,
        expand: false,
        builder: (_, scrollController) => ListView(
          controller: scrollController,
          padding: const EdgeInsets.all(20),
          children: [
            Center(
              child: Container(width: 40, height: 4, decoration: BoxDecoration(color: Colors.white.withAlpha(40), borderRadius: BorderRadius.circular(2))),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                CircleAvatar(
                  radius: 30,
                  backgroundColor: Colors.cyanAccent.withAlpha(30),
                  child: Text(
                    '${(soul['prenom'] ?? soul['nom'] ?? '?')[0]}'.toUpperCase(),
                    style: const TextStyle(color: Colors.cyanAccent, fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${soul['prenom'] ?? ''} ${soul['nom'] ?? ''}',
                        style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        soul['typeDisciple']?.toString() ?? '',
                        style: TextStyle(color: Colors.cyanAccent.withAlpha(200), fontSize: 13),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            _buildDetailRow('Statut', soul['statut']?.toString() ?? ''),
            _buildDetailRow('Type', soul['typeDisciple']?.toString() ?? ''),
            _buildDetailRow('Date intégration', (soul['dateIntegration']?.toString() ?? '').substring(0, (soul['dateIntegration']?.toString() ?? '').length > 10 ? 10 : (soul['dateIntegration']?.toString() ?? '').length)),
            _buildDetailRow('Dernier contact', (soul['dateDernierContact']?.toString() ?? '').isNotEmpty ? (soul['dateDernierContact']?.toString() ?? '').substring(0, (soul['dateDernierContact']?.toString() ?? '').length > 10 ? 10 : (soul['dateDernierContact']?.toString() ?? '').length) : 'Jamais'),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () {
                  Navigator.pop(context);
                  Navigator.pushNamed(context, '/souls/${soul['id']}');
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.cyanAccent,
                  foregroundColor: Colors.black,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: const Text('Voir la fiche complète'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 13)),
          Text(value, style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 13)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      body: Stack(
        children: [
          // Map
          _isLoading
              ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
              : GoogleMap(
                  initialCameraPosition: _defaultPosition,
                  onMapCreated: (controller) => _mapController.complete(controller),
                  markers: _markers,
                  myLocationEnabled: true,
                  myLocationButtonEnabled: true,
                  mapToolbarEnabled: false,
                  zoomControlsEnabled: false,
                  onCameraMove: (_) => setState(() {}),
                ),

          // App bar overlay
          Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: SafeArea(
              child: Container(
                margin: const EdgeInsets.all(12),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: const Color(0xFF1A1A3A).withAlpha(230),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Row(
                  children: [
                    GestureDetector(
                      onTap: () => Navigator.pop(context),
                      child: Icon(Icons.arrow_back, color: Colors.white.withAlpha(200)),
                    ),
                    const SizedBox(width: 12),
                    const Icon(Icons.map, color: Colors.cyanAccent, size: 20),
                    const SizedBox(width: 8),
                    const Expanded(
                      child: Text('Carte Vivante des Âmes', style: TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
                    ),
                    Text('${_markers.length}', style: TextStyle(color: Colors.cyanAccent, fontSize: 13)),
                  ],
                ),
              ),
            ),
          ),

          // Filter chips
          Positioned(
            top: 80,
            left: 12,
            right: 12,
            child: SizedBox(
              height: 36,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: [
                  _buildFilterChip('Tous', 'all', Icons.people),
                  _buildFilterChip('Disciples', 'disciples', Icons.school),
                  _buildFilterChip('Nouveaux', 'new', Icons.fiber_new),
                  _buildFilterChip('Inactifs', 'inactive', Icons.person_off),
                ],
              ),
            ),
          ),

          // Soul count
          Positioned(
            bottom: 20,
            left: 20,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: const Color(0xFF1A1A3A).withAlpha(200),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Text(
                '${_markers.length} âmes affichées',
                style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 12),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterChip(String label, String value, IconData icon) {
    final isActive = _filter == value;
    return GestureDetector(
      onTap: () {
        setState(() => _filter = value);
        _buildMarkers();
      },
      child: Container(
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isActive ? Colors.cyanAccent.withAlpha(30) : const Color(0xFF1A1A3A).withAlpha(200),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isActive ? Colors.cyanAccent.withAlpha(80) : Colors.white.withAlpha(20),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: isActive ? Colors.cyanAccent : Colors.white.withAlpha(150), size: 14),
            const SizedBox(width: 6),
            Text(label, style: TextStyle(color: isActive ? Colors.cyanAccent : Colors.white.withAlpha(180), fontSize: 12, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }
}
