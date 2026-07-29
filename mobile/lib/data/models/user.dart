class User {
  final String id;
  final String email;
  final String? firstName;
  final String? lastName;
  final String role;
  final bool estChefDeFamille;
  final String statut;
  final DateTime createdAt;

  User({
    required this.id,
    required this.email,
    this.firstName,
    this.lastName,
    required this.role,
    this.estChefDeFamille = false,
    this.statut = 'ACTIVE',
    DateTime? createdAt,
  }) : createdAt = createdAt ?? DateTime.now();

  factory User.fromJson(Map<String, dynamic> json) => User(
    id: json['id'] as String,
    email: json['email'] as String,
    firstName: json['firstName'] as String?,
    lastName: json['lastName'] as String?,
    role: json['role'] as String,
    estChefDeFamille: json['estChefDeFamille'] as bool? ?? false,
    statut: json['statut'] as String? ?? 'ACTIVE',
    createdAt: json['createdAt'] != null
        ? DateTime.parse(json['createdAt'] as String)
        : DateTime.now(),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'email': email,
    'firstName': firstName,
    'lastName': lastName,
    'role': role,
    'estChefDeFamille': estChefDeFamille,
    'statut': statut,
  };
}
