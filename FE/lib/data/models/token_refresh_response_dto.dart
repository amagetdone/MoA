/// 토큰 재발급 API 응답
class TokenRefreshResponseDto {
  final String accessToken;
  final String refreshToken;
  final String status;
  final bool account;
  final bool plan;

  TokenRefreshResponseDto({
    required this.accessToken,
    required this.refreshToken,
    required this.status,
    required this.account,
    required this.plan,
  });

  factory TokenRefreshResponseDto.fromJson(Map<String, dynamic> json) {
    return TokenRefreshResponseDto(
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
      status: json['status'] as String? ?? 'TEMP',
      account: json['account'] as bool? ?? false,
      plan: json['plan'] as bool? ?? false,
    );
  }
}
