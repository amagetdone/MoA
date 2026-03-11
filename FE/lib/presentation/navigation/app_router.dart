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
import 'package:more_app/presentation/pages/splash/splash_page.dart';

abstract class AppRoutes {
  static const String splash = '/';
  static const String login = '/login';
  static const String home = '/home';
  static const String goalTab = '/goal';
  static const String spendingTab = '/spending';
  static const String menuTab = '/menu';

  // Onboarding
  static const String onboarding = '/onboarding';
  static const String onboardingProfile = '/onboarding/profile';
  static const String onboardingAccount = '/onboarding/account';
  static const String onboardingAccountSelect = '/onboarding/account/select';
  static const String onboardingAccountVerify = '/onboarding/account/verify';
  static const String onboardingAccountComplete =
      '/onboarding/account/complete';
  static const String onboardingGoal = '/onboarding/goal';
  static const String onboardingLoading = '/onboarding/loading';
  static const String onboardingIncome = '/onboarding/income';
  static const String onboardingAnalysis = '/onboarding/analysis';
  static const String onboardingPlan = '/onboarding/plan';

  // Profile
  static const String profileEdit = '/profile/edit';

  // Account
  static const String accountManage = '/account/manage';
  static const String accountAdd = '/account/add';
  static const String accountSelect = '/account/select';
  static const String accountVerify = '/account/verify';
  static const String accountComplete = '/account/complete';

  // Goal
  static const String goalCreate = '/goal/create';
  static const String goalDetail = '/goal/detail';
  static const String goalPriority = '/goal/priority';

  // Spending
  static const String spendingBudget = '/spending/budget';

  // Asset
  static const String asset = '/asset';
  static const String assetDetail = '/asset/detail';

  // Settings
  static const String settings = '/settings';

  static String goalDetailLocation(String goalId) => '$goalDetail?id=$goalId';
  static String assetDetailLocation(String transactionId) =>
      '$assetDetail?id=$transactionId';
}

final _rootNavigatorKey = GlobalKey<NavigatorState>();

final appRouter = GoRouter(
  navigatorKey: _rootNavigatorKey,
  initialLocation: AppRoutes.splash,
  routes: [
    // ── Pre-auth routes ──
    GoRoute(
      path: AppRoutes.splash,
      builder: (context, state) => const SplashPage(),
    ),
    GoRoute(
      path: AppRoutes.login,
      builder: (context, state) => const LoginPage(),
    ),

    // ── Onboarding flow ──
    GoRoute(
      path: AppRoutes.onboarding,
      builder: (context, state) => const OnboardingPage(),
    ),
    GoRoute(
      path: AppRoutes.onboardingProfile,
      builder: (context, state) => const ProfileSetupPage(isOnboarding: true),
    ),
    GoRoute(
      path: AppRoutes.onboardingAccount,
      builder: (context, state) => const AccountAddPage(isOnboarding: true),
    ),
    GoRoute(
      path: AppRoutes.onboardingAccountSelect,
      builder: (context, state) => const AccountSelectPage(isOnboarding: true),
    ),
    GoRoute(
      path: AppRoutes.onboardingAccountVerify,
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? const {};
        return AccountVerifyPage(
          isOnboarding: extra['isOnboarding'] as bool? ?? true,
          bankName: extra['bankName'] as String? ?? '',
          accountNumber: extra['accountNumber'] as String? ?? '',
        );
      },
    ),
    GoRoute(
      path: AppRoutes.onboardingAccountComplete,
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? const {};
        return AccountCompletePage(
          isOnboarding: extra['isOnboarding'] as bool? ?? true,
          bankName: extra['bankName'] as String? ?? '',
          accountNumber: extra['accountNumber'] as String? ?? '',
        );
      },
    ),
    GoRoute(
      path: AppRoutes.onboardingGoal,
      builder: (context, state) => const GoalCreatePage(isOnboarding: true),
    ),
    GoRoute(
      path: AppRoutes.onboardingLoading,
      builder: (context, state) => const OnboardingLoadingPage(),
    ),
    GoRoute(
      path: AppRoutes.onboardingIncome,
      builder: (context, state) => const OnboardingIncomePage(),
    ),
    GoRoute(
      path: AppRoutes.onboardingAnalysis,
      builder: (context, state) => const OnboardingAnalysisPage(),
    ),
    GoRoute(
      path: AppRoutes.onboardingPlan,
      builder: (context, state) => const OnboardingPlanPage(),
    ),

    // ── Main app with bottom tabs ──
    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) =>
          MainShellPage(navigationShell: navigationShell),
      branches: [
        // Home tab (홈)
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: AppRoutes.home,
              builder: (context, state) => const HomePage(),
            ),
          ],
        ),
        // Spending tab (분석)
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: AppRoutes.spendingTab,
              builder: (context, state) => const SpendingAnalysisPage(),
            ),
          ],
        ),
        // Goal tab (목표)
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: AppRoutes.goalTab,
              builder: (context, state) => const GoalListPage(),
            ),
          ],
        ),
        // Menu tab (내 정보)
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: AppRoutes.menuTab,
              builder: (context, state) => const MenuPage(),
            ),
          ],
        ),
      ],
    ),

    // ── Full-screen routes (outside shell) ──
    GoRoute(
      path: AppRoutes.profileEdit,
      builder: (context, state) => const ProfileSetupPage(isOnboarding: false),
    ),
    GoRoute(
      path: AppRoutes.accountManage,
      builder: (context, state) => const AccountManagePage(),
    ),
    GoRoute(
      path: AppRoutes.accountAdd,
      builder: (context, state) => const AccountAddPage(isOnboarding: false),
    ),
    GoRoute(
      path: AppRoutes.accountSelect,
      builder: (context, state) => const AccountSelectPage(isOnboarding: false),
    ),
    GoRoute(
      path: AppRoutes.accountVerify,
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? const {};
        return AccountVerifyPage(
          isOnboarding: extra['isOnboarding'] as bool? ?? false,
          bankName: extra['bankName'] as String? ?? '',
          accountNumber: extra['accountNumber'] as String? ?? '',
        );
      },
    ),
    GoRoute(
      path: AppRoutes.accountComplete,
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? const {};
        return AccountCompletePage(
          isOnboarding: extra['isOnboarding'] as bool? ?? false,
          bankName: extra['bankName'] as String? ?? '',
          accountNumber: extra['accountNumber'] as String? ?? '',
        );
      },
    ),
    GoRoute(
      path: AppRoutes.goalCreate,
      builder: (context, state) => const GoalCreatePage(isOnboarding: false),
    ),
    GoRoute(
      path: AppRoutes.goalDetail,
      builder: (context, state) {
        final goalId = state.uri.queryParameters['id'] ?? '';
        return GoalDetailPage(goalId: goalId);
      },
    ),
    GoRoute(
      path: AppRoutes.goalPriority,
      builder: (context, state) => const GoalPriorityPage(),
    ),
    GoRoute(
      path: AppRoutes.spendingBudget,
      builder: (context, state) => const SpendingBudgetPage(),
    ),
    GoRoute(
      path: AppRoutes.asset,
      builder: (context, state) => const AssetPage(),
    ),
    GoRoute(
      path: AppRoutes.assetDetail,
      builder: (context, state) {
        final txId = state.uri.queryParameters['id'] ?? '';
        return AssetDetailPage(transactionId: txId);
      },
    ),
    GoRoute(
      path: AppRoutes.settings,
      builder: (context, state) => const SettingsPage(),
    ),
  ],
);
