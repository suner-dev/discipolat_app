import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core/tenant_session.dart';
import '../presentation/screens/login/login_screen.dart';

/// Écran de sélection du tenant (affiché après connexion si multi-tenant)
class TenantSelectionScreen extends ConsumerStatefulWidget {
  const TenantSelectionScreen({super.key});

  @override
  ConsumerState<TenantSelectionScreen> createState() => _TenantSelectionScreenState();
}

class _TenantSelectionScreenState extends ConsumerState<TenantSelectionScreen> {
  @override
  Widget build(BuildContext context) {
    final session = ref.watch(tenantSessionProvider);
    
    if (!session.hasMultipleTenants || session.tenants.isEmpty) {
      // Rediriger vers l'écran principal si pas de choix
      Future.microtask(() {
        if (mounted) {
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(builder: (_) => const MainScaffold()),
          );
        }
      });
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Choisir votre organisation'),
        centerTitle: true,
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: session.tenants.length,
        itemBuilder: (context, index) {
          final tenant = session.tenants[index];
          final isSelected = tenant['id'] == session.activeTenantId;
          
          return Card(
            elevation: isSelected ? 4 : 1,
            color: isSelected ? Theme.of(context).primaryColor.withOpacity(0.1) : null,
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: isSelected 
                    ? Theme.of(context).primaryColor 
                    : Colors.grey[300],
                child: Icon(
                  Icons.business,
                  color: isSelected ? Colors.white : Colors.grey[700],
                ),
              ),
              title: Text(
                tenant['name'] ?? 'Organisation',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                  color: isSelected ? Theme.of(context).primaryColor : null,
                ),
              ),
              subtitle: Text(
                tenant['role'] ?? 'Membre',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Colors.grey[600],
                ),
              ),
              trailing: isSelected
                  ? const Icon(Icons.check_circle, color: Colors.green)
                  : null,
              onTap: () async {
                await session.switchTenant(tenant['id']!);
                if (mounted) {
                  Navigator.of(context).pushReplacement(
                    MaterialPageRoute(builder: (_) => const MainScaffold()),
                  );
                }
              },
            ),
          );
        },
      ),
    );
  }
}

/// Écran principal après sélection du tenant
class MainScaffold extends StatelessWidget {
  const MainScaffold({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Discipolat'),
        actions: [
          // Bouton de changement de tenant
          IconButton(
            icon: const Icon(Icons.business),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const TenantSelectionScreen()),
              );
            },
            tooltip: 'Changer d'organisation',
          ),
        ],
      ),
      body: const Center(
        child: Text('Contenu principal de l'application'),
      ),
    );
  }
}
