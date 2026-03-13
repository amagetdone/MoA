import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/presentation/pages/account/account_add_page.dart';
import 'package:more_app/presentation/pages/account/account_select_page.dart';
import 'package:more_app/presentation/pages/account/account_complete_page.dart';
import 'package:more_app/presentation/pages/account/account_manage_page.dart';
import 'package:more_app/presentation/pages/account/account_verify_page.dart';
import 'package:more_app/presentation/pages/asset/asset_detail_page.dart';
import 'package:more_app/presentation/pages/asset/asset_page.dart';
import 'package:more_app/presentation/pages/goal/goal_create_page.dart';
import 'package:more_app/presentation/pages/goal/goal_detail_page.dart';
import 'package:more_app/presentation/pages/goal/goal_list_page.dart';
import 'package:more_app/presentation/pages/goal/goal_priority_page.dart';
import 'package:more_app/presentation/pages/home/home_page.dart';
import 'package:more_app/presentation/pages/login/login_page.dart';
import 'package:more_app/presentation/pages/main_shell/main_shell_page.dart';
import 'package:more_app/presentation/pages/menu/menu_page.dart';
import 'package:more_app/presentation/pages/menu/settings_page.dart';
import 'package:more_app/presentation/pages/onboarding/onboarding_analysis_page.dart';
import 'package:more_app/presentation/pages/onboarding/onboarding_income_page.dart';
import 'package:more_app/presentation/pages/onboarding/onboarding_loading_page.dart';
import 'package:more_app/presentation/pages/onboarding/onboarding_page.dart';
import 'package:more_app/presentation/pages/onboarding/onboarding_plan_page.dart';
import 'package:more_app/presentation/pages/profile/profile_setup_page.dart';
import 'package:more_app/presentation/pages/spending/spending_analysis_page.dart';
import 'package:more_app/presentation/pages/spending/spending_budget_page.dart';
import 'package:more_app/presentation/pages/spending/spending_pattern_page.dart';
import 'package:more_app/presentation/pages/splash/splash_page.dart';

abstract class AppRoutes {
  static const String splash = '/';
  static const String login = '/login';
  static const String home = '/home';
  static const String goalTab = '/goal';
  static const String spendingTab = '/spending';
  static const String spendingPattern = '/spending/pattern';
  static const String menuTab = '/menu';

  static const String onboarding = '/onboarding';
  static const String onboardingProfile = '/onboarding/profile';
  static const String onboardingAccount = '/onboarding/account';
  static const String onboardingAccountSelect = '/onboarding/account/select';
  static const String onboardingAccountVerify = '/onboarding/account/verify';
  static const String onboardingAccountComplete = '/onboarding/account/complete';
  static const String onboardingGoal = '/onboarding/goal';
  static const String onboardingLoading = '/onboarding/loading';
  static const String onboardingIncome = '/onboarding/income';
  static const String onboardingAnalysis = '/onboarding/analysis';
  static const String onboardingPlan = '/onboarding/plan';

  static const String profileEdit = '/profile/edit';
  static const String spendingBudget = '/spending/budget';
  
  // 자산 및 거래내역 경로를 분석 탭 하위로 지정 (하단바 유지)
  static const String asset = '/spending/asset';
  static const String assetDetail = '/spending/asset/detail';
  static const String accountManage = '/spending/asset/manage';
  
  static const String accountAdd = '/account/add';
  static const String accountSelect = '/account/select';
  static const String accountVerify = '/account/verify';
  static const String accountComplete = '/account/complete';
  static const String goalCreate = '/goal/create';
  static const String goalDetail = '/goal/detail';
  static const String goalPriority = '/goal/priority';
  static const String settings = '/settings';

  static String goalDetailLocation(String goalId) => '$goalDetail?id=$goalId';
  static String assetDetailLocation(String transactionId) => '$assetDetail?id=$transactionId';
}

final _rootNavigatorKey = GlobalKey<NavigatorState>();
final _shellNavigatorHomeKey = GlobalKey<NavigatorState>(debugLabel: 'home');
final _shellNavigatorSpendingKey = GlobalKey<NavigatorState>(debugLabel: 'spending');
final _shellNavigatorGoalKey = GlobalKey<NavigatorState>(debugLabel: 'goal');
final _shellNavigatorMenuKey = GlobalKey<NavigatorState>(debugLabel: 'menu');

