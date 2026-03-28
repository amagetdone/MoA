/// 소셜 로그인 / 자동 로그인 후 온보딩 상태를 나타내는 domain 엔티티
class AuthResult {
  final String status; // "ACTIVE" | "TEMP"
  final bool account;
  final bool plan;
  final String? nickname;
  final String? email;
  final String? profileImageUrl;

  const AuthResult({
    required this.status,
    required this.account,
    required this.plan,
    this.nickname,
    this.email,
    this.profileImageUrl,
  });
}
