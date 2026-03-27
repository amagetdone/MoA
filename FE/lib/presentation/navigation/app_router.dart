import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/presentation/pages/account/account_add_page.dart';
import 'package:more_app/presentation/pages/account/account_select_page.dart';
import 'package:more_app/presentation/pages/account/account_complete_page.dart';
import 'package:more_app/presentation/pages/account/account_manage_page.dart';
import 'package:more_app/presentation/pages/account/account_verify_page.dart';
import 'package:more_app/presentation/pages/asset/asset_detail_page.dart';
import 'package:more_app/presentation/pages/asset/asset_page.dart';
import 'package:more_app/domain/entities/transaction.dart' as tx;
import 'package:more_app/presentation/pages/goal/goal_create_page.dart';
import 'package:more_app/presentation/pages/goal/goal_detail_page.dart';
import 'package:more_app/presentation/pages/goal/goal_edit_page.dart';
import 'package:more_app/presentation/pages/goal/goal_list_page.dart';
import 'package:more_app/presentation/pages/goal/goal_loading_page.dart';
import 'package:more_app/presentation/pages/goal/goal_plan_page.dart';
import 'package:more_app/presentation/pages/goal/goal_preview_page.dart';
import 'package:more_app/presentation/pages/goal/goal_priority_page.dart';
import 'package:more_app/presentation/pages/home/home_page.dart';
import 'package:more_app/presentation/pages/login/login_page.dart';
import 'package:more_app/presentation/pages/main_shell/main_shell_page.dart';
import 'package:more_app/presentation/pages/menu/menu_page.dart';
import 'package:more_app/presentation/pages/notification/notification_page.dart';
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
import 'package:more_app/presentation/pages/chatbot/chatbot_page.dart';
import 'package:more_app/presentation/pages/splash/splash_page.dart';

abstract class AppRoutes {
  static const String splash = '/';
  static const String login = '/login';
  static const String sessionExpiredReason = 'session-expired';
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
  static const String onboardingAccountComplete =
      '/onboarding/account/complete';
  static const String onboardingGoal = '/onboarding/goal';
  static const String onboardingLoading = '/onboarding/loading';
  static const String onboardingIncome = '/onboarding/income';
  static const String onboardingAnalysis = '/onboarding/analysis';
  static const String onboardingPlan = '/onboarding/plan';

  static const String chatbot = '/chatbot';
  static const String profileEdit = '/profile/edit';
  static const String spendingBudget = '/spending/budget';

  static const String asset = '/spending/asset';
  static const String assetDetail = '/spending/asset/detail';
  static const String accountManage = '/spending/asset/manage';

  static const String accountAdd = '/account/add';
  static const String accountSelect = '/account/select';
  static const String accountVerify = '/account/verify';
  static const String accountComplete = '/account/complete';
  static const String goalCreate = '/goal/create';
  static const String goalEdit = '/goal/edit';
  static const String goalLoading = '/goal/loading';
  static const String goalPlan = '/goal/plan';
  static const String goalPreview = '/goal/preview';
  static const String goalDetail = '/goal/detail';
  static const String goalPriority = '/goal/priority';
  static const String settings = '/settings';
  static const String notifications = '/notifications';

  static String goalEditLocation(String goalId) => '$goalEdit?id=$goalId';
  static String loginLocation({String? reason}) =>
      reason == null ? login : '$login?reason=$reason';
  static String goalLoadingLocation(
    String goalId, {
    String source = 'create',
  }) => '$goalLoading?id=$goalId&source=$source';
  static String goalPreviewLocation(String goalId) => '$goalPreview?id=$goalId';
  static String goalDetailLocation(String goalId) => '$goalDetail?id=$goalId';
  static String assetLocation({String? accountNo}) {
    if (accountNo == null || accountNo.isEmpty) {
      return asset;
    }

    final query = Uri(queryParameters: {'accountNo': accountNo}).query;
    return '$asset?$query';
  }

  static String assetDetailLocation(String transactionId) =>
      '$assetDetail?id=$transactionId';
}

