// B — Écran de consentement RGPD (mobile).
//
// Consomme les endpoints réellement livrés par l'Agent A :
//   GET  /compliance/consents/mine   — état actuel des consentements
//   POST /compliance/consents        — accord ou retrait (RGPD art. 7.1 / 7.3)
//   GET  /public/legal               — documents à accepter (CGU, PRIVACY)
//
// Règles : aucun type de consentement inventé (seuls les 6 du backend sont
// proposés) ; aucun consentement tacite (l'utilisateur agit explicitement) ;
// aucune donnée en dur (tout vient de l'API).

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/services/compliance_service.dart';
import '../../widgets/glass_theme.dart';

final complianceServiceProvider =
    Provider<ComplianceService>((ref) => ComplianceService());

class ConsentScreen extends ConsumerStatefulWidget {
  const ConsentScreen({super.key});

  @override
  ConsumerState<ConsentScreen> createState() => _ConsentScreenState();
}

class _ConsentScreenState extends ConsumerState<ConsentScreen> {
  static const Map<String, String> _labels = {
    'CGU': "Conditions d'utilisation",
    'PRIVACY': 'Politique de confidentialité',
    'CONSENT_ART9': 'Données sensibles de santé',
    'WHATSAPP': 'Notifications WhatsApp',
    'MARKETING': 'Communications marketing',
    'PHOTO': "Photos de l'église",
  };

  static const Map<String, String> _purposes = {
    'CGU': "Vous.authentication sur la plateforme et son utilisation.",
    'PRIVACY': "Nous traitons vos données personnelles pour fournir le service.",
    'CONSENT_ART9': "Nous traitons des données de santé que vous nous confiez volontairement.",
    'WHATSAPP': "Nous vous envoyons des notifications WhatsApp sur WhatsApp.",
    'MARKETING': "Nous vous envoyons des communications promotionnelles.",
    'PHOTO': "Nous publions les photos que vous déposez pour l'église.",
  };

  bool _loading = true;
  bool _saving = false;
  String? _error;
  Set<String> _granted = <String>{};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final mine = await ref.read(complianceServiceProvider).myConsents();
      if (!mounted) return;
      setState(() {
        _granted = mine.where((c) => c.granted).map((c) => c.type.wire).toSet();
        _loading = false;
      });
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Impossible de charger vos consentements. Vérifiez votre connexion.';
        _loading = false;
      });
      debugPrint('ConsentScreen._load: $e');
    }
  }

  Future<void> _setConsent(String wire, bool value) async {
    final type = ConsentType.tryParse(wire);
    if (type == null) return; // garde-fou : jamais de type hors contrat
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await ref.read(complianceServiceProvider).recordConsent(type, granted: value);
      if (!mounted) return;
      setState(() {
        if (value) {
          _granted.add(wire);
        } else {
          _granted.remove(wire);
        }
        _saving = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(value ? 'Consentement enregistré.' : 'Consentement retiré.'),
      ));
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        _saving = false;
        _error = "L'enregistrement a échoué. Réessayez.";
      });
      debugPrint('ConsentScreen._setConsent: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Vos consentements')),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 12),
            Text('Chargement de vos consentements…'),
          ],
        ),
      );
    }

    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off, size: 48),
              const SizedBox(height: 12),
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _load,
                style: FilledButton.styleFrom(
                    minimumSize: const Size(200, 48), backgroundColor: AppColors.primary),
                child: const Text('Réessayer'),
              ),
            ],
          ),
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          "Vous gardez le contrôle sur l'usage de vos données. Vous pouvez accepter ou retirer "
          "chaque consentement à tout moment.",
          style: TextStyle(fontSize: 13),
        ),
        const SizedBox(height: 16),
        for (final entry in _labels.entries)
          GlassCard(
            margin: const EdgeInsets.only(bottom: 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(entry.value, style: const TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text(_purposes[entry.key] ?? '', style: const TextStyle(fontSize: 12)),
                // Semantics : sans cela, un lecteur d'ecran annonce seulement
                // "Accordé / Non accordé" sans dire CE QUE l'on accorde.
                Semantics(
                  label: entry.value,
                  child: SwitchListTile(
                    key: ValueKey('consent-${entry.key}'),
                    value: _granted.contains(entry.key),
                    onChanged: _saving ? null : (v) => _setConsent(entry.key, v),
                    title: Text(_granted.contains(entry.key) ? 'Accordé' : 'Non accordé'),
                    subtitle: const Text('Glissez pour accord ou retirer'),
                    contentPadding: EdgeInsets.zero,
                    activeThumbColor: AppColors.primary,
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
