import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:more_app/core/theme/app_theme.dart';
import 'package:more_app/core/utils/app_env.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppEnv.load();
  try {
    await Hive.initFlutter('more_app');
  } catch (_) {}
  await initializeDateFormatting('ko_KR');

  final kakaoNativeAppKey = AppEnv.optional('KAKAO_NATIVE_APP_KEY');
  if (kakaoNativeAppKey != null) {
    KakaoSdk.init(nativeAppKey: kakaoNativeAppKey);
  }

  runApp(const ProviderScope(child: MoreApp()));
}

class MoreApp extends StatelessWidget {
  const MoreApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'MORE',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      routerConfig: appRouter,
    );
  }
}
