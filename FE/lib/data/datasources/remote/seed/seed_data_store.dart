import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:more_app/data/models/app_snapshot_dto.dart';

class SeedDataStore {
  SeedDataStore({AssetBundle? assetBundle})
    : _assetBundle = assetBundle ?? rootBundle;

  static const _seedAssetPath = 'assets/seeds/app_state.json';

  static AppSnapshotDto? _sessionSnapshot;

  final AssetBundle _assetBundle;

  Future<AppSnapshotDto> ensureSessionSnapshot() async {
    if (_sessionSnapshot != null) {
      return _sessionSnapshot!;
    }

    final jsonString = await _assetBundle.loadString(_seedAssetPath);
    _sessionSnapshot = AppSnapshotDto.fromJson(
      jsonDecode(jsonString) as Map<String, dynamic>,
    );
    return _sessionSnapshot!;
  }

  Future<AppSnapshotDto> mutate(
    AppSnapshotDto Function(AppSnapshotDto snapshot) transform,
  ) async {
    await Future.delayed(const Duration(milliseconds: 180));
    final current = await ensureSessionSnapshot();
    _sessionSnapshot = transform(current);
    return clone(_sessionSnapshot!);
  }

  AppSnapshotDto clone(AppSnapshotDto snapshot) {
    return AppSnapshotDto.fromJson(
      jsonDecode(jsonEncode(snapshot.toJson())) as Map<String, dynamic>,
    );
  }
}
