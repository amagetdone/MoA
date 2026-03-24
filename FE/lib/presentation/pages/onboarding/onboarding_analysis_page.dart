import 'dart:math' as math;
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

class OnboardingAnalysisPage extends ConsumerWidget {
  const OnboardingAnalysisPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final analysis = ref.watch(onboardingViewModelProvider).analysisResult;

    final averageIncome = analysis?.averageIncome ?? 0;
    final averageExpense = analysis?.averageExpense ?? 0;
    final remainingAmount = analysis?.remainingAmount ?? 0;
    final fixedRatio = (analysis?.fixedExpenseRatio ?? 65.0) / 100;
    final variableRatio = (analysis?.variableExpenseRatio ?? 35.0) / 100;
    final insightMessage = analysis?.insightMessage ?? '';
    final categories = analysis?.categoryExpenses ?? [];

    return PopScope(
      canPop: false,
      child: Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: MoreAppBar(
          title: '',
          showBack: false,
        ),
        body: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '분석 결과',
                style: AppTextStyles.headingLg.copyWith(
                  color: colors.textPrimary,
                  fontSize: 24.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: 6.h),
              Text(
                analysis?.summaryText ?? '3개월 간 소비패턴을 분석했어요.',
                style: AppTextStyles.bodyMd.copyWith(
                  color: colors.gray500,
                  fontSize: 14.sp,
                ),
              ),
              SizedBox(height: 24.h),

              // Stats card
              MoreCard(
                child: Column(
                  children: [
                    _StatRow(
                      label: '평균 수입',
                      value: CurrencyFormatter.formatKRW(averageIncome),
                    ),
                    SizedBox(height: 8.h),
                    _StatRow(
                      label: '평균 지출',
                      value: CurrencyFormatter.formatKRW(averageExpense),
                      valueColor: AppColors.orange500,
                    ),
                    Divider(height: 24.h),
                    _StatRow(
                      label: '남은 금액',
                      value: CurrencyFormatter.formatKRW(remainingAmount),
                      isBold: true,
                      valueColor: const Color(0xFF4CAF50),
                    ),
                  ],
                ),
              ),
              SizedBox(height: 12.h),

              // Donut chart card
              MoreCard(
                child: Column(
                  children: [
                    Center(
                      child: SizedBox(
                        width: 200.w,
                        height: 200.w,
                        child: CustomPaint(
                          painter: _DonutChartPainter(
                            fixedRatio: fixedRatio,
                            variableRatio: variableRatio,
                          ),
                          child: Center(
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  CurrencyFormatter.formatKRW(averageExpense),
                                  style: AppTextStyles.headingMd.copyWith(
                                    color: colors.gray900,
                                    fontSize: 16.sp,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                                SizedBox(height: 2.h),
                                Text(
                                  '평균 지출',
                                  style: AppTextStyles.bodySm.copyWith(
                                    color: colors.gray500,
                                    fontSize: 12.sp,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                    SizedBox(height: 16.h),

                    // Legend
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        _LegendDot(
                          color: AppColors.orange500,
                          label:
                              '고정비 ${(fixedRatio * 100).toStringAsFixed(0)}%',
                        ),
                        SizedBox(width: 24.w),
                        _LegendDot(
                          color: const Color(0xFF4CAF50),
                          label:
                              '변동비 ${(variableRatio * 100).toStringAsFixed(0)}%',
                        ),
                      ],
                    ),

                    if (insightMessage.isNotEmpty) ...[
                      SizedBox(height: 16.h),
                      Container(
                        width: double.infinity,
                        padding: EdgeInsets.symmetric(
                            horizontal: 16.w, vertical: 12.h),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFF8E1),
                          borderRadius: BorderRadius.circular(8.r),
                        ),
                        child: Row(
                          children: [
                            Icon(Icons.lightbulb_outline,
                                size: 20.w,
                                color: const Color(0xFFFFA000)),
                            SizedBox(width: 8.w),
                            Expanded(
                              child: Text(
                                insightMessage,
                                style: AppTextStyles.bodySm.copyWith(
                                  color: const Color(0xFFF57C00),
                                  fontWeight: FontWeight.w600,
                                  fontSize: 13.sp,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],

                    if (categories.isNotEmpty) ...[
                      SizedBox(height: 20.h),
                      Align(
                        alignment: Alignment.centerLeft,
                        child: Text(
                          '카테고리별 지출',
                          style: AppTextStyles.bodyMd.copyWith(
                            color: colors.gray900,
                            fontWeight: FontWeight.w600,
                            fontSize: 14.sp,
                          ),
                        ),
                      ),
                      SizedBox(height: 12.h),
                      ...categories.asMap().entries.map((entry) {
                        final cat = entry.value;
                        final ratio = averageExpense > 0
                            ? (cat.amount / averageExpense).clamp(0.0, 1.0)
                            : 0.0;
                        return Padding(
                          padding: EdgeInsets.only(
                              bottom: entry.key < categories.length - 1
                                  ? 12.h
                                  : 0),
                          child: _CategoryBar(
                            name: cat.categoryName,
                            amount: CurrencyFormatter.formatKRW(cat.amount),
                            ratio: ratio,
                          ),
                        );
                      }),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
        bottomNavigationBar: MoreBottomButton(
          text: '다음',
          progress: 5 / 7,
          onPressed: () {
            context.go(AppRoutes.onboardingGoal);
          },
        ),
      ),
    );
  }
}

class _StatRow extends StatelessWidget {
  final String label;
  final String value;
  final bool isBold;
  final Color? valueColor;

  const _StatRow({
    required this.label,
    required this.value,
    this.isBold = false,
    this.valueColor,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: colors.gray500,
            fontWeight: isBold ? FontWeight.w600 : FontWeight.w400,
            fontSize: 13.sp,
          ),
        ),
        Text(
          value,
          style:
              (isBold ? AppTextStyles.bodyLg : AppTextStyles.bodySm).copyWith(
            color: valueColor ?? colors.gray900,
            fontWeight: isBold ? FontWeight.w700 : FontWeight.w500,
            fontSize: isBold ? 16.sp : 13.sp,
          ),
        ),
      ],
    );
  }
}

class _LegendDot extends StatelessWidget {
  final Color color;
  final String label;

  const _LegendDot({required this.color, required this.label});

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 10.w,
          height: 10.w,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        SizedBox(width: 6.w),
        Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: colors.gray700,
            fontSize: 13.sp,
          ),
        ),
      ],
    );
  }
}

class _CategoryBar extends StatelessWidget {
  final String name;
  final String amount;
  final double ratio;

  const _CategoryBar({
    required this.name,
    required this.amount,
    required this.ratio,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              name,
              style: AppTextStyles.bodySm.copyWith(
                color: colors.gray700,
                fontSize: 13.sp,
              ),
            ),
            Text(
              amount,
              style: AppTextStyles.bodySm.copyWith(
                color: colors.gray900,
                fontWeight: FontWeight.w600,
                fontSize: 13.sp,
              ),
            ),
          ],
        ),
        SizedBox(height: 6.h),
        ClipRRect(
          borderRadius: BorderRadius.circular(4.r),
          child: SizedBox(
            height: 8.h,
            child: LinearProgressIndicator(
              value: ratio,
              backgroundColor: colors.gray100,
              valueColor:
                  const AlwaysStoppedAnimation<Color>(AppColors.orange500),
            ),
          ),
        ),
      ],
    );
  }
}

class _DonutChartPainter extends CustomPainter {
  final double fixedRatio;
  final double variableRatio;

  const _DonutChartPainter({
    required this.fixedRatio,
    required this.variableRatio,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2;
    const strokeWidth = 28.0;
    final rect =
        Rect.fromCircle(center: center, radius: radius - strokeWidth / 2);

    final fixedPaint = Paint()
      ..color = AppColors.orange500
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.butt;

    final variablePaint = Paint()
      ..color = const Color(0xFF4CAF50)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.butt;

    const startAngle = -math.pi / 2;
    final fixedSweep = 2 * math.pi * fixedRatio;
    final variableSweep = 2 * math.pi * variableRatio;

    canvas.drawArc(rect, startAngle, fixedSweep, false, fixedPaint);
    canvas.drawArc(
        rect, startAngle + fixedSweep, variableSweep, false, variablePaint);
  }

  @override
  bool shouldRepaint(covariant _DonutChartPainter oldDelegate) =>
      oldDelegate.fixedRatio != fixedRatio ||
      oldDelegate.variableRatio != variableRatio;
}
