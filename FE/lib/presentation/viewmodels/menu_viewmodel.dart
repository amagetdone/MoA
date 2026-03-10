import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/utils/mock_data.dart';

class MenuState {
  final String userName;
  final String userEmail;
  final int activeGoalCount;
  final int achievedGoalCount;
  final int totalSaved;
  final bool notificationEnabled;
  final bool biometricEnabled;

  const MenuState({
    this.userName = '',
    this.userEmail = '',
    this.activeGoalCount = 0,
    this.achievedGoalCount = 0,
    this.totalSaved = 0,
    this.notificationEnabled = true,
    this.biometricEnabled = false,
  });

  MenuState copyWith({
    String? userName,
    String? userEmail,
    int? activeGoalCount,
    int? achievedGoalCount,
    int? totalSaved,
    bool? notificationEnabled,
    bool? biometricEnabled,
  }) {
    return MenuState(
      userName: userName ?? this.userName,
      userEmail: userEmail ?? this.userEmail,
      activeGoalCount: activeGoalCount ?? this.activeGoalCount,
      achievedGoalCount: achievedGoalCount ?? this.achievedGoalCount,
      totalSaved: totalSaved ?? this.totalSaved,
      notificationEnabled: notificationEnabled ?? this.notificationEnabled,
      biometricEnabled: biometricEnabled ?? this.biometricEnabled,
    );
  }
}

class MenuViewModel extends StateNotifier<MenuState> {
  MenuViewModel() : super(const MenuState()) {
    _loadData();
  }

  void _loadData() {
    state = MenuState(
      userName: '테스트유저',
      userEmail: 'test@more.com',
      activeGoalCount: MockData.activeGoals.length,
      achievedGoalCount: MockData.achievedGoals.length,
      totalSaved: MockData.goals.fold(0, (sum, g) => sum + g.currentAmount),
    );
  }

  void toggleNotification(bool value) {
    state = state.copyWith(notificationEnabled: value);
  }

  void toggleBiometric(bool value) {
    state = state.copyWith(biometricEnabled: value);
  }
}

final menuViewModelProvider =
    StateNotifierProvider<MenuViewModel, MenuState>((ref) {
  return MenuViewModel();
});
