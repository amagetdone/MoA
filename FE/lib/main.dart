import 'dart:ui';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:more_app/core/notifications/firebase_web_push_config.dart';
import 'package:more_app/core/notifications/push_notification_sync_service.dart';
import 'package:more_app/core/theme/app_theme.dart';
import 'package:more_app/core/utils/app_env.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/theme_viewmodel.dart';
import 'package:more_app/presentation/widgets/preview/web_preview_shell.dart';
import 'package:more_app/presentation/widgets/security/biometric_app_lock.dart';

class AppScrollBehavior extends MaterialScrollBehavior {
  @override
  Set<PointerDeviceKind> get dragDevices => {
        PointerDeviceKind.touch,
        PointerDeviceKind.mouse,
        PointerDeviceKind.stylus,
        PointerDeviceKind.trackpad,
      };
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppEnv.load();
  try {
    await Hive.initFlutter('more_app');
  } catch (_) {}
  await initializeDateFormatting('ko_KR');

  if (kIsWeb) {
    KakaoSdk.init(
      javaScriptAppKey: '19ba61e6ae279e95967d7a6e100f1dad',
    );
  } else {
    final nativeKey = AppEnv.optional('KAKAO_NATIVE_APP_KEY');
    if (nativeKey != null) {
      KakaoSdk.init(nativeAppKey: nativeKey);
    }
  }

  if (kIsWeb) {
    try {
      await Firebase.initializeApp(options: FirebaseWebPushConfig.options);
    } catch (_) {}
  } else if (defaultTargetPlatform == TargetPlatform.android) {
    try {
      await Firebase.initializeApp();
      FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
    } catch (_) {}
  }

  runApp(const ProviderScope(child: AppBootstrap()));
}

class AppBootstrap extends ConsumerStatefulWidget {
  const AppBootstrap({super.key});

  @override
  ConsumerState<AppBootstrap> createState() => _AppBootstrapState();
}

class _AppBootstrapState extends ConsumerState<AppBootstrap> {
  @override
  void initState() {
    super.initState();
    Future.microtask(
      () => ref.read(pushNotificationSyncServiceProvider).initialize(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return const MoreApp();
  }
}

class MoreApp extends ConsumerWidget {
  const MoreApp({super.key});

  static const _designSize = Size(412, 892);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeProvider);

    return ScreenUtilInit(
      designSize: _designSize,
      minTextAdapt: true,
      splitScreenMode: true,
      builder: (context, child) {
        return MaterialApp.router(
          title: 'MORE',
          debugShowCheckedModeBanner: false,
          theme: AppTheme.light,
          darkTheme: AppTheme.dark,
          themeMode: themeMode,
          scrollBehavior: AppScrollBehavior(),
          routerConfig: appRouter,
          builder: (context, routerChild) {
            final appContent = routerChild ?? const SizedBox.shrink();

            final securedContent = BiometricAppLock(child: appContent);

            if (WebPreviewShell.isEnabled) {
              return WebPreviewShell(
                designSize: _designSize,
                minTextAdapt: true,
                splitScreenMode: true,
                child: securedContent,
              );
            }

            return securedContent;
          },
        );
      },
    );
  }
}
