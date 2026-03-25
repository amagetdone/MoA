import 'package:flutter/material.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/pages/spending/widgets/category_list.dart';
import 'package:more_app/presentation/pages/spending/widgets/spending_line_chart.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_chip.dart';

class SpendingAnalysisPage extends ConsumerStatefulWidget {
  const SpendingAnalysisPage({super.key});

  @override
  ConsumerState<SpendingAnalysisPage> createState() => _SpendingAnalysisPageState();
}

class _SpendingAnalysisPageState extends ConsumerState<SpendingAnalysisPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(spendingViewModelProvider.notifier).loadData();
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(spendingViewModelProvider);
    final viewModel = ref.read(spendingViewModelProvider.notifier);

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
                  Row(
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
                  SizedBox(height: 16.h),

                  _SpendingSummaryCard(state: state),
                  SizedBox(height: 16.h),

                  if (state.isPeriodLoading)
                    SizedBox(
                      height: 300.h,
                      child: const Center(child: CircularProgressIndicator()),
                    )
                  else ...[
                    MoreCard(
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
                    SizedBox(height: 16.h),

                    MoreCard(
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
                  ],
                  SizedBox(height: 40.h),
                ],
              ),
            ),
          ),
          ),
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
                onPressed: () => viewModel.navigatePeriod(1),
                icon: const Icon(Icons.arrow_right, size: 32),
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
