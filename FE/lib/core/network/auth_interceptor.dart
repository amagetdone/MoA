import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:more_app/core/constants/app_constants.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor({this.onForceLogout});

  final void Function()? onForceLogout;
  final _storage = const FlutterSecureStorage();
  bool _isRefreshing = false;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final path = options.path;
    final isAuthRequest = path.contains('auth/social') || path.contains('auth/refresh');
    if (!isAuthRequest) {
      final token = await _storage.read(key: 'access_token');
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
        print('[AuthInterceptor] Authorization: Bearer $token');
      } else {
        print('[AuthInterceptor] No access_token found for ${options.path}');
      }
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    // 401이 아니거나, refresh 요청 자체가 실패한 경우 그냥 넘김
    if (err.response?.statusCode != 401) {
      return handler.next(err);
    }

    // refresh 요청이 401이면 → 로그인 화면으로
    if (err.requestOptions.path.contains('auth/refresh')) {
      await _forceLogout();
      return handler.next(err);
    }

    // 이미 refresh 중이면 중복 방지
    if (_isRefreshing) {
      return handler.next(err);
    }

    _isRefreshing = true;
    try {
      final refreshToken = await _storage.read(key: 'refresh_token');
      if (refreshToken == null) {
        await _forceLogout();
        return handler.next(err);
      }

      // 별도 Dio로 refresh 요청 (인터셉터 무한루프 방지)
      final refreshDio = Dio(BaseOptions(
        baseUrl: AppConstants.baseUrl,
        headers: {'Content-Type': 'application/json'},
      ));

      final response = await refreshDio.post(
        'v1/auth/refresh',
        data: {'refreshToken': refreshToken},
      );

      final data = response.data as Map<String, dynamic>;
      // 공통 래퍼 언래핑
      final inner = data.containsKey('data')
          ? data['data'] as Map<String, dynamic>
          : data;

      final newAccessToken = inner['accessToken'] as String?;
      final newRefreshToken = inner['refreshToken'] as String?;

      if (newAccessToken == null || newRefreshToken == null) {
        await _forceLogout();
        return handler.next(err);
      }

      // 새 토큰 저장
      await _storage.write(key: 'access_token', value: newAccessToken);
      await _storage.write(key: 'refresh_token', value: newRefreshToken);

      // 원래 요청 재시도
      final opts = err.requestOptions;
      opts.headers['Authorization'] = 'Bearer $newAccessToken';

      final retryResponse = await refreshDio.fetch(opts);
      return handler.resolve(retryResponse);
    } on DioException {
      // refresh 실패 → 로그인 화면
      await _forceLogout();
      return handler.next(err);
    } finally {
      _isRefreshing = false;
    }
  }

  Future<void> _forceLogout() async {
    await _storage.delete(key: 'access_token');
    await _storage.delete(key: 'refresh_token');
    onForceLogout?.call();
  }
}