final _rootNavigatorKey = GlobalKey<NavigatorState>();
final _shellNavigatorHomeKey = GlobalKey<NavigatorState>(debugLabel: 'home');
final _shellNavigatorSpendingKey = GlobalKey<NavigatorState>(
  debugLabel: 'spending',
);
final _shellNavigatorGoalKey = GlobalKey<NavigatorState>(debugLabel: 'goal');
final _shellNavigatorMenuKey = GlobalKey<NavigatorState>(debugLabel: 'menu');

final appRouter = GoRouter(
  navigatorKey: _rootNavigatorKey,
  initialLocation: AppRoutes.splash,
  routes: [
    GoRoute(
      path: AppRoutes.splash,
      builder: (context, state) => const SplashPage(),
    ),
    GoRoute(
      path: AppRoutes.login,
      builder: (context, state) =>
          LoginPage(reason: state.uri.queryParameters['reason']),
    ),
    GoRoute(
      path: AppRoutes.onboarding,
      pageBuilder: (context, state) =>
          _fadeTransitionPage(state, const OnboardingPage()),
    ),
    GoRoute(
      path: AppRoutes.onboardingProfile,
      pageBuilder: (context, state) => _fadeTransitionPage(
        state,
        const ProfileSetupPage(isOnboarding: true),
      ),
    ),
    GoRoute(
      path: AppRoutes.onboardingAccount,
      pageBuilder: (context, state) => _fadeTransitionPage(
        state,
        const AccountManagePage(isOnboarding: true),
      ),
    ),
    GoRoute(
      path: AppRoutes.onboardingAccountSelect,
      pageBuilder: (context, state) => _fadeTransitionPage(
        state,
        const AccountSelectPage(isOnboarding: true),
      ),
    ),
    GoRoute(
      path: AppRoutes.onboardingAccountVerify,
      pageBuilder: (context, state) {
        final extra = state.extra as Map?;
        return _fadeTransitionPage(
          state,
          AccountVerifyPage(
            isOnboarding: true,
            bankName: extra?['bankName'] ?? '',
            accountNumber: extra?['accountNumber'] ?? '',
          ),
        );
      },
    ),
    GoRoute(
      path: AppRoutes.onboardingAccountComplete,
      pageBuilder: (context, state) {
        final extra = state.extra as Map?;
        return _fadeTransitionPage(
          state,
          AccountCompletePage(
            isOnboarding: true,
            bankName: extra?['bankName'] ?? '',
            accountNumber: extra?['accountNumber'] ?? '',
          ),
        );
      },
    ),
    GoRoute(
      path: AppRoutes.onboardingGoal,
      pageBuilder: (context, state) =>
          _fadeTransitionPage(state, const GoalCreatePage(isOnboarding: true)),
    ),
    GoRoute(
      path: AppRoutes.onboardingLoading,
      pageBuilder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? const {};
        return _fadeTransitionPage(
          state,
          OnboardingLoadingPage(nextRoute: extra['nextRoute'] as String?),
        );
      },
    ),
    GoRoute(
      path: AppRoutes.onboardingIncome,
      pageBuilder: (context, state) =>
          _fadeTransitionPage(state, const OnboardingIncomePage()),
    ),
    GoRoute(
      path: AppRoutes.onboardingAnalysis,
      pageBuilder: (context, state) =>
          _fadeTransitionPage(state, const OnboardingAnalysisPage()),
    ),
    GoRoute(
      path: AppRoutes.onboardingPlan,
      pageBuilder: (context, state) =>
          _fadeTransitionPage(state, const OnboardingPlanPage()),
    ),

    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) =>
          MainShellPage(navigationShell: navigationShell),
      branches: [
        StatefulShellBranch(
          navigatorKey: _shellNavigatorHomeKey,
          routes: [
            GoRoute(
              path: AppRoutes.home,
              builder: (context, state) => const HomePage(),
            ),
          ],
        ),
        StatefulShellBranch(
          navigatorKey: _shellNavigatorSpendingKey,
          routes: [
            GoRoute(
              path: AppRoutes.spendingTab,
              builder: (context, state) => const SpendingAnalysisPage(),
              routes: [
                GoRoute(
                  path: 'asset',
                  builder: (context, state) => AssetPage(
                    initialSelectedAccountNo:
                        state.uri.queryParameters['accountNo'],
                  ),
                  routes: [
                    GoRoute(
                      path: 'detail',
                      builder: (context, state) => AssetDetailPage(
                        transaction: state.extra as tx.Transaction,
                      ),
                    ),
                    GoRoute(
                      path: 'manage',
                      builder: (context, state) => const AccountManagePage(),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
        StatefulShellBranch(
          navigatorKey: _shellNavigatorGoalKey,
          routes: [
            GoRoute(
              path: AppRoutes.goalTab,
              builder: (context, state) => const GoalListPage(),
              routes: [
                GoRoute(
                  path: 'detail',
                  builder: (context, state) => GoalDetailPage(
                    goalId: state.uri.queryParameters['id'] ?? '',
                  ),
                ),
              ],
            ),
          ],
        ),
        StatefulShellBranch(
          navigatorKey: _shellNavigatorMenuKey,
          routes: [
            GoRoute(
              path: AppRoutes.menuTab,
              builder: (context, state) => const MenuPage(),
            ),
          ],
        ),
      ],
    ),

    GoRoute(
      path: AppRoutes.profileEdit,
      builder: (context, state) {
        final extra = state.extra as Map<String, String>?;
        return ProfileSetupPage(isOnboarding: false, initialData: extra);
      },
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
        final extra = state.extra as Map?;
        return AccountVerifyPage(
          isOnboarding: extra?['isOnboarding'] as bool? ?? false,
          bankName: extra?['bankName'] ?? '',
          accountNumber: extra?['accountNumber'] ?? '',
        );
      },
    ),
    GoRoute(
      path: AppRoutes.accountComplete,
      builder: (context, state) {
        final extra = state.extra as Map?;
        return AccountCompletePage(
          isOnboarding: false,
          bankName: extra?['bankName'] ?? '',
          accountNumber: extra?['accountNumber'] ?? '',
        );
      },
    ),
    GoRoute(
      path: AppRoutes.goalCreate,
      builder: (context, state) => const GoalCreatePage(isOnboarding: false),
    ),
    GoRoute(
      path: AppRoutes.goalEdit,
      builder: (context, state) =>
          GoalEditPage(goalId: state.uri.queryParameters['id'] ?? ''),
    ),
    GoRoute(
      path: AppRoutes.goalLoading,
      builder: (context, state) => GoalLoadingPage(
        goalId: state.uri.queryParameters['id'] ?? '',
        source: state.uri.queryParameters['source'] ?? 'create',
      ),
    ),
    GoRoute(
      path: AppRoutes.goalPlan,
      builder: (context, state) => const GoalPlanPage(),
    ),
    GoRoute(
      path: AppRoutes.goalPreview,
      builder: (context, state) =>
          GoalPreviewPage(goalId: state.uri.queryParameters['id'] ?? ''),
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
      path: AppRoutes.notifications,
      builder: (context, state) => const NotificationPage(),
    ),
    GoRoute(
      path: AppRoutes.spendingPattern,
      parentNavigatorKey: _rootNavigatorKey,
      builder: (context, state) => const SpendingPatternPage(),
    ),
    GoRoute(
      path: AppRoutes.settings,
      builder: (context, state) => const SettingsPage(),
    ),
    GoRoute(
      path: AppRoutes.chatbot,
      builder: (context, state) => const ChatbotPage(),
    ),
  ],
);

CustomTransitionPage<void> _fadeTransitionPage(
  GoRouterState state,
  Widget child,
) {
  return CustomTransitionPage<void>(
    key: state.pageKey,
    child: child,
    transitionDuration: Duration.zero,
    reverseTransitionDuration: Duration.zero,
    opaque: false,
    barrierColor: null,
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      return child;
    },
  );
}
