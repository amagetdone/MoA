import 'dart:async';

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
  final bool isTest;

  const AuthAuthenticated(this.authResult, {this.isTest = false});
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
  AuthViewModel(this._repository) : super(const AuthInitial());

  final AuthRepository _repository;
  int _socialLoginAttempt = 0;

  bool _isSilentLoginInterruption(Object error) {
    final message = error.toString().toLowerCase();
    return message.contains('popup_closed') ||
        message.contains('취소') ||
        message.contains('cancel') ||
        message.contains('timeout') ||
        message.contains('timed out');
  }

  Future<void> _runSocialLogin(Future<AuthResult> Function() action) async {
    final attempt = ++_socialLoginAttempt;
    state = const AuthLoading();

    try {
      final result = await action();
      if (attempt != _socialLoginAttempt) {
        return;
      }

      state = AuthAuthenticated(result);
    } catch (error) {
      if (attempt != _socialLoginAttempt) {
        return;
      }

      if (_isSilentLoginInterruption(error)) {
        await resetInterruptedLogin();
        return;
      }

      state = AuthError(error.toString());
    }
  }

  Future<void> loginWithKakao() => _runSocialLogin(_repository.loginWithKakao);

  Future<void> loginWithTest({required int id}) async {
    state = const AuthLoading();
    try {
      final result = await _repository.loginWithTest(id: id);
      state = AuthAuthenticated(result, isTest: true);
    } catch (error) {
      state = AuthError(error.toString());
    }
  }

  Future<void> loginWithGoogle() => _runSocialLogin(_repository.loginWithGoogle);

  Future<void> resetInterruptedLogin() async {
    if (state is! AuthLoading) {
      return;
    }

    _socialLoginAttempt++;
    unawaited(_repository.resetSocialLoginState());
    state = const AuthUnauthenticated();
  }

  Future<void> autoLogin() async {
    state = const AuthLoading();
    try {
      final result = await _repository.autoLogin();
      if (result != null) {
        state = AuthAutoLoginSuccess(result);
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
    } catch (error) {
      state = AuthError(error.toString());
    }
  }
}

final authViewModelProvider = StateNotifierProvider<AuthViewModel, AuthState>((
  ref,
) {
  return AuthViewModel(ref.read(authRepositoryProvider));
});
