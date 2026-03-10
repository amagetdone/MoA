import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/seed_auth_remote_datasource.dart';
import 'package:more_app/data/repositories/account_repository_impl.dart';
import 'package:more_app/data/repositories/goal_repository_impl.dart';
import 'package:more_app/data/repositories/home_repository_impl.dart';
import 'package:more_app/data/repositories/menu_repository_impl.dart';
import 'package:more_app/data/repositories/spending_repository_impl.dart';
import 'package:more_app/domain/repositories/account_repository.dart';
import 'package:more_app/domain/repositories/goal_repository.dart';
import 'package:more_app/domain/repositories/home_repository.dart';
import 'package:more_app/domain/repositories/menu_repository.dart';
import 'package:more_app/domain/repositories/spending_repository.dart';

final appLocalDataSourceProvider = Provider<AppLocalDataSource>((ref) {
  return AppLocalDataSourceImpl();
});

final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  return SeedAuthRemoteDataSource(
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final goalRepositoryProvider = Provider<GoalRepository>((ref) {
  return GoalRepositoryImpl(
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final homeRepositoryProvider = Provider<HomeRepository>((ref) {
  return HomeRepositoryImpl(
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final accountRepositoryProvider = Provider<AccountRepository>((ref) {
  return AccountRepositoryImpl(
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final spendingRepositoryProvider = Provider<SpendingRepository>((ref) {
  return SpendingRepositoryImpl(
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final menuRepositoryProvider = Provider<MenuRepository>((ref) {
  return MenuRepositoryImpl(
    localDataSource: ref.read(appLocalDataSourceProvider),
  );
});

final goalSketchVideoProvider = FutureProvider.family<Uint8List?, String>((
  ref,
  goalId,
) {
  return ref.read(goalRepositoryProvider).getGoalSketchVideo(goalId);
});
