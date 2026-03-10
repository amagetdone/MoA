import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_empty_state.dart';
import 'package:more_app/presentation/pages/home/widgets/spending_card.dart';
import 'package:more_app/presentation/pages/home/widgets/savings_card.dart';
import 'package:more_app/presentation/pages/home/widgets/asset_card.dart';
import 'package:more_app/presentation/pages/home/widgets/goal_summary_card.dart';

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(homeViewModelProvider);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(
        title: 'MORE',
        showBack: false,
        showNotification: true,
      ),
      body: _buildBody(context, state),
    );
  }

  Widget _buildBody(BuildContext context, HomeState state) {
    switch (state.status) {
      case HomeStatus.noAccount:
        return _buildNoAccount(context);
      case HomeStatus.noGoal:
        return _buildNoGoal(context, state);
      case HomeStatus.ready:
        return _buildReady(context, state);
    }
  }

  Widget _buildNoAccount(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: MoreEmptyState(
        message: '계좌를 연결해주세요\n모아 서비스를 시작하려면\n계좌 연동이 필요해요',
        buttonText: '계좌 연결하기',
        onTap: () => context.push(AppRoutes.onboardingAccount),
      ),
    );
  }

  Widget _buildNoGoal(BuildContext context, HomeState state) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          MoreCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '총 자산',
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  CurrencyFormatter.formatKRW(state.totalAsset),
                  style: AppTextStyles.headingLg,
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          MoreEmptyState(
            message: '첫 번째 저축 목표를 세워보세요',
            buttonText: '목표 만들기',
            onTap: () => context.push(AppRoutes.goalCreate),
          ),
        ],
      ),
    );
  }

  Widget _buildReady(BuildContext context, HomeState state) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          SpendingCard(
            weeklyBudget: state.weeklyBudget,
            weeklySpent: state.weeklySpent,
          ),
          const SizedBox(height: 16),
          SavingsCard(
            weeklySaved: state.weeklySaved,
          ),
          const SizedBox(height: 16),
          AssetCard(
            totalAsset: state.totalAsset,
          ),
          const SizedBox(height: 16),
          ...state.activeGoals.map(
            (goal) => Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: GoalSummaryCard(goal: goal),
            ),
          ),
        ],
      ),
    );
  }
}
