import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/presentation/viewmodels/spending_pattern_viewmodel.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/pages/spending/widgets/category_list.dart';
import 'package:more_app/domain/entities/spending.dart';

const _sectionColors = [
  Color(0xFFE57373),
  Color(0xFFFFB74D),
  Color(0xFF81C784),
];
const _sectionLabels = ['고정지출', '변동지출', '여유 자금'];

class SpendingPatternPage extends ConsumerStatefulWidget {
  const SpendingPatternPage({super.key});

  @override
  ConsumerState<SpendingPatternPage> createState() => _SpendingPatternPageState();
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
    final patternState = ref.watch(spendingPatternViewModelProvider);
    final touchedIndex = ref.watch(spendingViewModelProvider).touchedPieIndex;
    final spendingViewModel = ref.read(spendingViewModelProvider.notifier);

    if (patternState.isLoading) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: const MoreAppBar(title: '', showBack: true, showNotification: true),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    final result = patternState.result;
    final sectionAmounts = [
      result.fixedAmount,
      result.variableAmount,
      result.freeAmount,
    ];
    final total = result.totalExpense;

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(
        title: '',
        showBack: true,
        showNotification: true,
      ),
      body: Scrollbar(
        thumbVisibility: false,
        child: SingleChildScrollView(
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
                    _SummaryRow(label: '총 수입', value: result.totalIncome, color: AppColors.textPrimary),
                    SizedBox(height: 16.h),
                    _SummaryRow(label: '총 지출', value: result.totalExpense, color: AppColors.orange500),
                    const Divider(height: 32, color: Color(0xFFEEEEEE)),
                    _SummaryRow(
                      label: '잔여 금액',
                      value: result.savingsAmount,
                      color: const Color(0xFF4CAF50),
                    ),
                  ],
                ),
              ),
              SizedBox(height: 20.h),

              // 2. 패턴 분석 상세 카드 (도넛 차트)
              MoreCard(
                padding: EdgeInsets.all(24.w),
                child: Column(
                  children: [
                    SizedBox(
                      height: 220.h,
                      child: Stack(
                        alignment: Alignment.center,
                        children: [
                          PieChart(
                            PieChartData(
                              sectionsSpace: 4,
                              centerSpaceRadius: 65.r,
                              pieTouchData: PieTouchData(
                                touchCallback: (event, response) {
                                  if (!event.isInterestedForInteractions ||
                                      response == null ||
                                      response.touchedSection == null) {
                                    spendingViewModel.setTouchedPieIndex(null);
                                    return;
                                  }
                                  final idx = response.touchedSection!.touchedSectionIndex;
                                  spendingViewModel.setTouchedPieIndex(idx >= 0 ? idx : null);
                                },
                              ),
                              sections: _buildSections(touchedIndex, sectionAmounts, total),
                            ),
                          ),
                          Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                touchedIndex != null
                                    ? CurrencyFormatter.formatKRW(sectionAmounts[touchedIndex])
                                    : CurrencyFormatter.formatKRW(result.totalExpense),
                                style: AppTextStyles.bodyMd.copyWith(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16.sp,
                                  color: touchedIndex != null
                                      ? _sectionColors[touchedIndex]
                                      : AppColors.textPrimary,
                                ),
                              ),
                              Text(
                                touchedIndex != null ? _sectionLabels[touchedIndex] : '총지출',
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
                            Expanded(child: _LegendItem(label: '고정지출', amount: result.fixedAmount, color: _sectionColors[0])),
                            Expanded(child: _LegendItem(label: '변동지출', amount: result.variableAmount, color: _sectionColors[1])),
                          ],
                        ),
                        SizedBox(height: 12.h),
                        Row(
                          children: [
                            Expanded(child: _LegendItem(label: '여유 자금', amount: result.freeAmount, color: _sectionColors[2])),
                          ],
                        ),
                      ],
                    ),
                    SizedBox(height: 24.h),

                    // 인사이트 박스
                    if (result.topCategoryName.isNotEmpty)
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
                              '${result.topCategoryName} 절약이 필요해보여요.',
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

                    // 카테고리별 지출
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
                            '3개월 전체 지출에서 차지하는 비율',
                            style: AppTextStyles.caption.copyWith(
                              color: AppColors.textTertiary,
                              fontSize: 12.sp,
                            ),
                          ),
                        ],
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

  List<PieChartSectionData> _buildSections(
    int? touchedIndex,
    List<int> amounts,
    int total,
  ) {
    if (total == 0) {
      return List.generate(
        _sectionColors.length,
        (i) => PieChartSectionData(
          value: 25,
          color: _sectionColors[i].withValues(alpha: 0.3),
          radius: 28.r,
          showTitle: false,
        ),
      );
    }
    return List.generate(_sectionColors.length, (i) {
      final isTouched = touchedIndex == i;
      final rawValue = amounts[i] / total * 100;
      // 0이 아닌 슬라이스는 최소 2% 보장해서 보이게 함
      final value = amounts[i] > 0 ? rawValue.clamp(2.0, 100.0) : 0.0;
      return PieChartSectionData(
        value: value,
        color: _sectionColors[i],
        radius: isTouched ? 36.r : 28.r,
        showTitle: false,
      );
    });
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
  final int amount;
  final Color color;
  const _LegendItem({required this.label, required this.amount, required this.color});

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
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textSecondary,
                fontSize: 12.sp,
              ),
            ),
            Text(
              CurrencyFormatter.formatKRW(amount),
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
                fontSize: 11.sp,
              ),
            ),
          ],
        ),
      ],
    );
  }
}