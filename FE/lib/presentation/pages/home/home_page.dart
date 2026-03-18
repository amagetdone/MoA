import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/pages/home/widgets/asset_card.dart';
import 'package:more_app/presentation/pages/home/widgets/goals_section_card.dart';
import 'package:more_app/presentation/pages/home/widgets/savings_card.dart';
import 'package:more_app/presentation/pages/home/widgets/spending_card.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_empty_state.dart';

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(homeViewModelProvider);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(showBack: false, showLogo: true, showNotification: true),
      body: _buildBody(context, ref, state),
    );
  }

  Widget _buildBody(BuildContext context, WidgetRef ref, HomeState state) {
    if (state.isLoading) {
      return const SizedBox.shrink();
    }

    if (state.errorMessage != null) {
      return Center(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('홈 데이터를 불러오지 못했습니다.', style: AppTextStyles.bodyLg),
              SizedBox(height: 8.h),
              Text(
                state.errorMessage!,
                textAlign: TextAlign.center,
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
              SizedBox(height: 16.h),
              FilledButton(
                onPressed: () {
                  ref.read(homeViewModelProvider.notifier).loadData();
                },
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      );
    }

    switch (state.status) {
      case HomeStatus.noAccount:
        return _buildNoAccount(context);
      case HomeStatus.noGoal:
        return _buildNoGoal(context);
      case HomeStatus.ready:
        return _buildDashboard(context, state, showGoalEmptyState: false);
    }
  }

  Widget _buildNoAccount(BuildContext context) {
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: 24.w),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            Text(
              '계좌를 연결해주세요.',
              style: AppTextStyles.headingLg.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
            SizedBox(height: 12.h),
            Text(
              '계좌를 연결하면 소비 분석을 통해\n똑똑한 저축 계획을 세울게요!',
              textAlign: TextAlign.center,
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
            SizedBox(height: 32.h),
            GestureDetector(
              onTap: () => context.push(AppRoutes.accountAdd),
              child: MoreCard(
                child: Column(
                  children: [
                    Container(
                      width: 48.w,
                      height: 48.w,
                      decoration: BoxDecoration(
                        color: AppColors.orange500,
                        borderRadius: BorderRadius.circular(12.r),
                      ),
                      child: Icon(Icons.add, color: Colors.white, size: 28.w),
                    ),
                    SizedBox(height: 12.h),
                    Text(
                      '새로운 계좌 연결하기',
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    SizedBox(height: 4.h),
                    Text(
                      '계좌를 추가하고 자산을 관리하세요!',
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDashboard(
    BuildContext context,
    HomeState state, {
    required bool showGoalEmptyState,
  }) {
    final orderedAccounts = _orderedAccounts(state.accounts);
    final topGoals = _sortedGoals(state.activeGoals).take(3).toList();

    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 28.h),
      child: Column(
        children: [
          // 예산 카드
          SpendingCard(
            weeklyBudget: state.weeklyBudget,
            weeklySpent: state.weeklySpent,
          ),
          SizedBox(height: 12.h),

          // 저축 카드
          SavingsCard(weeklySaved: state.weeklySaved),
          SizedBox(height: 12.h),

          // 총자산 / 계좌 카드
          AssetCard(
            totalAsset: state.totalAsset,
            accounts: orderedAccounts,
          ),
          SizedBox(height: 12.h),

          // 목표 카드
          showGoalEmptyState
              ? MoreEmptyState(
                  message: '첫 번째 저축 목표를 세워보세요',
                  buttonText: '목표 만들기',
                  onTap: () => context.push(AppRoutes.goalCreate),
                )
              : GoalsSectionCard(
                  goals: topGoals,
                  onViewAll: () => context.go(AppRoutes.goalTab),
                ),
        ],
      ),
    );
  }

  Widget _buildNoGoal(BuildContext context) {
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: 24.w),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            Text(
              '첫 번째 저축 목표를 세워보세요.',
              style: AppTextStyles.headingLg.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
            SizedBox(height: 12.h),
            Text(
              '목표에 도달할 수 있게 제가 도와드릴게요!',
              textAlign: TextAlign.center,
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
            SizedBox(height: 32.h),
            GestureDetector(
              onTap: () => context.push(AppRoutes.goalCreate),
              child: MoreCard(
                child: Column(
                  children: [
                    Container(
                      width: 48.w,
                      height: 48.w,
                      decoration: BoxDecoration(
                        color: AppColors.orange500,
                        borderRadius: BorderRadius.circular(12.r),
                      ),
                      child: Icon(Icons.add, color: Colors.white, size: 28.w),
                    ),
                    SizedBox(height: 12.h),
                    Text(
                      '첫 목표 세우러 가기',
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    SizedBox(height: 4.h),
                    Text(
                      '목표를 세우고, 돈을 모아봐요!',
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<Account> _orderedAccounts(List<Account> accounts) {
    final items = List<Account>.from(accounts);
    items.sort((a, b) {
      if (a.isPrimary == b.isPrimary) return a.id.compareTo(b.id);
      return a.isPrimary ? -1 : 1;
    });
    return items;
  }

  List<Goal> _sortedGoals(List<Goal> goals) {
    return List<Goal>.from(goals)
      ..sort((a, b) => b.progressRate.compareTo(a.progressRate));
  }
}
