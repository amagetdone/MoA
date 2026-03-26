import 'package:shared_preferences/shared_preferences.dart';

class TutorialService {
  static const _prefix = 'tutorial_shown_';

  static Future<bool> isShown(String pageKey) async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool('$_prefix$pageKey') ?? false;
  }

  static Future<void> markShown(String pageKey) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('$_prefix$pageKey', true);
  }

  static Future<void> resetAll() async {
    final prefs = await SharedPreferences.getInstance();
    final keys = prefs.getKeys().where((k) => k.startsWith(_prefix));
    for (final k in keys) {
      await prefs.remove(k);
    }
  }

  // 페이지 키 상수
  static const home = 'home';
  static const spendingBudget = 'spending_budget';
  static const spendingAnalysis = 'spending_analysis';
  static const goalList = 'goal_list';
}
