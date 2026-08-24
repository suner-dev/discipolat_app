import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

class LeaveRequestsScreen extends StatefulWidget {
  const LeaveRequestsScreen({super.key});

  @override
  State<LeaveRequestsScreen> createState() => _LeaveRequestsScreenState();
}

class _LeaveRequestsScreenState extends State<LeaveRequestsScreen> {
  List<dynamic> requests = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadRequests();
  }

  Future<void> _loadRequests() async {
    try {
      final res = await ApiService().get('/leave-requests');
      setState(() {
        requests = (res.data['content'] ?? res.data ?? []) as List;
        loading = false;
      });
    } catch (_) {
      setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Demandes d'absence"),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: () {}),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : requests.isEmpty
              ? const Center(child: Text('Aucune demande'))
              : ListView.builder(
                  itemCount: requests.length,
                  itemBuilder: (context, index) {
                    final req = requests[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                      child: ListTile(
                        leading: Icon(
                          _typeIcon(req['type']),
                          color: Colors.orange,
                        ),
                        title: Text('${req['type'] ?? ''}'),
                        subtitle: Text(
                          '${req['dateDebut'] ?? ''} → ${req['dateFin'] ?? ''}',
                          style: const TextStyle(fontSize: 12),
                        ),
                        trailing: Chip(
                          label: Text(req['statut'] ?? '', style: const TextStyle(fontSize: 10)),
                          backgroundColor: _statusColor(req['statut']),
                        ),
                      ),
                    );
                  },
                ),
    );
  }

  IconData _typeIcon(String? type) {
    switch (type) {
      case 'MALADIE': return Icons.sick;
      case 'CONGE': return Icons.beach_access;
      case 'MISSION': return Icons.flight;
      case 'PERSONNEL': return Icons.person;
      default: return Icons.event_busy;
    }
  }

  Color _statusColor(String? statut) {
    switch (statut) {
      case 'EN_ATTENTE': return Colors.amber.shade100;
      case 'APPROUVE': return Colors.green.shade100;
      case 'REFUSE': return Colors.red.shade100;
      default: return Colors.grey.shade100;
    }
  }
}
