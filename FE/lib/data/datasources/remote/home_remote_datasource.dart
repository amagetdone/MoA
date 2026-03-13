import 'package:more_app/domain/entities/home_dashboard.dart';

abstract class HomeRemoteDataSource {
  Future<HomeDashboard> fetchHomeDashboard();
}
