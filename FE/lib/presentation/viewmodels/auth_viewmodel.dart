import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/auth_result.dart';
import 'package:more_app/domain/repositories/auth_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

sealed class AuthState {
  const AuthState();
}

class AuthInitial extends AuthState {
  const AuthInitial();
}

class AuthLoading extends AuthState {
  const AuthLoading();
}

class AuthAuthenticated extends AuthState {
  final AuthResult authResult;
  const AuthAuthenticated(this.authResult);
}

class AuthAutoLoginSuccess extends AuthState {
  final AuthResult authResult;
  const AuthAutoLoginSuccess(this.authResult);
}

class AuthUnauthenticated extends AuthState {
  const AuthUnauthenticated();
}

class AuthError extends AuthState {
  final String message;
  const AuthError(this.message);
}

class AuthViewModel extends StateNotifier<AuthState> {
  final AuthRepository _repository;

  AuthViewModel(this._repository) : super(const AuthInitial());

  // TODO: TEMP 하드코딩 - 테스트 후 원복 필요
  AuthResult _overrideResult(AuthResult result) {
    return AuthResult(status: result.status, account: true, plan: true);
  }

  Future<void> loginWithKakao() async {
    state = const AuthLoading();
    try {
      final result = await _repository.loginWithKakao();
      state = AuthAuthenticated(_overrideResult(result));
    } catch (e) {
      state = AuthError(e.toString());
    }
  }

  Future<void> loginWithGoogle() async {
    state = const AuthLoading();
    try {
      final result = await _repository.loginWithGoogle();
      state = AuthAuthenticated(_overrideResult(result));
    } catch (e) {
      state = AuthError(e.toString());
    }
  }

  Future<void> autoLogin() async {
    state = const AuthLoading();
    try {
      final result = await _repository.autoLogin();
      if (result != null) {
        state = AuthAutoLoginSuccess(_overrideResult(result));
      } else {
        state = const AuthUnauthenticated();
      }
    } catch (_) {
      state = const AuthUnauthenticated();
    }
  }

  Future<void> logout() async {
    state = const AuthLoading();
    try {
      await _repository.logout();
      state = const AuthUnauthenticated();
    } catch (e) {
      state = AuthError(e.toString());
    }
  }
}

final authViewModelProvider = StateNotifierProvider<AuthViewModel, AuthState>((
  ref,
) {
  return AuthViewModel(ref.read(authRepositoryProvider));
});
