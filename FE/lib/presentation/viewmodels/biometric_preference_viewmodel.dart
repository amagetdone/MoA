import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/data/datasources/local/app_local_datasource.dart';

class BiometricPreferenceViewModel extends StateNotifier<bool> {
  BiometricPreferenceViewModel(this._localDataSource) : super(false) {
    _load();
  }

  final AppLocalDataSource _localDataSource;
  bool _hasExplicitUpdate = false;

  Future<void> _load() async {
    final preferences = await _localDataSource.getMenuPreferences();
    if (_hasExplicitUpdate) {
      return;
    }

    state = preferences.biometricEnabled;
  }

  Future<void> setEnabled(bool enabled) async {
    _hasExplicitUpdate = true;
    await _localDataSource.updateMenuPreferences(biometricEnabled: enabled);
    state = enabled;
  }
}
