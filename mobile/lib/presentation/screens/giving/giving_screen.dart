import 'package:flutter/material.dart';
import 'dart:async';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Dîmes & Offrandes 2.0 — don par Mobile Money avec suivi du statut.
class GivingScreen extends StatefulWidget {
  const GivingScreen({super.key});

  @override
  State<GivingScreen> createState() => _GivingScreenState();
}

class _GivingScreenState extends State<GivingScreen> {
  final _apiService = ApiService();
  final _formKey = GlobalKey<FormState>();
  final _amountCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();

  String _operator = 'ORANGE_MONEY';
  String _purpose = 'OFFRANDE';
  bool _submitting = false;
  String? _pendingRef; // providerReference du paiement en cours
  Timer? _pollTimer;
  List<dynamic> _mine = [];
  bool _isLoading = true;

  static const _operators = {
    'M_PESA': 'M-Pesa',
    'MTN_MOMO': 'MTN MoMo',
    'ORANGE_MONEY': 'Orange Money',
    'AIRTEL_MONEY': 'Airtel Money',
    'WAVE': 'Wave',
    'CARD': 'Carte bancaire',
    'CASH': 'Espèces',
  };
  static const _purposes = {
    'DIME': 'Dîme',
    'OFFRANDE': 'Offrande',
    'PROMESSE': 'Promesse',
    'PROJET_SPECIAL': 'Projet spécial',
    'DON_DIASPORA': 'Don diaspora',
  };

  @override
  void initState() {
    super.initState();
    _loadMine();
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _amountCtrl.dispose();
    _phoneCtrl.dispose();
    super.dispose();
  }
