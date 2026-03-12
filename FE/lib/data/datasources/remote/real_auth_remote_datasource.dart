import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/models/user_model.dart';

class RealAuthRemoteDataSource implements AuthRemoteDataSource {
  final Dio _dio;

  RealAuthRemoteDataSource({required Dio dio}) : _dio = dio;

  @override
  Future<SocialLoginResponse> loginWithSocialToken({
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

    return SocialLoginResponse.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<TokenRefreshResponse> refreshToken({
    required String refreshToken,
  }) async {
    final response = await _dio.post(
      'v1/auth/refresh',
      data: {'refreshToken': refreshToken},
    );

    return TokenRefreshResponse.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<UserModel> getCurrentUser({required String accessToken}) async {
    final response = await _dio.get(
      'v1/auth/me',
      options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
    );

    return UserModel.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<void> logout({required String refreshToken}) async {
    await _dio.post(
      'v1/auth/logout',
      data: {'refreshToken': refreshToken},
    );
  }
}
