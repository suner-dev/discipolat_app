import 'package:flutter/material.dart';

class InventoryScreen extends StatelessWidget {
  const InventoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Inventaire'),
        backgroundColor: Colors.indigo.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Low stock alert
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.amber.shade50,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.amber.shade200),
            ),
            child: Row(
              children: [
                Icon(Icons.warning_amber, color: Colors.amber.shade600),
                const SizedBox(width: 8),
                const Expanded(child: Text('1 article en stock bas', style: TextStyle(fontSize: 13))),
              ],
            ),
          ),
          const SizedBox(height: 16),
          // Search
          TextField(
            decoration: InputDecoration(
              hintText: 'Rechercher un article...',
              prefixIcon: const Icon(Icons.search, size: 20),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            ),
          ),
          const SizedBox(height: 16),
          // Items
          _itemCard('Chaises pliantes', 'Mobilier', '200 unités', 'Salle principale', Colors.blue, false),
          _itemCard('Bibles', 'Literature', '45 exemplaires', 'Bibliothèque', Colors.green, false),
          _itemCard('Microphones sans fil', 'Audio/Video', '8 unités', 'Réserve technique', Colors.purple, false),
          _itemCard('Nappes blanches', 'Événementiel', '15 unités', 'Réserve', Colors.orange, true),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.indigo,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  Widget _itemCard(String name, String category, String quantity, String location, Color color, bool isLow) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: color.withOpacity(0.1),
          child: Icon(Icons.inventory_2, color: color, size: 20),
        ),
        title: Text(name, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text('$category • $location', style: const TextStyle(fontSize: 12)),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(quantity, style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.bold,
              color: isLow ? Colors.red : Colors.grey.shade700,
            )),
            if (isLow)
              const Text('Stock bas', style: TextStyle(fontSize: 10, color: Colors.red)),
          ],
        ),
      ),
    );
  }
}
