import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

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
      final res = await ApiService().get('/surveys');
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
        title: Text(AppLocalizations.of(context).surveysTitle),
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
              ? Center(child: Text(AppLocalizations.of(context).surveysEmpty))
              : ListView.builder(
                  itemCount: surveys.length,
                  itemBuilder: (context, index) {
                    final survey = surveys[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                      child: ListTile(
                        leading: const Icon(Icons.poll, color: Colors.indigo),
                        title: Text(survey['titre'] ?? ''),
                        subtitle: Text(AppLocalizations.of(context).responsesCount((survey['totalReponses'] as num?)?.toInt() ?? 0)),
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
