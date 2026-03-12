import 'package:more_app/domain/entities/menu_summary.dart';

abstract class MenuRemoteDataSource {
  Future<MenuSummary> fetchMenuDashboard();
  Future<MenuSummary> updateMenuPreferences({
    bool? notificationEnabled,
    bool? biometricEnabled,
  });
}
