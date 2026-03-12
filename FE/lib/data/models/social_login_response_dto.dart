/// 소셜 로그인 API 응답
class SocialLoginResponseDto {
  final String status; // "ACTIVE" | "TEMP"
  final bool account;
  final bool plan;
  final String accessToken;
  final String refreshToken;

  SocialLoginResponseDto({
    required this.status,
    required this.account,
    required this.plan,
    required this.accessToken,
    required this.refreshToken,
  });

  factory SocialLoginResponseDto.fromJson(Map<String, dynamic> json) {
    return SocialLoginResponseDto(
      status: json['status'] as String? ?? 'TEMP',
      account: json['account'] as bool? ?? false,
      plan: json['plan'] as bool? ?? false,
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
    );
  }
}
