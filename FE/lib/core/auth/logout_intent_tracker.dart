class LogoutIntentTracker {
  LogoutIntentTracker._();

  static bool _userInitiatedLogoutInProgress = false;

  static bool get isUserInitiatedLogoutInProgress =>
      _userInitiatedLogoutInProgress;

  static void markUserInitiatedLogout() {
    _userInitiatedLogoutInProgress = true;
  }

  static void clear() {
    _userInitiatedLogoutInProgress = false;
  }
}
