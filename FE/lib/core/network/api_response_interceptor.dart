import 'package:dio/dio.dart';

/// 공통 API 응답 래퍼를 자동으로 언래핑하는 인터셉터
///
/// 성공: { "status": "ok", "message": "...", "data": { ... } } → response.data = data 부분만
/// 실패: { "status": "error", "message": "...", "errorCode": "..." } → DioException throw
class ApiResponseInterceptor extends Interceptor {
  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final data = response.data;

    if (data is Map<String, dynamic> && data.containsKey('status')) {
      if (data['status'] == 'ok') {
        // data 필드만 꺼내서 전달
        response.data = data['data'];
      } else if (data['status'] == 'error') {
        // 에러 응답을 DioException으로 변환
        handler.reject(
          DioException(
            requestOptions: response.requestOptions,
            response: response,
            message: data['message'] as String? ?? '요청 처리에 실패했습니다.',
            type: DioExceptionType.badResponse,
          ),
        );
        return;
      }
    }

    handler.next(response);
  }
}
