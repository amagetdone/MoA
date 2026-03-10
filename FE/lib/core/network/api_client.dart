import 'package:dio/dio.dart';
import 'package:more_app/core/constants/app_constants.dart';
import 'package:more_app/core/network/auth_interceptor.dart';

class ApiClient {
  late final Dio dio;

  ApiClient() {
    dio = Dio(
      BaseOptions(
        baseUrl: AppConstants.baseUrl,
        connectTimeout: AppConstants.connectionTimeout,
        receiveTimeout: AppConstants.receiveTimeout,
        headers: {'Content-Type': 'application/json'},
      ),
    );
    dio.interceptors.add(AuthInterceptor());
    dio.interceptors.add(
      LogInterceptor(requestBody: true, responseBody: true),
    );
  }
}
