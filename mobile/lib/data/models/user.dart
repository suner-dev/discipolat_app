class User {
  final String id;
  final String email;
  final String? firstName;
  final String? lastName;
  final String role;
  final List<String> roles;
  final String activeRole;
  final bool estChefDeFamille;
  final String statut;
  final DateTime createdAt;

  User({
    required this.id,
    required this.email,
    this.firstName,
    this.lastName,
    required this.role,
    this.roles = const [],
    this.activeRole = '',
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
    roles: json['roles'] != null
        ? List<String>.from(json['roles'] as List)
        : (json['role'] != null ? [json['role'] as String] : []),
    activeRole: json['activeRole'] as String? ?? json['role'] as String? ?? '',
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
    'roles': roles,
    'activeRole': activeRole,
    'estChefDeFamille': estChefDeFamille,
    'statut': statut,
  };
}
