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
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_chip.dart';
import 'package:showcaseview/showcaseview.dart';

class SpendingAnalysisPage extends ConsumerStatefulWidget {
  const SpendingAnalysisPage({super.key});

  @override
  ConsumerState<SpendingAnalysisPage> createState() => _SpendingAnalysisPageState();
}

class _SpendingAnalysisPageState extends ConsumerState<SpendingAnalysisPage> {
  BuildContext? _showcaseContext;
  final _keyPeriodChips = GlobalKey();
  final _keySummary = GlobalKey();
  final _keyChart = GlobalKey();
  final _keyCategory = GlobalKey();

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(spendingViewModelProvider.notifier).loadData();
    });
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final shown = await TutorialService.isShown(TutorialService.spendingAnalysis);
      if (!shown && mounted) {
        await TutorialService.markShown(TutorialService.spendingAnalysis);
        if (mounted && _showcaseContext != null) {
          ShowCaseWidget.of(_showcaseContext!).startShowCase([
            _keyPeriodChips,
            _keySummary,
            _keyChart,
            _keyCategory,
          ]);
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(spendingViewModelProvider);
    final viewModel = ref.read(spendingViewModelProvider.notifier);

    return ShowCaseWidget(
      builder: (ctx) {
        _showcaseContext = ctx;
        return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: MoreAppBar(
        title: '',
        showBack: false,
        showLogo: true,
        showNotification: false,
      ),
      body: state.isLoading
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
                  Text('지출 내역', style: AppTextStyles.headingMd.copyWith(fontSize: 20.sp, fontWeight: FontWeight.bold, color: colors.textPrimary)),
                  SizedBox(height: 4.h),
                  Text(
                    '주, 월 단위로 소비 내역을 확인할 수 있어요.',
                    style: AppTextStyles.caption.copyWith(
                      color: colors.textTertiary,
                      fontSize: 12.sp,
                    ),
                  ),
                  SizedBox(height: 16.h),
                  Showcase(
                    key: _keyPeriodChips,
                    title: '주간·월간 탭',
                    description: '주간 또는 월간 단위로 소비 내역을 볼 수 있어요. 원하는 기간을 선택해서 소비를 분석해보세요.',
                    child: Row(
                    children: [
                      MoreChip(
                        label: '주간',
                        selected: state.isWeekly,
                        onTap: () => viewModel.togglePeriod(true),
                      ),
                      SizedBox(width: 8.w),
                      MoreChip(
                        label: '월간',
                        selected: !state.isWeekly,
                        onTap: () => viewModel.togglePeriod(false),
                      ),
                      const Spacer(),
                      GestureDetector(
                        onTap: () => context.push(AppRoutes.spendingPattern),
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

                  Showcase(
                    key: _keySummary,
                    title: '기간 이동',
                    description: '좌우 화살표를 눌러 지난 주/달의 소비 내역을 확인할 수 있어요. 소비 추이를 파악하는 데 도움이 돼요.',
                    child: _SpendingSummaryCard(state: state),
                  ),
                  SizedBox(height: 16.h),

                  if (state.isPeriodLoading)
                    SizedBox(
                      height: 300.h,
                      child: const Center(child: CircularProgressIndicator()),
                    )
                  else ...[
                    Showcase(
                      key: _keyChart,
                      title: '지출 그래프',
                      description: '선택한 기간의 일별/주별 지출을 그래프로 보여줘요. 어느 날 지출이 많았는지 한눈에 파악해보세요.',
                      child: MoreCard(
                      padding: EdgeInsets.all(20.w),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('지출 그래프', style: AppTextStyles.headingMd.copyWith(color: colors.textPrimary)),
                              GestureDetector(
                                onTap: () => context.push(AppRoutes.spendingBudget),
                                child: Text(
                                  '주간 예산 설정 >',
                                  style: AppTextStyles.caption.copyWith(
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
                              data: state.isWeekly
                                  ? state.dailySpendings
                                  : state.monthlySpendings,
                              isWeekly: state.isWeekly,
                            ),
                          ),
                        ],
                      ),
                    ),
                    ),
                    SizedBox(height: 16.h),

                    Showcase(
                      key: _keyCategory,
                      title: '카테고리별 지출',
                      description: '식비, 교통비 등 카테고리별로 얼마나 썼는지 확인할 수 있어요. 주요 지출 항목을 파악해 절약 계획을 세워보세요.',
                      child: MoreCard(
                      padding: EdgeInsets.all(20.w),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('카테고리별 지출', style: AppTextStyles.headingMd.copyWith(color: colors.textPrimary)),
                          SizedBox(height: 4.h),
                          Text(
                            state.isWeekly
                                ? '이번 주 전체 지출 대비 카테고리별 금액이에요.'
                                : '이번 달 전체 지출 대비 카테고리별 금액이에요.',
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                              fontSize: 12.sp,
                            ),
                          ),
                          SizedBox(height: 16.h),
                          CategoryList(
                            categories: state.categories,
                            totalAmount: state.isWeekly
                                ? state.weeklyExpenseAmount
                                : state.monthlyExpenseAmount,
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

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final viewModel = ref.read(spendingViewModelProvider.notifier);
    final periodLabel = state.isWeekly
        ? _weekLabel(state.selectedWeekStart)
        : _monthLabel(state.selectedMonthStart);
    final periodRange = state.isWeekly
        ? _weekDateRange(state.selectedWeekStart)
        : _monthDateRange(state.selectedMonthStart);
    final canMoveNext = state.isWeekly
        ? state.selectedWeekStart.isBefore(_currentWeekStart())
        : state.selectedMonthStart.isBefore(_currentMonthStart());

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
                        style: AppTextStyles.headingMd.copyWith(color: colors.textPrimary),
                      ),
                    ),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      child: Text(
                        periodRange,
                        style: AppTextStyles.caption
                            .copyWith(color: colors.textTertiary),
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: canMoveNext ? () => viewModel.navigatePeriod(1) : null,
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
                  style: AppTextStyles.bodyMd.copyWith(color: colors.textSecondary),
                ),
                Text(
                  CurrencyFormatter.formatKRW(
                    state.isWeekly ? state.weeklyExpenseAmount : state.monthlyExpenseAmount,
                  ),
                  style: AppTextStyles.headingMd.copyWith(fontSize: 22.sp, color: colors.textPrimary),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
