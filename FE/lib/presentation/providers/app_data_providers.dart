import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/security/biometric_auth_service.dart';
import 'package:more_app/core/notifications/local_notification_service.dart';
import 'package:more_app/core/notifications/push_notification_sync_service.dart';
import 'package:more_app/core/network/api_client.dart';
import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/account_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/goal_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/home_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/mock/mock_api_goal_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/menu_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/push_notification_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/real/real_auth_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/real/real_account_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/real/real_goal_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/real/real_push_notification_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';
import 'package:more_app/data/datasources/remote/real/real_home_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/real/real_menu_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/budget_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/real/real_budget_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/real/real_spending_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/social_auth_datasource.dart';
import 'package:more_app/data/datasources/remote/social_auth_datasource_impl.dart';
import 'package:more_app/data/datasources/remote/spending_remote_datasource.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource_impl.dart';
import 'package:more_app/data/repositories/account_repository_impl.dart';
import 'package:more_app/data/repositories/auth_repository_impl.dart';
import 'package:more_app/data/repositories/goal_repository_impl.dart';
import 'package:more_app/data/repositories/home_repository_impl.dart';
import 'package:more_app/data/repositories/menu_repository_impl.dart';
import 'package:more_app/data/repositories/notification_inbox_repository_impl.dart';
import 'package:more_app/data/repositories/budget_repository_impl.dart';
import 'package:more_app/data/repositories/spending_repository_impl.dart';
import 'package:more_app/domain/repositories/account_repository.dart';
import 'package:more_app/domain/repositories/auth_repository.dart';
import 'package:more_app/domain/repositories/goal_repository.dart';
import 'package:more_app/domain/repositories/home_repository.dart';
import 'package:more_app/domain/repositories/menu_repository.dart';
import 'package:more_app/domain/repositories/notification_inbox_repository.dart';
import 'package:more_app/domain/repositories/budget_repository.dart';
import 'package:more_app/domain/repositories/spending_repository.dart';
import 'package:more_app/data/datasources/remote/real/real_chatbot_remote_datasource.dart';
import 'package:more_app/data/repositories/chatbot_repository_impl.dart';
import 'package:more_app/domain/repositories/chatbot_repository.dart';
import 'package:more_app/domain/entities/app_notification_item.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/biometric_preference_viewmodel.dart';

final appLocalDataSourceProvider = Provider<AppLocalDataSource>((ref) {
  return AppLocalDataSourceImpl();
});

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    onForceLogout: () => appRouter.go(
      AppRoutes.loginLocation(reason: AppRoutes.sessionExpiredReason),
    ),
  );
});

final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  return RealAuthRemoteDataSource(dio: ref.read(apiClientProvider).dio);
});

final authLocalDataSourceProvider = Provider<AuthLocalDataSource>((ref) {
  return AuthLocalDataSourceImpl();
});

final socialAuthDataSourceProvider = Provider<SocialAuthDataSource>((ref) {
  return SocialAuthDataSourceImpl();
});

final pushNotificationRemoteDataSourceProvider =
    Provider<PushNotificationRemoteDataSource>((ref) {
      return RealPushNotificationRemoteDataSource(
        dio: ref.read(apiClientProvider).dio,
      );
    });

final pushNotificationSyncServiceProvider =
    Provider<PushNotificationSyncService>((ref) {
      final service = PushNotificationSyncService(
        remoteDataSource: ref.read(pushNotificationRemoteDataSourceProvider),
        authLocalDataSource: ref.read(authLocalDataSourceProvider),
        appLocalDataSource: ref.read(appLocalDataSourceProvider),
        notificationInboxRepository: ref.read(
          notificationInboxRepositoryProvider,
        ),
        localNotificationService: ref.read(localNotificationServiceProvider),
      );
      ref.onDispose(service.dispose);
      return service;
    });

final localNotificationServiceProvider = Provider<LocalNotificationService>((
  ref,
) {
  return LocalNotificationService();
});

final notificationInboxRepositoryProvider =
    Provider<NotificationInboxRepository>((ref) {
      final repository = NotificationInboxRepositoryImpl(
        authLocalDataSource: ref.read(authLocalDataSourceProvider),
      );
      return repository;
    });

final notificationInboxProvider = StreamProvider<List<AppNotificationItem>>((
  ref,
) async* {
  final repository = ref.read(notificationInboxRepositoryProvider);
  await repository.initialize();
  yield* repository.watchNotifications();
});

