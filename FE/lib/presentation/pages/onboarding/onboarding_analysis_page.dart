import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/onboarding_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

const _sectionColors = [
  Color(0xFFE57373),
  Color(0xFFFFB74D),
  Color(0xFF81C784),
];
const _sectionLabels = ['고정지출', '변동지출', '여유 자금'];

class OnboardingAnalysisPage extends ConsumerStatefulWidget {
  const OnboardingAnalysisPage({super.key});

  @override
  ConsumerState<OnboardingAnalysisPage> createState() =>
      _OnboardingAnalysisPageState();
}

class _OnboardingAnalysisPageState extends ConsumerState<OnboardingAnalysisPage> {
  int? _touchedIndex;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final analysis = ref.watch(onboardingViewModelProvider).analysisResult;

    final averageIncome = analysis?.averageIncome ?? 0;
    final averageExpense = analysis?.averageExpense ?? 0;
    final freeAmount = analysis?.remainingAmount ?? 0;
    final fixedAmount =
        (averageExpense * ((analysis?.fixedExpenseRatio ?? 0) / 100)).round();
    final variableAmount =
        (averageExpense * ((analysis?.variableExpenseRatio ?? 0) / 100)).round();
    final sectionAmounts = [fixedAmount, variableAmount, freeAmount];
    final sectionTotal = sectionAmounts.fold<int>(0, (sum, amount) => sum + amount);
    final categories = analysis?.categoryExpenses ?? const <CategoryExpense>[];

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
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
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
                                    setState(() => _touchedIndex = null);
                                    return;
                                  }
                                  final idx =
                                      response.touchedSection!.touchedSectionIndex;
                                  setState(() {
                                    _touchedIndex = idx >= 0 ? idx : null;
                                  });
                                },
                              ),
                              sections: _buildSections(
                                touchedIndex: _touchedIndex,
                                amounts: sectionAmounts,
                                total: sectionTotal,
                              ),
                            ),
                          ),
                          Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                _touchedIndex != null
                                    ? CurrencyFormatter.formatKRW(
                                        sectionAmounts[_touchedIndex!],
                                      )
                                    : CurrencyFormatter.formatKRW(averageExpense),
                                style: AppTextStyles.bodyMd.copyWith(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16.sp,
                                  color: _touchedIndex != null
                                      ? _sectionColors[_touchedIndex!]
                                      : colors.textPrimary,
                                ),
                              ),
                              Text(
                                _touchedIndex != null
                                    ? _sectionLabels[_touchedIndex!]
                                    : '평균 지출',
                                style: AppTextStyles.caption.copyWith(
                                  color: colors.textTertiary,
                                  fontSize: 12.sp,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    SizedBox(height: 24.h),
                    Column(
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: _LegendItem(
                                label: '고정지출',
                                amount: fixedAmount,
                                color: _sectionColors[0],
                              ),
                            ),
                            Expanded(
                              child: _LegendItem(
                                label: '변동지출',
                                amount: variableAmount,
                                color: _sectionColors[1],
                              ),
                            ),
                          ],
                        ),
                        SizedBox(height: 12.h),
                        Row(
                          children: [
                            Expanded(
                              child: _LegendItem(
                                label: '여유 자금',
                                amount: freeAmount,
                                color: _sectionColors[2],
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    SizedBox(height: 32.h),
                    Column(
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
                      ],
                    ),
                    SizedBox(height: 24.h),
                    ...categories.asMap().entries.map((entry) {
                      final category = entry.value;
                      final ratio = averageExpense > 0
                          ? (category.amount / averageExpense).clamp(0.0, 1.0)
                          : 0.0;
                      return Padding(
                        padding: EdgeInsets.only(
                          bottom: entry.key < categories.length - 1 ? 12.h : 0,
                        ),
                        child: _CategoryBar(
                          name: category.categoryName,
                          amount: category.amount,
                          totalAmount: averageExpense,
                          ratio: ratio,
                        ),
                      );
                    }),
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

  List<PieChartSectionData> _buildSections({
    required int? touchedIndex,
    required List<int> amounts,
    required int total,
  }) {
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

class _LegendItem extends StatelessWidget {
  const _LegendItem({
    required this.label,
    required this.amount,
    required this.color,
  });

  final String label;
  final int amount;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 8.w,
          height: 8.w,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        SizedBox(width: 8.w),
        Flexible(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.caption.copyWith(
                  color: colors.textSecondary,
                  fontSize: 12.sp,
                ),
              ),
              Text(
                CurrencyFormatter.formatKRW(amount),
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.caption.copyWith(
                  color: colors.textTertiary,
                  fontSize: 11.sp,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _CategoryBar extends StatelessWidget {
  const _CategoryBar({
    required this.name,
    required this.amount,
    required this.totalAmount,
    required this.ratio,
  });

  final String name;
  final int amount;
  final int totalAmount;
  final double ratio;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final percent = totalAmount > 0 ? (ratio * 100).toStringAsFixed(1) : '0.0';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                name,
                style: AppTextStyles.bodyMd.copyWith(
                  color: colors.textPrimary,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            Flexible(
              child: RichText(
                textAlign: TextAlign.end,
                text: TextSpan(
                  style: AppTextStyles.bodySm.copyWith(color: colors.textPrimary),
                  children: [
                    TextSpan(
                      text: CurrencyFormatter.formatKRW(amount),
                      style: AppTextStyles.bodyMd.copyWith(
                        color: colors.textPrimary,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    TextSpan(
                      text: ' / ${CurrencyFormatter.formatKRW(totalAmount)} ',
                      style: AppTextStyles.bodySm.copyWith(
                        color: colors.textTertiary,
                      ),
                    ),
                    TextSpan(
                      text: '($percent%)',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.orange500,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
        SizedBox(height: 8.h),
        ClipRRect(
          borderRadius: BorderRadius.circular(999.r),
          child: LinearProgressIndicator(
            value: ratio,
            minHeight: 8.h,
            backgroundColor: colors.gray50,
            valueColor: const AlwaysStoppedAnimation<Color>(AppColors.orange500),
          ),
        ),
      ],
    );
  }
}
