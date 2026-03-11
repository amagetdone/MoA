import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class OnboardingAnalysisPage extends StatelessWidget {
  const OnboardingAnalysisPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: MoreAppBar(
        title: '분석 결과',
        showBack: true,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '3개월 간 소비패턴을 분석했어요.',
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.gray900,
                fontSize: 18.sp,
              ),
            ),
            SizedBox(height: 24.h),

            // Stats row
            Container(
              padding: EdgeInsets.all(16.w),
              decoration: BoxDecoration(
                color: AppColors.gray50,
                borderRadius: BorderRadius.circular(12.r),
              ),
              child: Column(
                children: [
                  _StatRow(label: '평균 수입', value: '3,000,000원'),
                  SizedBox(height: 8.h),
                  _StatRow(label: '평균 지출', value: '1,900,000원', valueColor: AppColors.gray700),
                  Divider(height: 24.h),
                  _StatRow(
                    label: '남은 금액',
                    value: '1,100,000원',
                    isBold: true,
                    valueColor: AppColors.orange500,
                  ),
                ],
              ),
            ),
            SizedBox(height: 32.h),

            // Donut chart
            Center(
              child: SizedBox(
                width: 200.w,
                height: 200.w,
                child: CustomPaint(
                  painter: _DonutChartPainter(),
                  child: Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          '1,900,000원',
                          style: AppTextStyles.headingMd.copyWith(
                            color: AppColors.gray900,
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        SizedBox(height: 2.h),
                        Text(
                          '평균 지출',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.gray500,
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
                _LegendDot(color: AppColors.orange500, label: '고정비 65%'),
                SizedBox(width: 24.w),
                _LegendDot(color: const Color(0xFF4CAF50), label: '변동비 35%'),
              ],
            ),
            SizedBox(height: 24.h),

            // Yellow banner
            Container(
              width: double.infinity,
              padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF8E1),
                borderRadius: BorderRadius.circular(8.r),
              ),
              child: Row(
                children: [
                  Icon(Icons.lightbulb_outline, size: 20.w, color: const Color(0xFFFFA000)),
                  SizedBox(width: 8.w),
                  Expanded(
                    child: Text(
                      '식비 절약이 필요해보여요.',
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
            SizedBox(height: 24.h),

            // Category bars
            _CategoryBar(name: '식비', amount: '750,000원', ratio: 0.75),
            SizedBox(height: 12.h),
            _CategoryBar(name: '여가비', amount: '550,000원', ratio: 0.55),
            SizedBox(height: 12.h),
            _CategoryBar(name: '교통비', amount: '150,000원', ratio: 0.15),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: '다음',
        onPressed: () {
          context.push(AppRoutes.onboardingPlan);
        },
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
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: AppColors.gray500,
            fontWeight: isBold ? FontWeight.w600 : FontWeight.w400,
            fontSize: 13.sp,
          ),
        ),
        Text(
          value,
          style: (isBold ? AppTextStyles.bodyLg : AppTextStyles.bodySm).copyWith(
            color: valueColor ?? AppColors.gray900,
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
            color: AppColors.gray700,
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              name,
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray700,
                fontSize: 13.sp,
              ),
            ),
            Text(
              amount,
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray900,
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
              backgroundColor: AppColors.gray100,
              valueColor: const AlwaysStoppedAnimation<Color>(AppColors.orange500),
            ),
          ),
        ),
      ],
    );
  }
}

class _DonutChartPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2;
    const strokeWidth = 28.0;
    final rect = Rect.fromCircle(center: center, radius: radius - strokeWidth / 2);

    // Fixed cost 65%
    final fixedPaint = Paint()
      ..color = AppColors.orange500
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.butt;

    // Variable cost 35%
    final variablePaint = Paint()
      ..color = const Color(0xFF4CAF50)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.butt;

    const startAngle = -math.pi / 2;
    const fixedSweep = 2 * math.pi * 0.65;
    const variableSweep = 2 * math.pi * 0.35;

    canvas.drawArc(rect, startAngle, fixedSweep, false, fixedPaint);
    canvas.drawArc(rect, startAngle + fixedSweep, variableSweep, false, variablePaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
