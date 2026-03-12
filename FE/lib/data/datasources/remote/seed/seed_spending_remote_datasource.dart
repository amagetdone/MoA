import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';
import 'package:more_app/data/datasources/remote/spending_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot_dto.dart';

class SeedSpendingRemoteDataSource implements SpendingRemoteDataSource {
  SeedSpendingRemoteDataSource({required SeedDataStore dataStore})
    : _dataStore = dataStore;

  final SeedDataStore _dataStore;

  @override
  Future<SpendingSnapshotDto> fetchSpendingSnapshotDto() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());
    return snapshot.spending;
  }

  @override
  Future<SpendingSnapshotDto> updateSpendingSnapshotDto(
    SpendingSnapshotDto spending,
  ) async {
    final snapshot = await _dataStore.mutate(
      (current) => current.copyWith(spending: spending),
    );
    return snapshot.spending;
  }
}
