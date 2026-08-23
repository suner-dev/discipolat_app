import 'package:flutter/material.dart';

class CommunityScreen extends StatelessWidget {
  const CommunityScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Communauté'),
        backgroundColor: Colors.pink.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Compose
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
              boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10)],
            ),
            child: Column(
              children: [
                const TextField(
                  maxLines: 2,
                  decoration: InputDecoration(
                    hintText: 'Partagez un témoignage ou encouragement...',
                    border: InputBorder.none,
                  ),
                ),
                Row(
                  children: [
                    _typeChip('Témoignage', Colors.yellow.shade600),
                    const SizedBox(width: 8),
                    _typeChip('Prière', Colors.pink.shade600),
                    const SizedBox(width: 8),
                    _typeChip('Encouragement', Colors.green.shade600),
                    const Spacer(),
                    ElevatedButton(
                      onPressed: () {},
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.pink.shade600, foregroundColor: Colors.white),
                      child: const Text('Publier', style: TextStyle(fontSize: 12)),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          // Posts
          _postCard('SC', 'Soeur Claire', 'Membre', 'Témoignage',
            'Dieu a guéri ma mère de la grippe cette semaine. Gloire à Dieu! 🙏', 24, 8),
          _postCard('FP', 'Frère Paul', 'Faiseur', 'Prière',
            'Priez pour ma famille, nous traversons une saison difficile.', 31, 12),
          _postCard('PJ', 'Pasteur Jean', 'Pasteur', 'Événement',
            'Rappel: Le culte spécial de jeûne aura lieu ce samedi à 6h.', 45, 6),
          _postCard('SM', 'Soeur Marie', 'Responsable', 'Encouragement',
            'Félicitations à tous ceux qui ont participé à l\'événement caritatif.', 38, 5),
        ],
      ),
    );
  }

  Widget _typeChip(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(label, style: TextStyle(fontSize: 10, color: color, fontWeight: FontWeight.w600)),
    );
  }

  Widget _postCard(String initials, String name, String role, String type, String content, int likes, int comments) {
    Color typeColor;
    if (type == 'Témoignage') typeColor = Colors.yellow.shade700;
    else if (type == 'Prière') typeColor = Colors.pink;
    else if (type == 'Événement') typeColor = Colors.blue;
    else typeColor = Colors.green;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: Colors.pink.shade100,
                  child: Text(initials, style: TextStyle(color: Colors.pink.shade700, fontSize: 12, fontWeight: FontWeight.bold)),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(name, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                          const SizedBox(width: 6),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                            decoration: BoxDecoration(
                              color: typeColor.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(type, style: TextStyle(fontSize: 9, color: typeColor, fontWeight: FontWeight.bold)),
                          ),
                        ],
                      ),
                      Text(role, style: TextStyle(fontSize: 10, color: Colors.grey.shade500)),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(content, style: const TextStyle(fontSize: 13, height: 1.4)),
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.favorite_border, size: 16, color: Colors.grey.shade400),
                Text(' $likes', style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                const SizedBox(width: 16),
                Icon(Icons.chat_bubble_outline, size: 16, color: Colors.grey.shade400),
                Text(' $comments', style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                const Spacer(),
                Icon(Icons.share, size: 16, color: Colors.grey.shade400),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
