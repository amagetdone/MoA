import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
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
    String? accessToken,
    String? idToken,
  }) async {
    final body = <String, String>{};
    if (provider == 'google') {
      if (idToken != null && idToken.isNotEmpty) {
        body['idToken'] = idToken;
      }
      if ((idToken == null || idToken.isEmpty) &&
          accessToken != null &&
          accessToken.isNotEmpty) {
        body['accessToken'] = accessToken;
      }
    } else if (accessToken != null && accessToken.isNotEmpty) {
      body['accessToken'] = accessToken;
    }

    try {
      final response = await _dio.post(
        'v1/auth/social/$provider',
        data: body,
      );

      return SocialLoginResponseDto.fromJson(
        response.data as Map<String, dynamic>,
      );
    } on DioException catch (error) {
      if (kDebugMode && provider == 'google') {
        debugPrint(
          '[GoogleLoginDebug] backend status=${error.response?.statusCode} '
          'body=${error.response?.data}',
        );
      }
      rethrow;
    }
  }

  @override
  Future<TokenRefreshResponseDto> refreshToken({
    required String refreshToken,
  }) async {
    final response = await _dio.post(
      'v1/auth/refresh',
      data: {'refreshToken': refreshToken},
    );

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

  @override
  Future<SocialLoginResponseDto> loginWithTest({required int id}) async {
    final response = await _dio.post('v1/auth/social/test', data: {'id': id});
    return SocialLoginResponseDto.fromJson(response.data as Map<String, dynamic>);
  }
}
