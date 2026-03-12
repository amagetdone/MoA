import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/pages/spending/widgets/category_list.dart';
import 'package:more_app/presentation/pages/spending/widgets/spending_chart.dart';
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
        title: '지출 내역',
        showBack: false,
        actions: [
          IconButton(
            icon: Icon(Icons.tune, size: 24.w),
            onPressed: () => context.push(AppRoutes.spendingBudget),
          ),
        ],
      ),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : state.errorMessage != null
          ? Center(
              child: Text(
                '소비 데이터를 불러오지 못했습니다.\n${state.errorMessage!}',
                textAlign: TextAlign.center,
              ),
            )
          : SingleChildScrollView(
              padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '주, 월 단위로 소비 내역을 확인할 수 있어요.',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.textTertiary,
                      fontSize: 13.sp,
                    ),
                  ),
                  SizedBox(height: 12.h),
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
                        onTap: () {
                          // TODO: navigate to 3-month analysis
                        },
                        child: Text(
                          '지난 3개월 소비분석 보기 >',
                          style: AppTextStyles.caption.copyWith(
                            color: AppColors.orange500,
                            fontSize: 12.sp,
                          ),
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 16.h),
                  MoreCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '총 지출',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textTertiary,
                            fontSize: 13.sp,
                          ),
                        ),
                        SizedBox(height: 4.h),
                        Text(
                          CurrencyFormatter.formatKRW(state.totalSpent),
                          style: AppTextStyles.headingXl.copyWith(
                            color: AppColors.orange500,
                            fontWeight: FontWeight.bold,
                            fontSize: 24.sp,
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 16.h),
                  SpendingLineChart(
                    data: state.isWeekly
                        ? state.dailySpendings
                        : state.monthlySpendings,
                    isWeekly: state.isWeekly,
                  ),
                  SizedBox(height: 16.h),
                  Padding(
                    padding: EdgeInsets.only(left: 4.w, bottom: 12.h),
                    child: Text(
                      '카테고리별 지출',
                      style: AppTextStyles.headingMd.copyWith(
                        color: AppColors.textPrimary,
                        fontSize: 16.sp,
                      ),
                    ),
                  ),
                  CategoryList(categories: state.categories),
                  SizedBox(height: 16.h),
                  SpendingDonutChart(categories: state.categories),
                  SizedBox(height: 24.h),
                ],
              ),
            ),
    );
  }
}
