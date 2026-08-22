import 'package:flutter/material.dart';
import '../../../core/api/api_service.dart';

class SurveysScreen extends StatefulWidget {
  const SurveysScreen({super.key});

  @override
  State<SurveysScreen> createState() => _SurveysScreenState();
}

class _SurveysScreenState extends State<SurveysScreen> {
  List<dynamic> surveys = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadSurveys();
  }

  Future<void> _loadSurveys() async {
    try {
      final res = await ApiService.get('/surveys');
      setState(() {
        surveys = (res.data['content'] ?? res.data ?? []) as List;
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
        title: const Text('Sondages'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {},
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : surveys.isEmpty
              ? const Center(child: Text('Aucun sondage'))
              : ListView.builder(
                  itemCount: surveys.length,
                  itemBuilder: (context, index) {
                    final survey = surveys[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                      child: ListTile(
                        leading: const Icon(Icons.poll, color: Colors.indigo),
                        title: Text(survey['titre'] ?? ''),
                        subtitle: Text('${survey['totalReponses'] ?? 0} réponses'),
                        trailing: Chip(
                          label: Text(survey['statut'] ?? '', style: const TextStyle(fontSize: 10)),
                          backgroundColor: survey['statut'] == 'ACTIF' ? Colors.green.shade100 : Colors.grey.shade100,
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