final unreadNotificationCountProvider = Provider<int>((ref) {
  return ref
      .watch(notificationInboxProvider)
      .maybeWhen(
        data: (items) => items.where((item) => !item.isRead).length,
        orElse: () => 0,
      );
});

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepositoryImpl(
    remoteDataSource: ref.read(authRemoteDataSourceProvider),
    localDataSource: ref.read(authLocalDataSourceProvider),
    socialAuthDataSource: ref.read(socialAuthDataSourceProvider),
  );
});

final biometricAuthServiceProvider = Provider<BiometricAuthService>((ref) {
  return BiometricAuthService();
});

final appLockAvailabilityProvider = FutureProvider<bool>((ref) {
  return ref.read(biometricAuthServiceProvider).isBiometricAvailable();
});

final appLockPinConfiguredProvider = FutureProvider<bool>((ref) {
  return ref.read(biometricAuthServiceProvider).hasAppPin();
});

final biometricPreferenceProvider =
    StateNotifierProvider<BiometricPreferenceViewModel, bool>((ref) {
      return BiometricPreferenceViewModel(ref.read(appLocalDataSourceProvider));
    });

final seedDataStoreProvider = Provider<SeedDataStore>((ref) {
  return SeedDataStore();
});

final homeRemoteDataSourceProvider = Provider<HomeRemoteDataSource>((ref) {
  return RealHomeRemoteDataSource(dio: ref.read(apiClientProvider).dio);
});

final accountRemoteDataSourceProvider = Provider<AccountRemoteDataSource>((
  ref,
) {
  return RealAccountRemoteDataSource(dio: ref.read(apiClientProvider).dio);
});

final goalRemoteDataSourceProvider = Provider<GoalRemoteDataSource>((ref) {
  const forceGoalMockApi = bool.fromEnvironment('USE_GOAL_MOCK_API');

  if (forceGoalMockApi) {
    return MockApiGoalRemoteDataSource();
  }

  return RealGoalRemoteDataSource(dio: ref.read(apiClientProvider).dio);
});

final spendingRemoteDataSourceProvider = Provider<SpendingRemoteDataSource>((
  ref,
) {
  return RealSpendingRemoteDataSource(dio: ref.read(apiClientProvider).dio);
});

final budgetRemoteDataSourceProvider = Provider<BudgetRemoteDataSource>((ref) {
  return RealBudgetRemoteDataSource(dio: ref.read(apiClientProvider).dio);
});

final budgetRepositoryProvider = Provider<BudgetRepository>((ref) {
  return BudgetRepositoryImpl(
    remoteDataSource: ref.read(budgetRemoteDataSourceProvider),
  );
});

final menuRemoteDataSourceProvider = Provider<MenuRemoteDataSource>((ref) {
  return RealMenuRemoteDataSource(dio: ref.read(apiClientProvider).dio);
});

final goalRepositoryProvider = Provider<GoalRepository>((ref) {
  return GoalRepositoryImpl(
    remoteDataSource: ref.read(goalRemoteDataSourceProvider),
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final homeRepositoryProvider = Provider<HomeRepository>((ref) {
  return HomeRepositoryImpl(
    remoteDataSource: ref.read(homeRemoteDataSourceProvider),
    goalRemoteDataSource: ref.read(goalRemoteDataSourceProvider),
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final accountRepositoryProvider = Provider<AccountRepository>((ref) {
  return AccountRepositoryImpl(
    remoteDataSource: ref.read(accountRemoteDataSourceProvider),
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final spendingRepositoryProvider = Provider<SpendingRepository>((ref) {
  return SpendingRepositoryImpl(
    remoteDataSource: ref.read(spendingRemoteDataSourceProvider),
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final menuRepositoryProvider = Provider<MenuRepository>((ref) {
  return MenuRepositoryImpl(
    remoteDataSource: ref.read(menuRemoteDataSourceProvider),
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final chatbotRemoteDataSourceProvider = Provider<RealChatbotRemoteDataSource>((
  ref,
) {
  return RealChatbotRemoteDataSource(dio: ref.read(apiClientProvider).dio);
});

final chatbotRepositoryProvider = Provider<ChatbotRepository>((ref) {
  return ChatbotRepositoryImpl(
    dataSource: ref.read(chatbotRemoteDataSourceProvider),
  );
});

final goalSketchVideoProvider = FutureProvider.family<Uint8List?, String>((
  ref,
  goalId,
) {
  return ref.read(goalRepositoryProvider).getGoalSketchVideo(goalId);
});
