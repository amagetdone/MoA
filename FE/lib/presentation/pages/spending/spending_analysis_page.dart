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

class SpendingAnalysisPage extends ConsumerWidget {
  const SpendingAnalysisPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(spendingViewModelProvider);
    final viewModel = ref.read(spendingViewModelProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '',
        showBack: false,
        showLogo: true,
        showNotification: true,
      ),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : Scrollbar(
              thumbVisibility: false,
              child: SingleChildScrollView(
              primary: true,
              physics: const BouncingScrollPhysics(),
              padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('지출 내역', style: AppTextStyles.headingLg.copyWith(fontSize: 24.sp, fontWeight: FontWeight.bold)),
                  SizedBox(height: 6.h),
                  Text(
                    '주, 월 단위로 소비 내역을 확인할 수 있어요.',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.textTertiary,
                      fontSize: 14.sp,
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
                      Flexible(
                        child: GestureDetector(
                          onTap: () => context.push(AppRoutes.spendingPattern),
                          child: Text(
                            '지난 3개월 소비분석 >',
                            style: AppTextStyles.caption.copyWith(
                              color: AppColors.orange500,
                              fontSize: 12.sp,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 16.h),

                  _SpendingSummaryCard(state: state),
                  SizedBox(height: 16.h),

                  MoreCard(
                    padding: EdgeInsets.all(20.w),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('지출 그래프', style: AppTextStyles.headingMd),
                        SizedBox(height: 20.h),
                        SizedBox(
                          height: 220.h,
                          child: SpendingLineChart(
                            data: state.isWeekly
                                ? state.dailySpendings
                                : state.monthlyComparisons,
                            isWeekly: state.isWeekly,
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 16.h),

                  _SpendingInsightBox(state: state),
                  SizedBox(height: 16.h),

                  MoreCard(
                    padding: EdgeInsets.all(20.w),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('카테고리별 지출', style: AppTextStyles.headingMd),
                        SizedBox(height: 20.h),
                        CategoryList(
                          categories: state.categories,
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 40.h),
                ],
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
                        style: AppTextStyles.headingMd,
                      ),
                    ),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      child: Text(
                        periodRange,
                        style: AppTextStyles.caption
                            .copyWith(color: AppColors.textTertiary),
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
                  style: AppTextStyles.bodyMd.copyWith(color: AppColors.textSecondary),
                ),
                Text(
                  CurrencyFormatter.formatKRW(
                    state.isWeekly ? state.weeklyExpenseAmount : state.monthlyExpenseAmount,
                  ),
                  style: AppTextStyles.headingMd.copyWith(fontSize: 22.sp),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SpendingInsightBox extends StatelessWidget {
  final SpendingState state;
  const _SpendingInsightBox({required this.state});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(16.w),
      decoration: BoxDecoration(
        color: AppColors.bgInsight,
        borderRadius: BorderRadius.circular(12.r),
      ),
      child: Row(
        children: [
          const Icon(Icons.lightbulb_outline, color: AppColors.orange500, size: 20),
          SizedBox(width: 8.w),
          Expanded(
            child: Text(
              state.isWeekly ? '이번 주 초과해서 사용하셨네요!' : '이번 달 예산을 아껴쓰고 있어요!',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.orange500,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          SizedBox(width: 8.w),
          GestureDetector(
            onTap: () => context.push(AppRoutes.spendingBudget),
            child: Text(
              '예산 설정하기 >',
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
                fontSize: 11.sp,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
