import 'package:flutter/material.dart';

class MarketplaceScreen extends StatelessWidget {
  const MarketplaceScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Marketplace'),
        backgroundColor: Colors.teal.shade600,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.add_circle_outline),
            onPressed: () {},
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            decoration: InputDecoration(
              hintText: 'Rechercher...',
              prefixIcon: const Icon(Icons.search, size: 20),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            ),
          ),
          const SizedBox(height: 16),
          // Filter chips
          Wrap(
            spacing: 8,
            children: [
              _chip('Tout', true),
              _chip('Offres', false),
              _chip('Demandes', false),
              _chip('Services', false),
              _chip('Gratuit', false),
            ],
          ),
          const SizedBox(height: 16),
          _listingCard('Clavier Yamaha PSR-E473', 'Frère Samuel', '45 000 FCFA', 'Musique', 'Offre'),
          _listingCard('Cours de guitare gratuit', 'Soeur Marie', 'Gratuit', 'Formation', 'Service'),
          _listingCard('Recherche babysitter', 'Sœur Priscilla', '', 'Services', 'Demande'),
          _listingCard('Vestes d\'hiver', 'Diaconie', 'Gratuit', 'Vêtements', 'Gratuit'),
          _listingCard('Bible NBS étudiant', 'Librairie Chrétiene', '8 000 FCFA', 'Livres', 'Offre'),
        ],
      ),
    );
  }

  Widget _chip(String label, bool selected) {
    return FilterChip(
      label: Text(label, style: TextStyle(fontSize: 12, color: selected ? Colors.white : Colors.teal)),
      selected: selected,
      onSelected: (_) {},
      backgroundColor: Colors.teal.shade50,
      selectedColor: Colors.teal.shade600,
    );
  }

  Widget _listingCard(String title, String seller, String price, String category, String type) {
    Color typeColor;
    if (type == 'Offre') typeColor = Colors.blue;
    else if (type == 'Demande') typeColor = Colors.orange;
    else if (type == 'Service') typeColor = Colors.green;
    else typeColor = Colors.purple;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            height: 120,
            width: double.infinity,
            decoration: BoxDecoration(
              color: Colors.teal.shade50,
              borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
            ),
            child: Center(child: Icon(Icons.store, size: 48, color: Colors.teal.shade200)),
          ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold))),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: typeColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(type, style: TextStyle(fontSize: 10, color: typeColor, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(seller, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(category, style: const TextStyle(fontSize: 11, color: Colors.grey)),
                    if (price.isNotEmpty)
                      Text(price, style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.teal.shade600)),
                  ],
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () {},
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.teal.shade600, foregroundColor: Colors.white),
                    child: const Text('Contacter', style: TextStyle(fontSize: 12)),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
