import 'package:flutter/material.dart';
import '../../../core/api/api_service.dart';

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  List<dynamic> events = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadEvents();
  }

  Future<void> _loadEvents() async {
    try {
      final res = await ApiService.get('/calendar?size=50');
      setState(() {
        events = (res.data['content'] ?? res.data ?? []) as List;
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
        title: const Text('Calendrier'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {},
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : events.isEmpty
              ? const Center(child: Text('Aucun événement'))
              : ListView.builder(
                  itemCount: events.length,
                  itemBuilder: (context, index) {
                    final event = events[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                      child: ListTile(
                        leading: const Icon(Icons.event, color: Colors.blue),
                        title: Text(event['titre'] ?? ''),
                        subtitle: Text(event['lieu'] ?? ''),
                        trailing: Text(
                          event['dateDebut'] ?? '',
                          style: const TextStyle(fontSize: 11, color: Colors.grey),
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
