import 'package:flutter/material.dart';

/// P1 #57 — Demandes administratives (baptême, dédicace, accueil nouveau)
class AdminRequestsScreen extends StatelessWidget {
  const AdminRequestsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📋 Demandes administratives'),
        backgroundColor: Colors.brown.shade600,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showNewRequestSheet(context),
        backgroundColor: Colors.brown.shade600,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Quick request types
          const Text('Types de demandes', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _requestTypeChip(context, '⛪', 'Baptême'),
              _requestTypeChip(context, '🏠', 'Dédicace'),
              _requestTypeChip(context, '👋', 'Accueil nouveau'),
              _requestTypeChip(context, '🔄', 'Transfert'),
              _requestTypeChip(context, '💍', 'Mariage'),
              _requestTypeChip(context, '🙏', 'Bénédiction'),
            ],
          ),
          const SizedBox(height: 24),
          // My requests
          const Text('Mes demandes', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          _requestCard('Baptême', 'Demande du 20 août', 'Approuvée', Colors.green),
          _requestCard('Dédicace', 'Demande du 15 août', 'En examen', Colors.orange),
          _requestCard('Accueil nouveau', 'Demande du 10 août', 'Traitée', Colors.blue),
        ],
      ),
    );
  }

  Widget _requestTypeChip(BuildContext ctx, String icon, String label) {
    return ActionChip(
      avatar: Text(icon),
      label: Text(label),
      onPressed: () => _showNewRequestSheet(ctx),
    );
  }

  Widget _requestCard(String type, String date, String status, Color statusColor) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: statusColor.withOpacity(0.1),
          child: Icon(Icons.description, color: statusColor),
        ),
        title: Text(type, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(date),
        trailing: Chip(
          label: Text(status, style: TextStyle(fontSize: 12, color: statusColor)),
          backgroundColor: statusColor.withOpacity(0.1),
        ),
      ),
    );
  }

  void _showNewRequestSheet(BuildContext ctx) {
    showModalBottomSheet(
      context: ctx,
      isScrollControlled: true,
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.6,
        expand: false,
        builder: (ctx, _) => Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Nouvelle demande', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const TextField(decoration: InputDecoration(labelText: 'Type', border: OutlineInputBorder())),
                      const SizedBox(height: 12),
                      const TextField(maxLines: 3, decoration: InputDecoration(labelText: 'Motif', border: OutlineInputBorder())),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(backgroundColor: Colors.brown.shade600),
                  onPressed: () => Navigator.pop(ctx),
                  child: const Text('Soumettre', style: TextStyle(color: Colors.white)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
