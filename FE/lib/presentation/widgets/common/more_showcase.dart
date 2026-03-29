import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:showcaseview/showcaseview.dart';

class MoreShowcase extends StatelessWidget {
  const MoreShowcase({
    super.key,
    required this.showcaseKey,
    required this.title,
    required this.description,
    required this.child,
    this.tooltipPosition,
    this.targetPadding = EdgeInsets.zero,
    this.targetShapeBorder,
    this.blurValue,
    this.overlayOpacity,
    this.disableMovingAnimation,
    this.tooltipOffsetX = 0,
    this.onSkip,
  });

  final GlobalKey showcaseKey;
  final String title;
  final String description;
  final Widget child;
  final TooltipPosition? tooltipPosition;
  final EdgeInsets targetPadding;
  final ShapeBorder? targetShapeBorder;
  final double? blurValue;
  final double? overlayOpacity;
  final bool? disableMovingAnimation;
  final double tooltipOffsetX;
  final VoidCallback? onSkip;

  @override
  Widget build(BuildContext context) {
    final showcase = ShowCaseWidget.of(context);
    final tooltipWidth = math.min(MediaQuery.sizeOf(context).width - 36.w, 320.w);
    final estimatedLines = description
        .split('\n')
        .fold<int>(0, (sum, line) => sum + math.max(1, (line.length / 26).ceil()));
    final tooltipHeight =
        (88.h + estimatedLines * 12.h).clamp(116.h, 156.h).toDouble();

    return Showcase.withWidget(
      key: showcaseKey,
      width: tooltipWidth,
      height: tooltipHeight,
      tooltipPosition: tooltipPosition,
      targetPadding: targetPadding,
      targetShapeBorder: targetShapeBorder ??
          const RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(12)),
          ),
      blurValue: blurValue,
      overlayOpacity: overlayOpacity ?? 0.75,
      disableMovingAnimation: disableMovingAnimation,
      toolTipSlideEndDistance: 11,
      container: Transform.translate(
        offset: Offset(tooltipOffsetX, 0),
        child: _MoreShowcaseTooltip(
          maxWidth: tooltipWidth,
          title: title,
          description: description,
          onSkip: () {
            onSkip?.call();
            showcase.dismiss();
          },
          onNext: showcase.next,
        ),
      ),
      child: child,
    );
  }
}

class ShowcaseHelpButton extends StatelessWidget {
  const ShowcaseHelpButton({
    super.key,
    required this.onTap,
    this.backgroundColor,
    this.textColor,
  });

  final VoidCallback onTap;
  final Color? backgroundColor;
  final Color? textColor;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 26.w,
        height: 26.w,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: backgroundColor ?? colors.borderDefault,
        ),
        child: Center(
          child: Text(
            '?',
            style: TextStyle(
              color: textColor ?? colors.textSecondary,
              fontSize: 13.sp,
              fontWeight: FontWeight.w800,
              height: 1,
            ),
          ),
        ),
      ),
    );
  }
}

class _MoreShowcaseTooltip extends StatelessWidget {
  const _MoreShowcaseTooltip({
    required this.maxWidth,
    required this.title,
    required this.description,
    this.onSkip,
    this.onNext,
  });

  final double maxWidth;
  final String title;
  final String description;
  final VoidCallback? onSkip;
  final VoidCallback? onNext;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return SizedBox(
      width: maxWidth,
      child: Container(
        padding: EdgeInsets.fromLTRB(14.w, 14.h, 14.w, 10.h),
        decoration: BoxDecoration(
          color: colors.bgSurface,
          borderRadius: BorderRadius.circular(18.r),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.12),
              blurRadius: 14.r,
              offset: const Offset(0, 5),
            ),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              title,
              style: AppTextStyles.bodyMd.copyWith(
                fontWeight: FontWeight.w800,
                fontSize: 15.sp,
                color: AppColors.orange500,
              ),
            ),
            SizedBox(height: 6.h),
            ConstrainedBox(
              constraints: BoxConstraints(maxHeight: 96.h),
              child: SingleChildScrollView(
                physics: const BouncingScrollPhysics(),
                child: Text(
                  description,
                  style: AppTextStyles.bodySm.copyWith(
                    color: colors.textPrimary,
                    fontWeight: FontWeight.w600,
                    height: 1.32,
                    fontSize: 13.sp,
                  ),
                ),
              ),
            ),
            SizedBox(height: 6.h),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: onSkip,
                  style: TextButton.styleFrom(
                    padding: EdgeInsets.symmetric(
                      horizontal: 6.w,
                      vertical: 4.h,
                    ),
                    minimumSize: Size.zero,
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                  child: Text(
                    '건너뛰기',
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textTertiary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                SizedBox(width: 8.w),
                FilledButton(
                  onPressed: onNext,
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.orange500,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999.r),
                    ),
                    padding: EdgeInsets.symmetric(
                      horizontal: 12.w,
                      vertical: 6.h,
                    ),
                    minimumSize: Size.zero,
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                  child: Text(
                    '다음',
                    style: AppTextStyles.bodySm.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
