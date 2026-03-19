import 'package:dio/dio.dart';
import 'package:more_app/core/constants/app_constants.dart';
import 'package:more_app/core/network/api_response_interceptor.dart';
import 'package:more_app/core/network/auth_interceptor.dart';

class ApiClient {
  late final Dio dio;

  ApiClient({void Function()? onForceLogout}) {
    dio = Dio(
      BaseOptions(
        baseUrl: AppConstants.baseUrl,
        connectTimeout: AppConstants.connectionTimeout,
        receiveTimeout: AppConstants.receiveTimeout,
        headers: {'Content-Type': 'application/json'},
      ),
    );
    dio.interceptors.add(AuthInterceptor(onForceLogout: onForceLogout));
    dio.interceptors.add(ApiResponseInterceptor());
    dio.interceptors.add(
      LogInterceptor(
        requestBody: true,
        responseBody: true,
        logPrint: (o) => print('[API] $o'),
      ),
    );
  }
}
