import 'package:flutter_riverpod/flutter_riverpod.dart';
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
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage
          ? null
          : errorMessage ?? this.errorMessage,
    );
  }
}

class MenuViewModel extends StateNotifier<MenuState> {
  MenuViewModel(this._repository) : super(const MenuState(isLoading: true)) {
    loadData();
  }

  final MenuRepository _repository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final summary = await _repository.fetchMenuSummary();

      state = state.copyWith(
        isLoading: false,
        userName: summary.user.name,
        userEmail: summary.user.email,
        profileImageUrl: summary.user.profileImageUrl,
        activeGoalCount: summary.activeGoalCount,
        achievedGoalCount: summary.achievedGoalCount,
        totalSaved: summary.totalSaved,
        notificationEnabled: summary.notificationEnabled,
        biometricEnabled: summary.biometricEnabled,
      );
    } catch (error) {
      state = state.copyWith(isLoading: false, errorMessage: error.toString());
    }
  }

  Future<void> toggleNotification(bool value) async {
    final summary = await _repository.updateNotification(value);
    state = state.copyWith(notificationEnabled: summary.notificationEnabled);
  }

  Future<void> toggleBiometric(bool value) async {
    final summary = await _repository.updateBiometric(value);
    state = state.copyWith(biometricEnabled: summary.biometricEnabled);
  }
}

final menuViewModelProvider = StateNotifierProvider<MenuViewModel, MenuState>((
  ref,
) {
  ref.watch(authViewModelProvider);
  return MenuViewModel(
    ref.read(menuRepositoryProvider),
  );
});
