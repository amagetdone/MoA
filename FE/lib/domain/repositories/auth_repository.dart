import 'package:more_app/domain/entities/user.dart';

abstract class AuthRepository {
  Future<User> loginWithKakao();
  Future<User> loginWithGoogle();
  Future<void> logout();
  Future<User?> getCurrentUser();
  Future<bool> isLoggedIn();
  Future<User?> autoLogin();
}
