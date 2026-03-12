import 'package:more_app/domain/entities/home_dashboard.dart';

abstract class HomeRepository {
  Future<HomeDashboard> fetchHomeDashboard();
}
