import 'package:more_app/domain/entities/user.dart';

class MenuSummary {
  final User user;
  final int activeGoalCount;
  final int achievedGoalCount;
  final int totalSaved;
  final bool notificationEnabled;
  final bool biometricEnabled;

  const MenuSummary({
    required this.user,
    this.activeGoalCount = 0,
    this.achievedGoalCount = 0,
    this.totalSaved = 0,
    this.notificationEnabled = true,
    this.biometricEnabled = false,
  });
}
