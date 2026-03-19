import 'package:more_app/domain/entities/auth_result.dart';
import 'package:more_app/domain/entities/user.dart';

abstract class AuthRepository {
  Future<AuthResult> loginWithKakao();
  Future<AuthResult> loginWithGoogle();
  Future<void> resetSocialLoginState();
  Future<void> logout();
  Future<User?> getCurrentUser();
  Future<bool> isLoggedIn();
  Future<AuthResult?> autoLogin();
  Future<AuthResult> loginWithTest();
}
