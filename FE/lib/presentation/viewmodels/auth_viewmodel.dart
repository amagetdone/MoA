import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource_impl.dart';
import 'package:more_app/data/datasources/remote/fake_auth_remote_datasource.dart';
import 'package:more_app/data/repositories/auth_repository_impl.dart';
import 'package:more_app/domain/entities/user.dart';
import 'package:more_app/domain/repositories/auth_repository.dart';

part 'auth_viewmodel.freezed.dart';

@freezed
sealed class AuthState with _$AuthState {
  const factory AuthState.initial() = AuthInitial;
  const factory AuthState.loading() = AuthLoading;
  const factory AuthState.authenticated(User user) = AuthAuthenticated;
  const factory AuthState.unauthenticated() = AuthUnauthenticated;
  const factory AuthState.error(String message) = AuthError;
}

class AuthViewModel extends StateNotifier<AuthState> {
  final AuthRepository _repository;

  AuthViewModel(this._repository) : super(const AuthState.initial());

  Future<void> loginWithKakao() async {
    state = const AuthState.loading();
    try {
      final user = await _repository.loginWithKakao();
      state = AuthState.authenticated(user);
    } catch (e) {
      state = AuthState.error(e.toString());
    }
  }

  Future<void> loginWithGoogle() async {
    state = const AuthState.loading();
    try {
      final user = await _repository.loginWithGoogle();
      state = AuthState.authenticated(user);
    } catch (e) {
      state = AuthState.error(e.toString());
    }
  }

  Future<void> autoLogin() async {
    state = const AuthState.loading();
    try {
      final user = await _repository.autoLogin();
      if (user != null) {
        state = AuthState.authenticated(user);
      } else {
        state = const AuthState.unauthenticated();
      }
    } catch (_) {
      state = const AuthState.unauthenticated();
    }
  }

  Future<void> logout() async {
    state = const AuthState.loading();
    try {
      await _repository.logout();
      state = const AuthState.unauthenticated();
    } catch (e) {
      state = AuthState.error(e.toString());
    }
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepositoryImpl(
    remoteDataSource: FakeAuthRemoteDataSource(),
    localDataSource: AuthLocalDataSourceImpl(),
  );
});

final authViewModelProvider =
    StateNotifierProvider<AuthViewModel, AuthState>((ref) {
  return AuthViewModel(ref.read(authRepositoryProvider));
});
