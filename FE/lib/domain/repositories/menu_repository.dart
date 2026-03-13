import 'package:more_app/domain/entities/menu_summary.dart';

abstract class MenuRepository {
  Future<MenuSummary> fetchMenuSummary();
  Future<MenuSummary> updateNotification(bool enabled);
  Future<MenuSummary> updateBiometric(bool enabled);
}
