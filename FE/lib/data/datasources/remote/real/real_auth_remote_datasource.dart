import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/models/social_login_response_dto.dart';
import 'package:more_app/data/models/token_refresh_response_dto.dart';
import 'package:more_app/data/models/user_dto.dart';

class RealAuthRemoteDataSource implements AuthRemoteDataSource {
  final Dio _dio;

  RealAuthRemoteDataSource({required Dio dio}) : _dio = dio;

  @override
  Future<SocialLoginResponseDto> loginWithSocialToken({
    required String provider,
    required String socialToken,
  }) async {
    // Google: { "idToken": "..." }, Kakao: { "accessToken": "..." }
    final body = provider == 'google'
        ? {'idToken': socialToken}
        : {'accessToken': socialToken};

    final response = await _dio.post(
      'v1/auth/social/$provider',
      data: body,
    );

    return SocialLoginResponseDto.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<TokenRefreshResponseDto> refreshToken({
    required String refreshToken,
  }) async {
    final response = await _dio.post(
      'v1/auth/refresh',
      data: {'refreshToken': refreshToken},
    );

    print('[RefreshToken] raw response.data = ${response.data}');
    return TokenRefreshResponseDto.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<UserDto> getCurrentUser({required String accessToken}) async {
    final response = await _dio.get(
      'v1/auth/me',
      options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
    );

    return UserDto.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<void> logout({required String refreshToken}) async {
    await _dio.post(
      'v1/auth/logout',
      data: {'refreshToken': refreshToken},
    );
  }
}
