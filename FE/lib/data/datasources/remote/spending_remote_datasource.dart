import 'package:more_app/data/models/app_snapshot_dto.dart';

abstract class SpendingRemoteDataSource {
  Future<SpendingSnapshotDto> fetchSpendingSnapshotDto();
  Future<SpendingSnapshotDto> updateSpendingSnapshotDto(SpendingSnapshotDto snapshot);
}