final appRouter = GoRouter(
  navigatorKey: _rootNavigatorKey,
  initialLocation: AppRoutes.splash,
  routes: [
    GoRoute(path: AppRoutes.splash, builder: (context, state) => const SplashPage()),
    GoRoute(path: AppRoutes.login, builder: (context, state) => const LoginPage()),
    GoRoute(path: AppRoutes.onboarding, builder: (context, state) => const OnboardingPage()),
    GoRoute(path: AppRoutes.onboardingProfile, builder: (context, state) => const ProfileSetupPage(isOnboarding: true)),
    GoRoute(path: AppRoutes.onboardingAccount, builder: (context, state) => const AccountAddPage(isOnboarding: true)),
    GoRoute(path: AppRoutes.onboardingAccountSelect, builder: (context, state) => const AccountSelectPage(isOnboarding: true)),
    GoRoute(path: AppRoutes.onboardingAccountVerify, builder: (context, state) {
      final extra = state.extra as Map?;
      return AccountVerifyPage(isOnboarding: true, bankName: extra?['bankName'] ?? '', accountNumber: extra?['accountNumber'] ?? '');
    }),
    GoRoute(path: AppRoutes.onboardingAccountComplete, builder: (context, state) {
      final extra = state.extra as Map?;
      return AccountCompletePage(isOnboarding: true, bankName: extra?['bankName'] ?? '', accountNumber: extra?['accountNumber'] ?? '');
    }),
    GoRoute(path: AppRoutes.onboardingGoal, builder: (context, state) => const GoalCreatePage(isOnboarding: true)),
    GoRoute(path: AppRoutes.onboardingLoading, builder: (context, state) => const OnboardingLoadingPage()),
    GoRoute(path: AppRoutes.onboardingIncome, builder: (context, state) => const OnboardingIncomePage()),
    GoRoute(path: AppRoutes.onboardingAnalysis, builder: (context, state) => const OnboardingAnalysisPage()),
    GoRoute(path: AppRoutes.onboardingPlan, builder: (context, state) => const OnboardingPlanPage()),
    
    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) => MainShellPage(navigationShell: navigationShell),
      branches: [
        StatefulShellBranch(
          navigatorKey: _shellNavigatorHomeKey,
          routes: [GoRoute(path: AppRoutes.home, builder: (context, state) => const HomePage())],
        ),
        StatefulShellBranch(
          navigatorKey: _shellNavigatorSpendingKey,
          routes: [
            GoRoute(
              path: AppRoutes.spendingTab,
              builder: (context, state) => const SpendingAnalysisPage(),
              routes: [
                GoRoute(path: 'pattern', builder: (context, state) => const SpendingPatternPage()),
                GoRoute(
                  path: 'asset', 
                  builder: (context, state) => const AssetPage(),
                  routes: [
                    GoRoute(path: 'detail', builder: (context, state) => AssetDetailPage(transactionId: state.uri.queryParameters['id'] ?? '')),
                    GoRoute(path: 'manage', builder: (context, state) => const AccountManagePage()),
                  ]
                ),
              ],
            ),
          ],
        ),
        StatefulShellBranch(
          navigatorKey: _shellNavigatorGoalKey,
          routes: [GoRoute(path: AppRoutes.goalTab, builder: (context, state) => const GoalListPage())],
        ),
        StatefulShellBranch(
          navigatorKey: _shellNavigatorMenuKey,
          routes: [GoRoute(path: AppRoutes.menuTab, builder: (context, state) => const MenuPage())],
        ),
      ],
    ),

    GoRoute(path: AppRoutes.profileEdit, builder: (context, state) => const ProfileSetupPage(isOnboarding: false)),
    GoRoute(path: AppRoutes.accountAdd, builder: (context, state) => const AccountAddPage(isOnboarding: false)),
    GoRoute(path: AppRoutes.accountSelect, builder: (context, state) => const AccountSelectPage(isOnboarding: false)),
    GoRoute(path: AppRoutes.accountVerify, builder: (context, state) {
      final extra = state.extra as Map?;
      return AccountVerifyPage(
        isOnboarding: extra?['isOnboarding'] as bool? ?? false,
        bankName: extra?['bankName'] ?? '',
        accountNumber: extra?['accountNumber'] ?? '',
      );
    }),
    GoRoute(path: AppRoutes.accountComplete, builder: (context, state) {
      final extra = state.extra as Map?;
      return AccountCompletePage(isOnboarding: false, bankName: extra?['bankName'] ?? '', accountNumber: extra?['accountNumber'] ?? '');
    }),
    GoRoute(path: AppRoutes.goalCreate, builder: (context, state) => const GoalCreatePage(isOnboarding: false)),
    GoRoute(path: AppRoutes.goalDetail, builder: (context, state) => GoalDetailPage(goalId: state.uri.queryParameters['id'] ?? '')),
    GoRoute(path: AppRoutes.goalPriority, builder: (context, state) => const GoalPriorityPage()),
    GoRoute(path: AppRoutes.spendingBudget, builder: (context, state) => const SpendingBudgetPage()),
    GoRoute(path: AppRoutes.settings, builder: (context, state) => const SettingsPage()),
  ],
);
