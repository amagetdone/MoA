import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/repositories/auth_repository.dart';
import 'package:more_app/domain/repositories/menu_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

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
  MenuViewModel(this._repository, this._authRepository) : super(const MenuState(isLoading: true)) {
    loadData();
  }

  final MenuRepository _repository;
  final AuthRepository _authRepository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final summary = await _repository.fetchMenuSummary();

      String userName = summary.user.name;
      String userEmail = summary.user.email;
      String? profileImageUrl = summary.user.profileImageUrl;
      try {
        final user = await _authRepository.getCurrentUser();
        if (user != null) {
          userName = user.name;
          userEmail = user.email;
          profileImageUrl = user.profileImageUrl;
        }
      } catch (_) {}

      state = state.copyWith(
        isLoading: false,
        userName: userName,
        userEmail: userEmail,
        profileImageUrl: profileImageUrl,
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
  return MenuViewModel(
    ref.read(menuRepositoryProvider),
    ref.read(authRepositoryProvider),
  );
});
