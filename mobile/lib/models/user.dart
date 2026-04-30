class User {
  final int? userId;
  final String username;
  final String name;
  final String? role;

  User({this.userId, required this.username, required this.name, this.role});

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      userId: json['userId'] as int?,
      username: json['username'] as String? ?? '',
      name: json['name'] as String? ?? '',
      role: json['role'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
        'userId': userId,
        'username': username,
        'name': name,
        'role': role,
      };
}
