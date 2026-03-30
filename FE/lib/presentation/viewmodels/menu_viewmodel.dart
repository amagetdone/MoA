import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/repositories/goal_repository.dart';
import 'package:more_app/core/notifications/push_notification_sync_service.dart';
import 'package:more_app/domain/repositories/menu_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';

class MenuState {
  final String userName;
  final String userEmail;
  final String? profileImageUrl;
  final int activeGoalCount;
  final int achievedGoalCount;
  final int totalSaved;
  final bool notificationEnabled;
  final bool biometricEnabled;
  final bool hasLoadedSummary;
  final bool isLoading;
  final String? errorMessage;

  const MenuState({
    this.userName = '',
    this.userEmail = '',
    this.profileImageUrl,
    this.activeGoalCount = 0,
    this.achievedGoalCount = 0,
    this.totalSaved = 0,
    this.notificationEnabled = true,
    this.biometricEnabled = false,
    this.hasLoadedSummary = false,
    this.isLoading = false,
    this.errorMessage,
  });

  MenuState copyWith({
    String? userName,
    String? userEmail,
    String? profileImageUrl,
    int? activeGoalCount,
    int? achievedGoalCount,
    int? totalSaved,
    bool? notificationEnabled,
    bool? biometricEnabled,
    bool? hasLoadedSummary,
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
  }) {
    return MenuState(
      userName: userName ?? this.userName,
      userEmail: userEmail ?? this.userEmail,
      profileImageUrl: profileImageUrl ?? this.profileImageUrl,
      activeGoalCount: activeGoalCount ?? this.activeGoalCount,
      achievedGoalCount: achievedGoalCount ?? this.achievedGoalCount,
      totalSaved: totalSaved ?? this.totalSaved,
      notificationEnabled: notificationEnabled ?? this.notificationEnabled,
      biometricEnabled: biometricEnabled ?? this.biometricEnabled,
      hasLoadedSummary: hasLoadedSummary ?? this.hasLoadedSummary,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage
          ? null
          : errorMessage ?? this.errorMessage,
    );
  }
}

class MenuViewModel extends StateNotifier<MenuState> {
  MenuViewModel(
    this._repository,
    this._goalRepository,
    this._pushNotificationSyncService,
  ) : super(const MenuState(isLoading: true)) {
    loadData();
  }

  final MenuRepository _repository;
  final GoalRepository _goalRepository;
  final PushNotificationSyncService _pushNotificationSyncService;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final summary = await _repository.fetchMenuSummary();
      var activeGoalCount = summary.activeGoalCount;
      var achievedGoalCount = summary.achievedGoalCount;
      var totalSaved = summary.totalSaved;

      try {
        final goals = await _goalRepository.fetchGoals();
        activeGoalCount = goals
            .where((goal) => goal.isEffectivelyInProgress)
            .length;
        achievedGoalCount = goals
            .where((goal) => goal.isEffectivelyAchieved)
            .length;
        totalSaved = goals.fold<int>(0, (sum, goal) => sum + goal.currentAmount);
      } catch (_) {}

      if (!mounted) {
        return;
      }

      state = state.copyWith(
        isLoading: false,
        userName: summary.user.name,
        userEmail: summary.user.email,
        profileImageUrl: summary.user.profileImageUrl,
        activeGoalCount: activeGoalCount,
        achievedGoalCount: achievedGoalCount,
        totalSaved: totalSaved,
        notificationEnabled: summary.notificationEnabled,
        biometricEnabled: summary.biometricEnabled,
        hasLoadedSummary: true,
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      state = state.copyWith(isLoading: false, errorMessage: error.toString());
    }
  }

  Future<void> toggleNotification(bool value) async {
    try {
      final resolved = await _pushNotificationSyncService
          .setNotificationsEnabled(value);
      final summary = await _repository.updateNotification(resolved);
      if (!mounted) {
        return;
      }
      state = state.copyWith(
        notificationEnabled: summary.notificationEnabled,
        clearErrorMessage: true,
      );

      if (value && !resolved) {
        state = state.copyWith(errorMessage: '알림 권한이 허용되지 않아 푸시 알림을 켤 수 없습니다.');
      }
    } catch (error) {
      if (!mounted) {
        return;
      }
      state = state.copyWith(errorMessage: error.toString());
    }
  }

  Future<void> toggleBiometric(bool value) async {
    try {
      final summary = await _repository.updateBiometric(value);
      if (!mounted) {
        return;
      }
      state = state.copyWith(
        biometricEnabled: summary.biometricEnabled,
        clearErrorMessage: true,
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      state = state.copyWith(errorMessage: error.toString());
    }
  }
}

final menuViewModelProvider = StateNotifierProvider<MenuViewModel, MenuState>((
  ref,
) {
  ref.watch(authViewModelProvider);
  return MenuViewModel(
    ref.read(menuRepositoryProvider),
    ref.read(goalRepositoryProvider),
    ref.read(pushNotificationSyncServiceProvider),
  );
});
