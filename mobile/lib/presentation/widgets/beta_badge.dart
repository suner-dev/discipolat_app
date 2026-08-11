import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/services/providers.dart';

/// Badge « BÊTA » — affiché uniquement lorsque le serveur déclare le mode bêta
/// (`GET /api/v1/public/meta` → `betaMode: true`, profil Spring `beta`).
/// Aucun affichage en production (fail-closed côté client).
class BetaBadge extends ConsumerWidget {
  const BetaBadge({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final meta = ref.watch(metaProvider).valueOrNull;
    if (meta == null || !meta.betaMode) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFFF59E0B), Color(0xFFF97316)],
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: const Color(0xFFF59E0B).withValues(alpha: 0.3), blurRadius: 8),
        ],
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.science_rounded, size: 12, color: Colors.white),
          const SizedBox(width: 4),
          Text(
            'BÊTA',
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.95),
              fontSize: 9,
              fontWeight: FontWeight.w800,
              letterSpacing: 1.2,
            ),
          ),
        ],
      ),
    );
  }
}
