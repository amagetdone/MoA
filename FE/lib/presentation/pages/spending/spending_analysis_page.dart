import 'package:flutter/material.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/core/utils/tutorial_service.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/pages/spending/widgets/category_list.dart';
import 'package:more_app/presentation/pages/spending/widgets/spending_line_chart.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/providers/showcase_preview_provider.dart';
import 'package:more_app/presentation/showcase/showcase_mock_data.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_chip.dart';
import 'package:more_app/presentation/widgets/common/more_showcase.dart';
import 'package:showcaseview/showcaseview.dart';

class SpendingAnalysisPage extends ConsumerStatefulWidget {
  const SpendingAnalysisPage({super.key});

  @override
  ConsumerState<SpendingAnalysisPage> createState() =>
      _SpendingAnalysisPageState();
}

class _SpendingAnalysisPageState extends ConsumerState<SpendingAnalysisPage> {
  BuildContext? _showcaseContext;
  final _keyPeriodChips = GlobalKey();
  final _keySummary = GlobalKey();
  final _keyChart = GlobalKey();
  final _keyCategory = GlobalKey();
  bool _showcaseResolved = false;
  bool _showcaseCheckInFlight = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(spendingViewModelProvider.notifier).loadData();
    });
  }

  bool _isShowcaseVisibleRoute() {
    final route = ModalRoute.of(context);
    final currentPath = GoRouter.of(context).state.uri.path;
    return route?.isCurrent == true && currentPath == AppRoutes.spendingTab;
  }

  void _maybeStartShowcase() {
    if (_showcaseResolved ||
        _showcaseCheckInFlight ||
        _showcaseContext == null) {
      return;
    }
    if (!_isShowcaseVisibleRoute()) {
      return;
    }

    _showcaseCheckInFlight = true;
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final shown = await TutorialService.isShown(
        TutorialService.spendingAnalysis,
      );
      if (!mounted) {
        return;
      }
      if (shown) {
        _showcaseResolved = true;
        _showcaseCheckInFlight = false;
        return;
      }
      if (!_isShowcaseVisibleRoute() || _showcaseContext == null) {
        _showcaseCheckInFlight = false;
        return;
      }

      ref.read(showcasePreviewModeProvider.notifier).state = true;
      ShowCaseWidget.of(
        _showcaseContext!,
      ).startShowCase([_keyPeriodChips, _keySummary, _keyChart, _keyCategory]);
      await TutorialService.markShown(TutorialService.spendingAnalysis);
      _showcaseResolved = true;
      _showcaseCheckInFlight = false;
    });
  }

  bool _shouldUseShowcaseMock(bool showcasePreviewMode, SpendingState state) {
    if (!showcasePreviewMode) {
      return false;
    }

    final hasSeries = state.isWeekly
        ? state.dailySpendings.isNotEmpty
        : state.monthlySpendings.isNotEmpty;

    return state.isLoading ||
        state.isPeriodLoading ||
        state.categories.isEmpty ||
        !hasSeries;
  }

  void _startShowcase() {
    if (_showcaseContext == null) {
      return;
    }
    ref.read(showcasePreviewModeProvider.notifier).state = true;
    ShowCaseWidget.of(
      _showcaseContext!,
    ).startShowCase([_keyPeriodChips, _keySummary, _keyChart, _keyCategory]);
  }

  void _stopShowcasePreview() {
    ref.read(showcasePreviewModeProvider.notifier).state = false;
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(spendingViewModelProvider);
    final showcasePreviewMode = ref.watch(showcasePreviewModeProvider);
    final useShowcaseMock = _shouldUseShowcaseMock(showcasePreviewMode, state);
    final effectiveState = useShowcaseMock
        ? ShowcaseMockData.spendingState(base: state)
        : state;
    final viewModel = ref.read(spendingViewModelProvider.notifier);

    return ShowCaseWidget(
      enableAutoScroll: true,
      scrollDuration: const Duration(milliseconds: 450),
      onStart: (index, key) {
        ref.read(showcasePreviewModeProvider.notifier).state = true;
      },
      onFinish: () {
        ref.read(showcasePreviewModeProvider.notifier).state = false;
      },
      builder: (ctx) {
        _showcaseContext = ctx;
        _maybeStartShowcase();
        return Scaffold(
          backgroundColor: colors.bgPrimary,
          appBar: MoreAppBar(
            title: '',
            showBack: false,
            showLogo: true,
            showNotification: false,
          ),
          body: state.isLoading && !useShowcaseMock
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: () => viewModel.loadData(),
                  child: Scrollbar(
                    thumbVisibility: false,
                    child: SingleChildScrollView(
                      primary: true,
                      physics: const BouncingScrollPhysics(),
                      padding: EdgeInsets.fromLTRB(20.w, 0, 20.w, 20.w),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Text(
                                '지출 내역',
                                style: AppTextStyles.headingMd.copyWith(
                                  fontSize: 20.sp,
                                  fontWeight: FontWeight.bold,
                                  color: colors.textPrimary,
                                ),
                              ),
                              SizedBox(width: 8.w),
                              ShowcaseHelpButton(onTap: _startShowcase),
                              const Spacer(),
                            ],
                          ),
                          SizedBox(height: 4.h),
                          Text(
                            '주, 월 단위로 소비 내역을 확인할 수 있어요.',
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                              fontSize: 12.sp,
                            ),
                          ),
                          SizedBox(height: 16.h),
                          MoreShowcase(
                            showcaseKey: _keyPeriodChips,
                            title: '주간·월간 지출 내역',
                            description:
                                '주간 또는 월간 단위로 지출 내역을 볼 수 있어요.\n원하는 기간을 선택해서 소비를 분석해보세요.',
                            onSkip: _stopShowcasePreview,
                            child: Row(
                              children: [
                                MoreChip(
                                  label: '주간',
                                  selected: effectiveState.isWeekly,
                                  onTap: () => viewModel.togglePeriod(true),
                                ),
                                SizedBox(width: 8.w),
                                MoreChip(
                                  label: '월간',
                                  selected: !effectiveState.isWeekly,
                                  onTap: () => viewModel.togglePeriod(false),
                                ),
                                const Spacer(),
                                GestureDetector(
                                  onTap: () =>
                                      context.push(AppRoutes.spendingPattern),
                                  child: Text(
                                    '전월 소비분석 >',
                                    style: AppTextStyles.caption.copyWith(
                                      color: AppColors.orange500,
                                      fontSize: 12.sp,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          SizedBox(height: 16.h),

                          MoreShowcase(
                            showcaseKey: _keySummary,
                            title: '기간 이동',
                            description:
                                '좌우 화살표를 눌러 지난 주/달의 지출 내역을\n확인할 수 있어요.\n소비 추이를 파악하는 데 도움이 돼요.',
                            tooltipPosition: TooltipPosition.top,
                            onSkip: _stopShowcasePreview,
                            child: _SpendingSummaryCard(state: effectiveState),
                          ),
                          SizedBox(height: 16.h),

                          if (effectiveState.isPeriodLoading && !useShowcaseMock)
                            SizedBox(
                              height: 300.h,
                              child: const Center(
                                child: CircularProgressIndicator(),
                              ),
                            )
                          else ...[
                            MoreShowcase(
                              showcaseKey: _keyChart,
                              title: '지출 그래프',
                              description:
                                  '선택한 기간의 일별/주별 지출을 그래프로\n보여줘요.\n어느 날 지출이 많았는지 한눈에 파악해보세요.',
                              tooltipPosition: TooltipPosition.top,
                              onSkip: _stopShowcasePreview,
                              child: MoreCard(
                                padding: EdgeInsets.all(20.w),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      mainAxisAlignment:
                                          MainAxisAlignment.spaceBetween,
                                      children: [
                                        Text(
                                          '지출 그래프',
                                          style: AppTextStyles.headingMd
                                              .copyWith(
                                                color: colors.textPrimary,
                                              ),
                                        ),
                                        GestureDetector(
                                          onTap: () => context.push(
                                            AppRoutes.spendingBudget,
                                          ),
                                          child: Text(
                                            '주간 예산 설정 >',
                                            style: AppTextStyles.caption
                                                .copyWith(
                                                  color: AppColors.orange500,
                                                  fontSize: 12.sp,
                                                ),
                                          ),
                                        ),
                                      ],
                                    ),
                                    SizedBox(height: 20.h),
                                    SizedBox(
                                      height: 220.h,
                                      child: SpendingLineChart(
                                        data: effectiveState.isWeekly
                                            ? effectiveState.dailySpendings
                                            : effectiveState.monthlySpendings,
                                        isWeekly: effectiveState.isWeekly,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            SizedBox(height: 16.h),

                            MoreShowcase(
                              showcaseKey: _keyCategory,
                              title: '카테고리별 지출',
                              description:
                                  '식비, 교통비 등 카테고리별로 얼마나 썼는지\n확인할 수 있어요.\n주요 지출 항목을 파악해 절약 계획을 세워보세요.',
                              tooltipPosition: TooltipPosition.top,
                              onSkip: _stopShowcasePreview,
                              child: MoreCard(
                                padding: EdgeInsets.all(20.w),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      '카테고리별 지출',
                                      style: AppTextStyles.headingMd.copyWith(
                                        color: colors.textPrimary,
                                      ),
                                    ),
                                    SizedBox(height: 4.h),
                                    Text(
                                      effectiveState.isWeekly
                                          ? '이번 주 전체 지출 대비 카테고리별 금액이에요.'
                                          : '이번 달 전체 지출 대비 카테고리별 금액이에요.',
                                      style: AppTextStyles.caption.copyWith(
                                        color: colors.textTertiary,
                                        fontSize: 12.sp,
                                      ),
                                    ),
                                    SizedBox(height: 16.h),
                                    CategoryList(
                                      categories: effectiveState.categories,
                                      totalAmount: effectiveState.isWeekly
                                          ? effectiveState.weeklyExpenseAmount
                                          : effectiveState.monthlyExpenseAmount,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ],
                          SizedBox(height: 40.h),
                        ],
                      ),
                    ),
                  ),
                ),
        );
      },
    );
  }
}

String _weekLabel(DateTime weekStart) {
  final weekOfMonth = ((weekStart.day - 1) ~/ 7) + 1;
  return '${weekStart.month}월 $weekOfMonth주차';
}

String _weekDateRange(DateTime weekStart) {
  final weekEnd = weekStart.add(const Duration(days: 6));
  String fmt(DateTime d) =>
      '${d.year}.${d.month.toString().padLeft(2, '0')}.${d.day.toString().padLeft(2, '0')}';
  return '${fmt(weekStart)} ~ ${fmt(weekEnd)}';
}

String _monthLabel(DateTime monthStart) {
  return '${monthStart.year}년 ${monthStart.month}월';
}

String _monthDateRange(DateTime monthStart) {
  final lastDay = DateTime(monthStart.year, monthStart.month + 1, 0);
  String fmt(DateTime d) =>
      '${d.year}.${d.month.toString().padLeft(2, '0')}.${d.day.toString().padLeft(2, '0')}';
  return '${fmt(monthStart)} ~ ${fmt(lastDay)}';
}

DateTime _currentWeekStart() {
  final now = DateTime.now();
  final monday = now.subtract(Duration(days: now.weekday - 1));
  return DateTime(monday.year, monday.month, monday.day);
}

DateTime _currentMonthStart() {
  final now = DateTime.now();
  return DateTime(now.year, now.month, 1);
}

class _SpendingSummaryCard extends ConsumerWidget {
  final SpendingState state;
  const _SpendingSummaryCard({required this.state});

  String? _buildBudgetInsight(HomeState homeState) {
    if (!state.isWeekly ||
        !_isSameCalendarDay(state.selectedWeekStart, _currentWeekStart())) {
      return null;
    }

    final weeklySaved = homeState.weeklySaved;
    final weeklyRemaining = homeState.weeklyRemaining;

    if (weeklySaved <= 0 && weeklyRemaining <= 0) {
      return null;
    }

    return '이번 주 저축 ${CurrencyFormatter.formatKRW(weeklySaved)}을 챙겼네요!\n'
        '여유 자금은 ${CurrencyFormatter.formatKRW(weeklyRemaining)}이 남아있어요.';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final viewModel = ref.read(spendingViewModelProvider.notifier);
    final homeState = ref.watch(homeViewModelProvider);
    final periodLabel = state.isWeekly
        ? _weekLabel(state.selectedWeekStart)
        : _monthLabel(state.selectedMonthStart);
    final periodRange = state.isWeekly
        ? _weekDateRange(state.selectedWeekStart)
        : _monthDateRange(state.selectedMonthStart);
    final canMoveNext = state.isWeekly
        ? state.selectedWeekStart.isBefore(_currentWeekStart())
        : state.selectedMonthStart.isBefore(_currentMonthStart());
    final budgetInsight = _buildBudgetInsight(homeState);

    return MoreCard(
      padding: EdgeInsets.symmetric(vertical: 24.h, horizontal: 16.w),
      child: Column(
        children: [
          Row(
            children: [
              IconButton(
                onPressed: () => viewModel.navigatePeriod(-1),
                icon: const Icon(Icons.arrow_left, size: 32),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
              ),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      child: Text(
                        periodLabel,
                        style: AppTextStyles.headingMd.copyWith(
                          color: colors.textPrimary,
                        ),
                      ),
                    ),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      child: Text(
                        periodRange,
                        style: AppTextStyles.caption.copyWith(
                          color: colors.textTertiary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: canMoveNext
                    ? () => viewModel.navigatePeriod(1)
                    : null,
                icon: Icon(
                  Icons.arrow_right,
                  size: 32,
                  color: canMoveNext
                      ? colors.textPrimary
                      : colors.textTertiary.withValues(alpha: 0.35),
                ),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
              ),
            ],
          ),
          SizedBox(height: 16.h),
          FittedBox(
            fit: BoxFit.scaleDown,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  '총 지출  ',
                  style: AppTextStyles.bodyMd.copyWith(
                    color: colors.textSecondary,
                  ),
                ),
                Text(
                  CurrencyFormatter.formatKRW(
                    state.isWeekly
                        ? state.weeklyExpenseAmount
                        : state.monthlyExpenseAmount,
                  ),
                  style: AppTextStyles.headingMd.copyWith(
                    fontSize: 22.sp,
                    color: colors.textPrimary,
                  ),
                ),
              ],
            ),
          ),
          if (budgetInsight != null) ...[
            SizedBox(height: 14.h),
            Container(
              width: double.infinity,
              padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 11.h),
              decoration: BoxDecoration(
                color: const Color(0xFF53A65A).withValues(alpha: 0.10),
                borderRadius: BorderRadius.circular(12.r),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(
                    Icons.savings_outlined,
                    size: 16.w,
                    color: const Color(0xFF53A65A),
                  ),
                  SizedBox(width: 8.w),
                  Expanded(
                    child: Text(
                      budgetInsight,
                      style: AppTextStyles.caption.copyWith(
                        color: colors.textSecondary,
                        fontSize: 12.sp,
                        fontWeight: FontWeight.w600,
                        height: 1.45,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

bool _isSameCalendarDay(DateTime left, DateTime right) {
  return left.year == right.year &&
      left.month == right.month &&
      left.day == right.day;
}
