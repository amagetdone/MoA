import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';

class SpendingCard extends StatelessWidget {
  const SpendingCard({
    super.key,
    required this.totalAmount,
    required this.usedAmount,
    required this.remainingAmount,
    required this.daysRemaining,
    required this.dailyAvailable,
  });

  final int totalAmount;
  final int usedAmount;
  final int remainingAmount;
  final int daysRemaining;
  final int dailyAvailable;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final isEmpty = totalAmount <= 0;
    final remainRatio = totalAmount > 0
        ? (remainingAmount / totalAmount).clamp(0.0, 1.0)
        : 0.0;
    final todayTarget = DateTime.now().weekday / 7;
    // 여유자금: 남은 비율이 (1 - 오늘까지 써도 되는 비율)보다 높거나 같으면 초록
    final isOnTrack = remainRatio >= (1 - todayTarget);
    final amountColor = isOnTrack ? const Color(0xFF53A65A) : AppColors.red500;

    return GaugeCard(
      fillRatio: remainRatio,
      fillColor: AppColors.orange500.withValues(alpha: 0.15),
      fillFromTop: false,
      todayLineFromTop: true,
      todayLineColor: amountColor,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            children: [
              Icon(Icons.account_balance_wallet_outlined,
                  size: 14.w, color: amountColor),
              SizedBox(width: 4.w),
              Text(
                '여유자금',
                style: AppTextStyles.caption.copyWith(
                  color: colors.textSecondary,
                  fontWeight: FontWeight.w600,
                  fontSize: 11.sp,
                ),
              ),
            ],
          ),
          SizedBox(height: 10.h),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              CurrencyFormatter.formatKRW(remainingAmount),
              style: AppTextStyles.headingMd.copyWith(
                color: amountColor,
                fontWeight: FontWeight.w800,
                fontSize: 17.sp,
              ),
            ),
          ),
          SizedBox(height: 6.h),
          Text(
            isEmpty
                ? '계좌를 연결해주세요'
                : '/ ${CurrencyFormatter.formatKRW(totalAmount)}',
            style: TextStyle(
              color: isEmpty ? AppColors.orange500 : colors.textTertiary,
              fontSize: 10.sp,
              fontWeight: isEmpty ? FontWeight.w700 : FontWeight.w400,
            ),
          ),
        ],
      ),
    );
  }
}

/// 카드 배경을 게이지처럼 채우는 공용 위젯
class GaugeCard extends StatelessWidget {
  const GaugeCard({
    super.key,
    required this.fillRatio,
    required this.fillColor,
    required this.child,
    this.fillFromTop = false,
    this.todayLineFromTop = false,
    this.todayLineColor,
  });

  final double fillRatio;
  final Color fillColor;
  final Widget child;
  final bool fillFromTop;
  final bool todayLineFromTop;
  final Color? todayLineColor;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final todayRatio = DateTime.now().weekday / 7;

    return Container(
      decoration: BoxDecoration(
        color: colors.bgSurface,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: colors.cardShadow,
            offset: Offset(0, 2.h),
            blurRadius: 8.r,
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          // Gauge fill
          Positioned.fill(
            child: Align(
              alignment: fillFromTop
                  ? Alignment.topCenter
                  : Alignment.bottomCenter,
              child: FractionallySizedBox(
                heightFactor: fillRatio,
                widthFactor: 1.0,
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: fillFromTop
                          ? Alignment.bottomCenter
                          : Alignment.topCenter,
                      end: fillFromTop
                          ? Alignment.topCenter
                          : Alignment.bottomCenter,
                      colors: [
                        fillColor.withValues(alpha: 0.0),
                        fillColor,
                      ],
                      stops: const [0.0, 0.3],
                    ),
                  ),
                ),
              ),
            ),
          ),
          // Today marker line
          Positioned.fill(
            child: CustomPaint(
              painter: _WeekDividerPainter(
                todayRatio: todayRatio,
                todayColor: todayLineColor ?? colors.borderDefault.withValues(alpha: 0.5),
                fromTop: todayLineFromTop,
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.all(14.w),
            child: child,
          ),
        ],
      ),
    );
  }
}

class _WeekDividerPainter extends CustomPainter {
  _WeekDividerPainter({
    required this.todayRatio,
    required this.todayColor,
    required this.fromTop,
  });

  final double todayRatio;
  final Color todayColor;
  final bool fromTop;

  @override
  void paint(Canvas canvas, Size size) {
    final todayPaint = Paint()
      ..color = todayColor
      ..strokeWidth = 1.2;

    // fromTop: 위에서 아래 (여유자금) → todayY = height * todayRatio
    // !fromTop: 아래에서 위 (저축) → todayY = height * (1 - todayRatio)
    final todayY = fromTop
        ? size.height * todayRatio
        : size.height * (1 - todayRatio);

    final lineWidth = size.width * 0.15;
    canvas.drawLine(
      Offset(size.width - lineWidth, todayY),
      Offset(size.width, todayY),
      todayPaint,
    );
  }

  @override
  bool shouldRepaint(covariant _WeekDividerPainter oldDelegate) =>
      oldDelegate.todayRatio != todayRatio ||
      oldDelegate.todayColor != todayColor ||
      oldDelegate.fromTop != fromTop;
}
