import 'package:flutter/material.dart';

import '../../data/services/impersonation_service.dart';
import '../../app.dart';

class ImpersonationBanner extends StatelessWidget {
  const ImpersonationBanner({super.key, required this.service});

  final ImpersonationService service;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: service,
      builder: (context, _) {
        if (!service.isImpersonating) return const SizedBox.shrink();
        return Material(
          color: Colors.orange.shade900,
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              child: Row(
                children: [
                  const Icon(Icons.warning_amber_rounded, color: Colors.white),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Impersonation : ${service.targetEmail ?? "utilisateur cible"}'
                      '${service.targetTenantName == null ? "" : " · ${service.targetTenantName}"}',
                      style: const TextStyle(color: Colors.white, fontSize: 12),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  TextButton(
                    onPressed: () async {
                      await service.stop();
                      if (context.mounted) {
                        appRouter.go('/platform/dashboard');
                      }
                    },
                    child: const Text('Quitter'),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
