import 'dart:async';
import 'package:flutter/material.dart';

/// Debounced search field — waits 400ms after the last keystroke
/// before triggering the callback. Prevents API spam on every keystroke.
class DebouncedSearchField extends StatefulWidget {
  final String hintText;
  final ValueChanged<String> onDebounced;
  final TextEditingController? controller;
  final IconData icon;

  const DebouncedSearchField({
    super.key,
    this.hintText = 'Rechercher...',
    required this.onDebounced,
    this.controller,
    this.icon = Icons.search,
  });

  @override
  State<DebouncedSearchField> createState() => _DebouncedSearchFieldState();
}

class _DebouncedSearchFieldState extends State<DebouncedSearchField> {
  Timer? _debounce;
  late final TextEditingController _ctrl;

  @override
  void initState() {
    super.initState();
    _ctrl = widget.controller ?? TextEditingController();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    if (widget.controller == null) _ctrl.dispose();
    super.dispose();
  }

  void _onChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      widget.onDebounced(value.trim());
    });
  }

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: _ctrl,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        hintText: widget.hintText,
        hintStyle: TextStyle(color: Colors.white.withAlpha(100)),
        prefixIcon: Icon(widget.icon, color: Colors.white.withAlpha(100)),
        filled: true,
        fillColor: Colors.white.withAlpha(10),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
        contentPadding: const EdgeInsets.symmetric(vertical: 0, horizontal: 16),
      ),
      onChanged: _onChanged,
    );
  }
}
