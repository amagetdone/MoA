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
import 'package:more_app/presentation/pages/home/widgets/spending_rose_chart.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_empty_state.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(homeViewModelProvider.notifier).loadData();
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(homeViewModelProvider);

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(showBack: false, showLogo: true, showNotification: true),
      body: _buildBody(context, ref, state),
    );
  }

  Widget _buildBody(BuildContext context, WidgetRef ref, HomeState state) {
    final colors = AppColors.of(context);
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
              Text('홈 데이터를 불러오지 못했습니다.', style: AppTextStyles.bodyLg.copyWith(color: colors.textPrimary)),
              SizedBox(height: 8.h),
              Text(
                state.errorMessage!,
                textAlign: TextAlign.center,
                style: AppTextStyles.bodySm.copyWith(
                  color: colors.textTertiary,
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
    final colors = AppColors.of(context);
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
                color: colors.textPrimary,
              ),
            ),
            SizedBox(height: 12.h),
            Text(
              '계좌를 연결하면 소비 분석을 통해\n똑똑한 저축 계획을 세울게요!',
              textAlign: TextAlign.center,
              style: AppTextStyles.bodyMd.copyWith(
                color: colors.textTertiary,
              ),
            ),
            SizedBox(height: 32.h),
            GestureDetector(
              onTap: () async {
                await context.push(AppRoutes.accountAdd);
                if (mounted) {
                  ref.read(homeViewModelProvider.notifier).loadData();
                }
              },
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
                        color: colors.textPrimary,
                      ),
                    ),
                    SizedBox(height: 4.h),
                    Text(
                      '계좌를 추가하고 자산을 관리하세요!',
                      style: AppTextStyles.bodySm.copyWith(
                        color: colors.textTertiary,
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
    final topGoals = _sortedGoals(state.activeGoals).take(1).toList();

    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(20.w, 0, 20.w, 24.h),
      child: Column(
        children: [
          // 1) 목표
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
          SizedBox(height: 8.h),

          // 2) 이번 주 기간 표시
          Builder(builder: (context) {
            final colors = AppColors.of(context);
            final now = DateTime.now();
            final monday = now.subtract(Duration(days: now.weekday - 1));
            final sunday = monday.add(const Duration(days: 6));
            final daysLeft = sunday.difference(now).inDays + 1;
            final daysPassed = now.weekday;
            final progress = daysPassed / 7;

            return Padding(
              padding: EdgeInsets.symmetric(horizontal: 2.w, vertical: 4.h),
              child: Column(
                children: [
                  Row(
                    children: [
                      Text(
                        '${monday.month}/${monday.day} - ${sunday.month}/${sunday.day}',
                        style: AppTextStyles.caption.copyWith(
                          color: colors.textTertiary,
                          fontSize: 11.sp,
                        ),
                      ),
                      const Spacer(),
                      Text(
                        '$daysLeft일 남음',
                        style: AppTextStyles.caption.copyWith(
                          color: AppColors.orange500,
                          fontWeight: FontWeight.w600,
                          fontSize: 11.sp,
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 6.h),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(2.r),
                    child: LinearProgressIndicator(
                      value: progress,
                      minHeight: 3.h,
                      backgroundColor: colors.borderDefault.withValues(alpha: 0.4),
                      valueColor: AlwaysStoppedAnimation<Color>(
                        AppColors.orange500.withValues(alpha: 0.5),
                      ),
                    ),
                  ),
                ],
              ),
            );
          }),
          SizedBox(height: 4.h),

          // 3) 저축 + 여유자금 (가로)
          IntrinsicHeight(
            child: Row(
              children: [
                Expanded(
                  child: SavingsCard(
                    weeklySaved: state.weeklySaved,
                    savingTargetAmount: state.savingTargetAmount,
                    savingProgressRate: state.savingProgressRate,
                  ),
                ),
                SizedBox(width: 8.w),
                Expanded(
                  child: SpendingCard(
                    totalAmount: state.weeklyBudget,
                    usedAmount: state.weeklySpent,
                    remainingAmount: state.weeklyRemaining,
                    daysRemaining: state.daysRemaining,
                    dailyAvailable: state.dailyAvailable,
                  ),
                ),
              ],
            ),
          ),
          SizedBox(height: 8.h),

          // 이번 주 소비 rose 차트
          Builder(builder: (context) {
            final colors = AppColors.of(context);
            final spendingState = ref.watch(spendingViewModelProvider);
            final weekStart = spendingState.selectedWeekStart;
            final weekOfMonth = ((weekStart.day - 1) ~/ 7) + 1;
            final weekLabel = '${weekStart.month}월 $weekOfMonth주차';

            // TODO: 테스트용 하드코딩 데이터 - 테스트 후 원복
            const testCategories = [
              SpendingCategory(name: '식비', amount: 85000, budget: 120000, colorValue: 0xFFFF7043, iconKey: 'restaurant'),
              SpendingCategory(name: '교통', amount: 32000, budget: 50000, colorValue: 0xFF42A5F5, iconKey: 'directions_bus'),
              SpendingCategory(name: '쇼핑', amount: 67000, budget: 80000, colorValue: 0xFFAB47BC, iconKey: 'shopping_bag'),
              SpendingCategory(name: '카페', amount: 28000, budget: 40000, colorValue: 0xFF8D6E63, iconKey: 'coffee'),
              SpendingCategory(name: '문화', amount: 15000, budget: 30000, colorValue: 0xFF66BB6A, iconKey: 'movie'),
            ];
            const useTestData = true;
            final categories = useTestData ? testCategories : spendingState.categories;
            final hasData = categories.isNotEmpty && (useTestData || !spendingState.isLoading);

            if (hasData) {
              return Padding(
                padding: EdgeInsets.only(bottom: 8.h),
                child: SpendingRoseChart(
                  categories: categories,
                  weekLabel: weekLabel,
                ),
              );
            }
            return Padding(
              padding: EdgeInsets.only(bottom: 8.h),
              child: Container(
                width: double.infinity,
                padding: EdgeInsets.all(16.w),
                decoration: BoxDecoration(
                  color: colors.bgSurface,
                  borderRadius: BorderRadius.circular(20.r),
                  boxShadow: [
                    BoxShadow(color: colors.cardShadow, blurRadius: 8, offset: const Offset(0, 2)),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(
                          '이번 주 소비',
                          style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.w700, color: colors.textPrimary),
                        ),
                        const Spacer(),
                        Text(
                          weekLabel,
                          style: TextStyle(color: colors.textTertiary, fontSize: 11.sp),
                        ),
                      ],
                    ),
                    SizedBox(height: 40.h),
                    Center(child: Icon(Icons.pie_chart_outline, size: 48.w, color: colors.gray100)),
                    SizedBox(height: 12.h),
                    Center(
                      child: Text(
                        '아직 소비 내역이 없어요',
                        style: AppTextStyles.bodySm.copyWith(color: colors.textTertiary),
                      ),
                    ),
                    SizedBox(height: 32.h),
                  ],
                ),
              ),
            );
          }),

          // 자산 카드
          AssetCard(
            totalAsset: state.totalAsset,
            accounts: orderedAccounts,
          ),
        ],
      ),
    );
  }

  Widget _buildNoGoal(BuildContext context) {
    final colors = AppColors.of(context);
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
                color: colors.textPrimary,
              ),
            ),
            SizedBox(height: 12.h),
            Text(
              '목표에 도달할 수 있게 제가 도와드릴게요!',
              textAlign: TextAlign.center,
              style: AppTextStyles.bodyMd.copyWith(
                color: colors.textTertiary,
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
                        color: colors.textPrimary,
                      ),
                    ),
                    SizedBox(height: 4.h),
                    Text(
                      '목표를 세우고, 돈을 모아봐요!',
                      style: AppTextStyles.bodySm.copyWith(
                        color: colors.textTertiary,
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
      ..sort((a, b) => a.priority.compareTo(b.priority));
  }
}
