/// 토큰 재발급 API 응답 (accessToken, refreshToken만 포함)
class TokenRefreshResponseDto {
  final String accessToken;
  final String refreshToken;

  TokenRefreshResponseDto({
    required this.accessToken,
    required this.refreshToken,
  });

  factory TokenRefreshResponseDto.fromJson(Map<String, dynamic> json) {
    return TokenRefreshResponseDto(
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
    );
  }
}
