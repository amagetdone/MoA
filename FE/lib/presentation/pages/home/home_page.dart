import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/pages/home/widgets/asset_card.dart';
import 'package:more_app/presentation/pages/home/widgets/goals_section_card.dart';
import 'package:more_app/presentation/pages/home/widgets/savings_card.dart';
import 'package:more_app/presentation/pages/home/widgets/spending_card.dart';
import 'package:more_app/presentation/pages/home/widgets/spending_rose_chart.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  late final PageController _homeSpendingPageController;
  int _homeSpendingPageIndex = 0;
  bool _isHomeSpendingLoading = true;
  List<_HomeSpendingPeriod> _homeSpendingPeriods = const [];
  Map<int, List<SpendingCategory>> _homeSpendingCategoryMap = const {};

  DateTime get _currentWeekStart {
    final now = DateTime.now();
    final monday = now.subtract(Duration(days: now.weekday - 1));
    return DateTime(monday.year, monday.month, monday.day);
  }

  DateTime _weekStartForOffset(int offset) => _currentWeekStart.subtract(
        Duration(days: 7 * offset),
      );

  @override
  void initState() {
    super.initState();
    _homeSpendingPeriods = _buildHomeSpendingPeriods();
    _homeSpendingPageIndex = _homeSpendingPeriods.length - 1;
    _homeSpendingPageController = PageController(
      initialPage: _homeSpendingPageIndex,
      viewportFraction: 1,
    );
    Future.microtask(() {
      ref.read(homeViewModelProvider.notifier).loadData();
      _loadHomeSpending();
    });
  }

  @override
  void dispose() {
    _homeSpendingPageController.dispose();
    super.dispose();
  }

  List<_HomeSpendingPeriod> _buildHomeSpendingPeriods() {
    final currentWeekOfMonth = ((_currentWeekStart.day - 1) ~/ 7) + 1;
    final periods = <_HomeSpendingPeriod>[];

    periods.add(
      _HomeSpendingPeriod(
        offset: currentWeekOfMonth,
        title: '한 달 전 소비',
        subtitle: _weekRangeLabel(_weekStartForOffset(currentWeekOfMonth)),
      ),
    );

    for (var offset = currentWeekOfMonth - 1; offset >= 0; offset--) {
      final title = switch (offset) {
        0 => '이번 주 소비',
        1 => '지난 주 소비',
        _ => '$offset주 전 소비',
      };
      final start = _weekStartForOffset(offset);
      periods.add(
        _HomeSpendingPeriod(
          offset: offset,
          title: title,
          subtitle: _weekRangeLabel(start),
        ),
      );
    }

    return periods;
  }

  Future<void> _loadHomeSpending() async {
    setState(() {
      _isHomeSpendingLoading = true;
    });

    final repository = ref.read(spendingRepositoryProvider);
    final categoryMap = <int, List<SpendingCategory>>{};

    for (final period in _homeSpendingPeriods) {
      try {
        final result = await repository.fetchWeeklyAnalysis(
          _weekStartForOffset(period.offset),
        );
        categoryMap[period.offset] = result.categories;
      } catch (_) {
        categoryMap[period.offset] = const [];
      }
    }

    if (!mounted) return;
    setState(() {
      _homeSpendingCategoryMap = categoryMap;
      _isHomeSpendingLoading = false;
    });
  }

  String _weekRangeLabel(DateTime start) {
    final end = start.add(const Duration(days: 6));
    return '${start.month}/${start.day} - ${end.month}/${end.day}';
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(homeViewModelProvider);

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      body: Column(
        children: [
          // Custom header with week bar
          Builder(builder: (context) {
            final now = DateTime.now();
            final monday = now.subtract(Duration(days: now.weekday - 1));
            final sunday = monday.add(const Duration(days: 6));
            const weekdays = ['월', '화', '수', '목', '금', '토', '일'];

            return Container(
              width: double.infinity,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    AppColors.orange500,
                    AppColors.orange300,
                  ],
                ),
                borderRadius: BorderRadius.vertical(
                  bottom: Radius.circular(24.r),
                ),
              ),
              child: SafeArea(
                bottom: false,
                child: Padding(
                  padding: EdgeInsets.fromLTRB(20.w, 8.h, 20.w, 16.h),
                  child: Column(
                    children: [
                      // Title row
                      Row(
                        children: [
                          Text(
                            '이번 주 현황',
                            style: AppTextStyles.headingLg.copyWith(
                              color: const Color(0xFFFFFFFF),
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const Spacer(),
                          Text(
                            '${monday.month}/${monday.day} - ${sunday.month}/${sunday.day}',
                            style: AppTextStyles.caption.copyWith(
                              color: const Color(0xFFFFFFFF).withValues(alpha: 0.7),
                              fontSize: 11.sp,
                            ),
                          ),
                        ],
                      ),
                      SizedBox(height: 14.h),
                      // Week day bar
                      Row(
                        children: List.generate(7, (i) {
                          final isToday = i == now.weekday - 1;
                          final isPast = i < now.weekday - 1;
                          return Expanded(
                            child: Padding(
                              padding: EdgeInsets.symmetric(horizontal: 2.w),
                              child: Column(
                                children: [
                                  Text(
                                    weekdays[i],
                                    style: TextStyle(
                                      fontSize: 10.sp,
                                      fontWeight: isToday ? FontWeight.w700 : FontWeight.w400,
                                      color: isToday
                                          ? const Color(0xFFFFFFFF)
                                          : isPast
                                              ? const Color(0xFFFFFFFF).withValues(alpha: 0.7)
                                              : const Color(0xFFFFFFFF).withValues(alpha: 0.35),
                                    ),
                                  ),
                                  SizedBox(height: 4.h),
                                  Container(
                                    height: 4.h,
                                    decoration: BoxDecoration(
                                      borderRadius: BorderRadius.circular(2.r),
                                      color: isToday
                                          ? const Color(0xFFFFFFFF)
                                          : isPast
                                              ? const Color(0xFFFFFFFF).withValues(alpha: 0.4)
                                              : const Color(0xFFFFFFFF).withValues(alpha: 0.15),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          );
                        }),
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),
          // Body
          Expanded(child: _buildBody(context, ref, state)),
        ],
      ),
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
                  _loadHomeSpending();
                },
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      );
    }

    return _buildDashboard(
      context,
      state,
      showGoalEmptyState: state.activeGoals.isEmpty,
      showAccountEmptyState: state.accounts.isEmpty,
    );
  }

  Widget _buildDashboard(
    BuildContext context,
    HomeState state, {
    required bool showGoalEmptyState,
    required bool showAccountEmptyState,
  }) {
    final orderedAccounts = _orderedAccounts(state.accounts);
    final topGoals = _sortedGoals(state.activeGoals).take(3).toList();
    final screenWidth = MediaQuery.sizeOf(context).width;
    final homeSpendingCarouselHeight = screenWidth <= 390 ? 452.h : 424.h;

    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(20.w, 14.h, 20.w, 24.h),
      child: Column(
        children: [
          // 1) 목표
          GoalsSectionCard(
            goals: topGoals,
            onViewAll: () => context.go(AppRoutes.goalTab),
            onAddGoal: showGoalEmptyState
                ? () => context.push(AppRoutes.goalCreate)
                : null,
          ),
          SizedBox(height: 10.h),

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
                SizedBox(width: 10.w),
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
          SizedBox(height: 10.h),

          // 이번 주 소비 rose 차트
          Builder(builder: (context) {
            final colors = AppColors.of(context);
            if (_homeSpendingPeriods.isEmpty) {
              return const SizedBox.shrink();
            }

            return Padding(
              padding: EdgeInsets.only(bottom: 8.h),
              child: Column(
                children: [
                  SizedBox(
                    height: homeSpendingCarouselHeight,
                    child: PageView.builder(
                      controller: _homeSpendingPageController,
                      itemCount: _homeSpendingPeriods.length,
                      onPageChanged: (index) {
                        setState(() {
                          _homeSpendingPageIndex = index;
                        });
                      },
                      itemBuilder: (context, index) {
                        final period = _homeSpendingPeriods[index];
                        final categories =
                            _homeSpendingCategoryMap[period.offset] ?? const [];

                        if (!_isHomeSpendingLoading && categories.isNotEmpty) {
                          return Padding(
                            padding: EdgeInsets.zero,
                            child: SpendingRoseChart(
                              categories: categories,
                              title: period.title,
                              subtitle: period.subtitle,
                            ),
                          );
                        }

                        return Padding(
                          padding: EdgeInsets.zero,
                          child: Container(
                            width: double.infinity,
                            padding: EdgeInsets.all(16.w),
                            decoration: BoxDecoration(
                              color: colors.bgSurface,
                              borderRadius: BorderRadius.circular(20.r),
                              boxShadow: [
                                BoxShadow(
                                  color: colors.cardShadow,
                                  blurRadius: 8,
                                  offset: const Offset(0, 2),
                                ),
                              ],
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  period.title,
                                  style: AppTextStyles.bodyMd.copyWith(
                                    fontWeight: FontWeight.w700,
                                    color: colors.textPrimary,
                                  ),
                                ),
                                SizedBox(height: 4.h),
                                Text(
                                  period.subtitle,
                                  style: AppTextStyles.caption.copyWith(
                                    color: colors.textTertiary,
                                    fontSize: 11.sp,
                                  ),
                                ),
                                SizedBox(height: 24.h),
                                Expanded(
                                  child: Column(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      Center(
                                        child: _isHomeSpendingLoading
                                            ? SizedBox(
                                                width: 32.w,
                                                height: 32.w,
                                                child: const CircularProgressIndicator(
                                                  strokeWidth: 2.4,
                                                ),
                                              )
                                            : Icon(
                                                Icons.pie_chart_outline,
                                                size: 48.w,
                                                color: colors.gray100,
                                              ),
                                      ),
                                      SizedBox(height: 12.h),
                                      Text(
                                        showAccountEmptyState
                                            ? '계좌를 연결하면 소비 분석을 보여드릴게요'
                                            : '${period.title} 데이터가 아직 없어요',
                                        style: AppTextStyles.bodySm.copyWith(
                                          color: colors.textTertiary,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                      SizedBox(height: 16.h),
                                      if (showAccountEmptyState)
                                        GestureDetector(
                                          onTap: () => context.push(AppRoutes.accountAdd),
                                          child: Container(
                                            padding: EdgeInsets.symmetric(
                                              horizontal: 14.w,
                                              vertical: 8.h,
                                            ),
                                            decoration: BoxDecoration(
                                              color: AppColors.orange50,
                                              borderRadius: BorderRadius.circular(999.r),
                                            ),
                                            child: Text(
                                              '계좌 연결하기',
                                              style: AppTextStyles.caption.copyWith(
                                                color: AppColors.orange500,
                                                fontWeight: FontWeight.w700,
                                                fontSize: 11.sp,
                                              ),
                                            ),
                                          ),
                                        ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                  if (_homeSpendingPeriods.length > 1) ...[
                    SizedBox(height: 8.h),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(_homeSpendingPeriods.length, (index) {
                        final isActive = index == _homeSpendingPageIndex;
                        return AnimatedContainer(
                          duration: const Duration(milliseconds: 180),
                          width: isActive ? 16.w : 6.w,
                          height: 6.w,
                          margin: EdgeInsets.symmetric(horizontal: 3.w),
                          decoration: BoxDecoration(
                            color: isActive
                                ? AppColors.orange500
                                : colors.borderDefault,
                            borderRadius: BorderRadius.circular(999.r),
                          ),
                        );
                      }),
                    ),
                  ],
                ],
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

class _HomeSpendingPeriod {
  const _HomeSpendingPeriod({
    required this.offset,
    required this.title,
    required this.subtitle,
  });

  final int offset;
  final String title;
  final String subtitle;
}
