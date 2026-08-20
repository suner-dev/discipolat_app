import 'dart:io';
import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Mobile PDF Report viewer — allows viewing and downloading reports as PDF.
class ReportPdfViewerScreen extends StatefulWidget {
  final String reportType; // 'consolidated' or 'maker'
  final String? familleId;
  final String? faiseurId;

  const ReportPdfViewerScreen({
    super.key,
    required this.reportType,
    this.familleId,
    this.faiseurId,
  });

  @override
  State<ReportPdfViewerScreen> createState() => _ReportPdfViewerScreenState();
}

class _ReportPdfViewerScreenState extends State<ReportPdfViewerScreen> {
  final ApiService _apiService = ApiService();
  bool _isGenerating = false;
  String? _error;
  String? _downloadPath;

  String get _endpoint {
    if (widget.reportType == 'maker') {
      return '/reports/export/maker-pdf';
    }
    return '/reports/export/consolidated-pdf';
  }

  String get _title {
    if (widget.reportType == 'maker') {
      return 'Rapport Faiseur PDF';
    }
    return 'Rapport Consolidé PDF';
  }

  Future<void> _generateAndDownload() async {
    setState(() {
      _isGenerating = true;
      _error = null;
    });

    try {
      final params = <String, String>{};
      if (widget.familleId != null) params['familleId'] = widget.familleId!;
      if (widget.faiseurId != null) params['faiseurId'] = widget.faiseurId!;

      final response = await _apiService.getBytes(
        _endpoint,
        params: params.isNotEmpty ? params : null,
      );

      final bytes = response.data;
      if (bytes == null || (bytes is List && bytes.isEmpty)) {
        setState(() => _error = 'Le PDF généré est vide');
        return;
      }

      // Save to downloads directory
      final dir = await _getDownloadDir();
      final filename = '${_title.replaceAll(' ', '_')}_${DateTime.now().millisecondsSinceEpoch}.pdf';
      final file = File('$dir/$filename');

      if (bytes is List<int>) {
        await file.writeAsBytes(bytes);
      } else {
        await file.writeAsBytes(List<int>.from(bytes));
      }

      setState(() {
        _downloadPath = file.path;
        _isGenerating = false;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('PDF téléchargé : $filename'),
            backgroundColor: Colors.green,
            action: SnackBarAction(
              label: 'Ouvrir',
              textColor: Colors.white,
              onPressed: () {},
            ),
          ),
        );
      }
    } catch (e) {
      setState(() {
        _error = 'Erreur lors de la génération du PDF : $e';
        _isGenerating = false;
      });
    }
  }

  Future<String> _getDownloadDir() async {
    final dir = Directory('/storage/emulated/0/Download/Discipolat');
    if (!await dir.exists()) {
      await dir.create(recursive: true);
    }
    return dir.path;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_title, style: const TextStyle(color: Colors.white)),
        backgroundColor: const Color(0xFF1A365D),
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              const Color(0xFFEBF8FF),
              Theme.of(context).scaffoldBackgroundColor,
            ],
          ),
        ),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // PDF Icon
                Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    color: const Color(0xFFE53E3E).withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(24),
                  ),
                  child: const Icon(
                    Icons.picture_as_pdf,
                    size: 64,
                    color: Color(0xFFE53E3E),
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  _title,
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: const Color(0xFF1A365D),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Générez un rapport PDF professionnel\navec en-tête, KPIs et tableau détaillé',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey[600], fontSize: 14),
                ),
                const SizedBox(height: 32),

                // Generate button
                SizedBox(
                  width: double.infinity,
                  height: 52,
                  child: ElevatedButton.icon(
                    onPressed: _isGenerating ? null : _generateAndDownload,
                    icon: _isGenerating
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Icon(Icons.download, color: Colors.white),
                    label: Text(
                      _isGenerating ? 'Génération en cours...' : 'Générer & Télécharger PDF',
                      style: const TextStyle(color: Colors.white, fontSize: 16),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF2B6CB0),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Info
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.blue.withValues(alpha: 0.05),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.blue.withValues(alpha: 0.2)),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.info_outline, size: 16, color: Colors.blue[700]),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'Le PDF sera sauvegardé dans Downloads/Discipolat/',
                          style: TextStyle(fontSize: 12, color: Colors.blue[700]),
                        ),
                      ),
                    ],
                  ),
                ),

                if (_error != null) ...[
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.red.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.error_outline, color: Colors.red, size: 16),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(_error!, style: const TextStyle(color: Colors.red, fontSize: 13)),
                        ),
                      ],
                    ),
                  ),
                ],

                if (_downloadPath != null) ...[
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.green.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.check_circle, color: Colors.green, size: 16),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Dernier PDF : ${_downloadPath!.split('/').last}',
                            style: const TextStyle(color: Colors.green, fontSize: 13),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
