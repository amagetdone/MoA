import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/pages/spending/widgets/category_list.dart';
import 'package:more_app/domain/entities/spending.dart';

class SpendingPatternPage extends StatelessWidget {
  const SpendingPatternPage({super.key});

  @override
  Widget build(BuildContext context) {
    // 확장 기능을 테스트하기 위해 카테고리 데이터를 늘립니다.
    final List<SpendingCategory> mockCategories = [
      SpendingCategory(
        name: '식비',
        amount: 750000,
        budget: 1000000,
        colorValue: AppColors.orange500.toARGB32(),
        iconKey: 'restaurant',
      ),
      SpendingCategory(
        name: '여가비',
        amount: 200000,
        budget: 500000,
        colorValue: AppColors.orange500.toARGB32(),
        iconKey: 'subscriptions',
      ),
      SpendingCategory(
        name: '교통비',
        amount: 150000,
        budget: 300000,
        colorValue: AppColors.orange500.toARGB32(),
        iconKey: 'directions_bus',
      ),
      SpendingCategory(
        name: '쇼핑',
        amount: 300000,
        budget: 400000,
        colorValue: AppColors.orange500.toARGB32(),
        iconKey: 'shopping_bag',
      ),
      SpendingCategory(
        name: '주거비',
        amount: 500000,
        budget: 500000,
        colorValue: AppColors.orange500.toARGB32(),
        iconKey: 'more_horiz',
      ),
    ];

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '',
        showBack: true,
        actions: [
          IconButton(
            icon: Icon(Icons.notifications_none_outlined, size: 24.w),
            onPressed: () {},
          ),
        ],
      ),
      body: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 12.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('소비 패턴 분석', style: AppTextStyles.headingLg),
            SizedBox(height: 8.h),
            Text(
              '지난 3개월 간 소비내역을 기반으로 분석된 내용입니다.',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.textTertiary,
                fontSize: 14.sp,
              ),
            ),
            SizedBox(height: 24.h),

            // 1. 요약 카드
            MoreCard(
              padding: EdgeInsets.all(24.w),
              child: Column(
                children: [
                  _SummaryRow(label: '총 수입', value: 3000000, color: AppColors.textPrimary),
                  SizedBox(height: 16.h),
                  _SummaryRow(label: '총 지출', value: 1900000, color: AppColors.orange500),
                  const Divider(height: 32, color: Color(0xFFEEEEEE)),
                  _SummaryRow(label: '남은 금액', value: 1100000, color: const Color(0xFF4CAF50)),
                ],
              ),
            ),
            SizedBox(height: 20.h),

            // 2. 패턴 분석 상세 카드 (도넛 차트 포함)
            MoreCard(
              padding: EdgeInsets.all(24.w),
              child: Column(
                children: [
                  // 도넛 차트
                  SizedBox(
                    height: 220.h,
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        PieChart(
                          PieChartData(
                            sectionsSpace: 4,
                            centerSpaceRadius: 65.r,
                            sections: [
                              PieChartSectionData(value: 20, color: const Color(0xFFE57373), radius: 28.r, showTitle: false),
                              PieChartSectionData(value: 40, color: const Color(0xFFFFB74D), radius: 28.r, showTitle: false),
                              PieChartSectionData(value: 20, color: const Color(0xFFFFD54F), radius: 28.r, showTitle: false),
                              PieChartSectionData(value: 20, color: const Color(0xFF81C784), radius: 28.r, showTitle: false),
                            ],
                          ),
                        ),
                        Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              '1,900,000원',
                              style: AppTextStyles.bodyMd.copyWith(
                                fontWeight: FontWeight.bold,
                                fontSize: 16.sp,
                              ),
                            ),
                            Text(
                              '총지출',
                              style: AppTextStyles.caption.copyWith(
                                color: AppColors.textTertiary,
                                fontSize: 12.sp,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 24.h),
                  // 범례
                  Column(
                    children: [
                      Row(
                        children: [
                          const Expanded(child: _LegendItem(label: '고정비 20%', color: Color(0xFFE57373))),
                          const Expanded(child: _LegendItem(label: '변동비 40%', color: Color(0xFFFFB74D))),
                        ],
                      ),
                      SizedBox(height: 12.h),
                      Row(
                        children: [
                          const Expanded(child: _LegendItem(label: '저축액 20%', color: Color(0xFF81C784))),
                          const Expanded(child: _LegendItem(label: '자유소비 20%', color: Color(0xFFFFD54F))),
                        ],
                      ),
                    ],
                  ),
                  SizedBox(height: 24.h),
                  Text(
                    '큰 카테고리가 궁금하신가요?',
                    style: AppTextStyles.caption.copyWith(
                      color: AppColors.textTertiary,
                      fontSize: 12.sp,
                    ),
                  ),
                  SizedBox(height: 16.h),
                  
                  // 인사이트 박스
                  Container(
                    padding: EdgeInsets.all(16.w),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF6ED),
                      borderRadius: BorderRadius.circular(12.r),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.lightbulb_outline, color: AppColors.orange500, size: 20),
                        SizedBox(width: 8.w),
                        Text(
                          '식비 절약이 필요해보여요.',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.orange500,
                            fontWeight: FontWeight.bold,
                            fontSize: 14.sp,
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 32.h),

                  // 하단 카테고리 리스트
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          '카테고리별 지출',
                          style: AppTextStyles.bodyMd.copyWith(
                            fontWeight: FontWeight.bold,
                            fontSize: 16.sp,
                          ),
                        ),
                        SizedBox(width: 8.w),
                        Text(
                          '100% 기준',
                          style: AppTextStyles.caption.copyWith(
                            color: AppColors.textTertiary,
                            fontSize: 12.sp,
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 24.h),
                  // CategoryList 위젯이 자체적으로 화살표와 확장 기능을 제공합니다.
                  CategoryList(categories: mockCategories),
                ],
              ),
            ),
            SizedBox(height: 60.h),
          ],
        ),
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  final String label;
  final int value;
  final Color color;
  const _SummaryRow({required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodyMd.copyWith(
            color: AppColors.textSecondary,
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

class _LegendItem extends StatelessWidget {
  final String label;
  final Color color;
  const _LegendItem({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 8.w,
          height: 8.w,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        SizedBox(width: 8.w),
        Text(
          label,
          style: AppTextStyles.caption.copyWith(
            color: AppColors.textSecondary,
            fontSize: 13.sp,
          ),
        ),
      ],
    );
  }
}
