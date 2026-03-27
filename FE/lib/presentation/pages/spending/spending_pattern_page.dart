import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/pages/home/widgets/spending_rose_chart.dart';
import 'package:more_app/presentation/pages/spending/widgets/category_list.dart';
import 'package:more_app/presentation/viewmodels/spending_pattern_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class SpendingPatternPage extends ConsumerStatefulWidget {
  const SpendingPatternPage({super.key});

  @override
  ConsumerState<SpendingPatternPage> createState() =>
      _SpendingPatternPageState();
}

class _SpendingPatternPageState extends ConsumerState<SpendingPatternPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(spendingPatternViewModelProvider.notifier).loadData();
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final patternState = ref.watch(spendingPatternViewModelProvider);

    if (patternState.isLoading) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(
          title: '',
          showBack: true,
          showNotification: false,
        ),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    final result = patternState.result;

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(
        title: '',
        showBack: true,
        showNotification: false,
      ),
      body: Scrollbar(
        thumbVisibility: false,
        child: SingleChildScrollView(
          physics: const BouncingScrollPhysics(),
          padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 12.h),
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
                '지난 달 소비내역을 기반으로 분석된 내용이에요.',
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
                      label: '총 수입',
                      value: result.totalIncome,
                      color: colors.textPrimary,
                    ),
                    SizedBox(height: 16.h),
                    _SummaryRow(
                      label: '총 지출',
                      value: result.totalExpense,
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
                            '지난 달 총 수입에서 총 지출을 제외한 금액이에요.',
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                              fontSize: 12.sp,
                            ),
                          ),
                          SizedBox(height: 12.h),
                          Text(
                            CurrencyFormatter.formatKRW(result.freeAmount),
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
              if (result.categories.isNotEmpty)
                SpendingRoseChart(
                  categories: result.categories,
                  title: '카테고리별 지출 분석',
                  subtitle: '지난 달 총 지출 기준',
                ),
              if (result.categories.isEmpty)
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
                      '이전 달 전체 지출 대비 카테고리별 금액이에요.',
                      style: AppTextStyles.caption.copyWith(
                        color: colors.textTertiary,
                        fontSize: 12.sp,
                      ),
                    ),
                    SizedBox(height: 24.h),
                    CategoryList(
                      categories: result.categories,
                      totalAmount: result.totalExpense,
                    ),
                  ],
                ),
              ),
              SizedBox(height: 60.h),
            ],
          ),
        ),
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  final String label;
  final int value;
  final Color color;

  const _SummaryRow({
    required this.label,
    required this.value,
    required this.color,
  });

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
