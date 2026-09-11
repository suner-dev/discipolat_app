import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiService {
  final String baseUrl;
  
  ApiService({this.baseUrl = 'https://api.discipolat.com'});
  
  Map<String, String>? _headers;
  
  Map<String, String> get headers => {
        'Content-Type': 'application/json',
        if (_headers != null) ..._headers!,
      };
  
  Future<void> setAuthToken(String token) async {
    _headers = {'Authorization': 'Bearer $token'};
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('access_token', token);
  }
  
  Future<void> clearAuth() async {
    _headers = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('access_token');
  }
  
  Future<dynamic> get(String endpoint) async {
    final response = await http.get(
      Uri.parse('$baseUrl/api/v1$endpoint'),
      headers: headers,
    );
    
    if (response.statusCode == 401) {
      // Tentative de refresh ou deconnexion
      throw Exception('Non autorise');
    }
    
    if (response.statusCode >= 400) {
      throw Exception('Erreur API: ${response.statusCode}');
    }
    
    return json.decode(response.body);
  }
  
  Future<dynamic> post(String endpoint, Map<String, dynamic> data) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/v1$endpoint'),
      headers: headers,
      body: json.encode(data),
    );
    
    if (response.statusCode >= 400) {
      throw Exception('Erreur API: ${response.statusCode}');
    }
    
    return json.decode(response.body);
  }
  
  Future<dynamic> put(String endpoint, Map<String, dynamic> data) async {
    final response = await http.put(
      Uri.parse('$baseUrl/api/v1$endpoint'),
      headers: headers,
      body: json.encode(data),
    );
    
    if (response.statusCode >= 400) {
      throw Exception('Erreur API: ${response.statusCode}');
    }
    
    return json.decode(response.body);
  }
  
  Future<void> delete(String endpoint) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/api/v1$endpoint'),
      headers: headers,
    );
    
    if (response.statusCode >= 400) {
      throw Exception('Erreur API: ${response.statusCode}');
    }
  }
}

// Instance globale
final apiService = ApiService();
