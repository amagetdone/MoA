import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/pages/home/widgets/spending_rose_chart.dart';
import 'package:more_app/presentation/pages/spending/widgets/category_list.dart';
import 'package:more_app/presentation/viewmodels/onboarding_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class OnboardingAnalysisPage extends ConsumerWidget {
  const OnboardingAnalysisPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final analysis = ref.watch(onboardingViewModelProvider).analysisResult;

    final averageIncome = analysis?.averageIncome ?? 0;
    final averageExpense = analysis?.averageExpense ?? 0;
    final freeAmount = analysis?.remainingAmount ?? 0;
    final categories = analysis?.categoryExpenses ?? const <CategoryExpense>[];
    final categoryItems = categories
        .map(
          (category) => SpendingCategory(
            name: category.categoryName,
            amount: category.amount,
            budget: averageExpense,
            colorValue: AppColors.orange500.toARGB32(),
            iconKey: 'more_horiz',
            ratio: averageExpense > 0
                ? (category.amount / averageExpense) * 100
                : 0,
          ),
        )
        .toList();

    return PopScope(
      canPop: false,
      child: Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(
          title: '',
          showBack: false,
          showNotification: false,
        ),
        body: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(20.w, 12.h, 20.w, 24.h),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '소비 패턴 분석',
                style: AppTextStyles.headingMd.copyWith(
                  fontSize: 20.sp,
                  fontWeight: FontWeight.bold,
                  color: colors.textPrimary,
                ),
              ),
              SizedBox(height: 4.h),
              Text(
                analysis?.summaryText ?? '지난 3개월 소비패턴을 분석한 내용이에요.',
                style: AppTextStyles.bodySm.copyWith(
                  color: colors.textTertiary,
                  fontSize: 12.sp,
                ),
              ),
              SizedBox(height: 24.h),
              MoreCard(
                padding: EdgeInsets.all(24.w),
                child: Column(
                  children: [
                    _SummaryRow(
                      label: '평균 수입',
                      value: averageIncome,
                      color: colors.textPrimary,
                    ),
                    SizedBox(height: 16.h),
                    _SummaryRow(
                      label: '평균 지출',
                      value: averageExpense,
                      color: AppColors.orange500,
                    ),
                  ],
                ),
              ),
              SizedBox(height: 20.h),
              MoreCard(
                padding: EdgeInsets.all(24.w),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 44.w,
                      height: 44.w,
                      decoration: BoxDecoration(
                        color: const Color(0xFFEFF8EF),
                        borderRadius: BorderRadius.circular(14.r),
                      ),
                      alignment: Alignment.center,
                      child: Icon(
                        Icons.account_balance_wallet_rounded,
                        color: const Color(0xFF53A65A),
                        size: 22.w,
                      ),
                    ),
                    SizedBox(width: 14.w),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '여유 자금',
                            style: AppTextStyles.bodyMd.copyWith(
                              fontWeight: FontWeight.bold,
                              fontSize: 16.sp,
                              color: colors.textPrimary,
                            ),
                          ),
                          SizedBox(height: 4.h),
                          Text(
                            '최근 3개월 평균 수입에서 평균 지출을 제외한 금액이에요.',
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                              fontSize: 12.sp,
                            ),
                          ),
                          SizedBox(height: 12.h),
                          Text(
                            CurrencyFormatter.formatKRW(freeAmount),
                            style: AppTextStyles.headingMd.copyWith(
                              color: const Color(0xFF53A65A),
                              fontWeight: FontWeight.w800,
                              fontSize: 20.sp,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(height: 20.h),
              if (categoryItems.isNotEmpty)
                SpendingRoseChart(
                  categories: categoryItems,
                  title: '카테고리별 지출 분석',
                  subtitle: '최근 3개월 평균 지출 기준',
                ),
              if (categoryItems.isEmpty)
                MoreCard(
                  padding: EdgeInsets.all(24.w),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '카테고리별 지출 분석',
                        style: AppTextStyles.bodyMd.copyWith(
                          fontWeight: FontWeight.bold,
                          fontSize: 16.sp,
                          color: colors.textPrimary,
                        ),
                      ),
                      SizedBox(height: 8.h),
                      Text(
                        '분석할 카테고리 지출 데이터가 아직 없어요.',
                        style: AppTextStyles.caption.copyWith(
                          color: colors.textTertiary,
                          fontSize: 12.sp,
                        ),
                      ),
                    ],
                  ),
                ),
              SizedBox(height: 20.h),
              MoreCard(
                padding: EdgeInsets.all(24.w),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '카테고리별 지출',
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.bold,
                        fontSize: 16.sp,
                        color: colors.textPrimary,
                      ),
                    ),
                    SizedBox(height: 4.h),
                    Text(
                      '최근 3개월 평균 지출 대비 카테고리별 금액이에요.',
                      style: AppTextStyles.caption.copyWith(
                        color: colors.textTertiary,
                        fontSize: 12.sp,
                      ),
                    ),
                    SizedBox(height: 24.h),
                    CategoryList(
                      categories: categoryItems,
                      totalAmount: averageExpense,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        bottomNavigationBar: MoreBottomButton(
          text: '다음',
          progress: 5 / 7,
          onPressed: () => context.go(AppRoutes.onboardingGoal),
        ),
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final int value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodyMd.copyWith(
            color: colors.textSecondary,
            fontSize: 15.sp,
          ),
        ),
        Text(
          CurrencyFormatter.formatKRW(value),
          style: AppTextStyles.bodyLg.copyWith(
            color: color,
            fontWeight: FontWeight.bold,
            fontSize: 17.sp,
          ),
        ),
      ],
    );
  }
}
