import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class AppPinKeypad extends StatefulWidget {
  const AppPinKeypad({
    super.key,
    required this.pin,
    required this.pinLength,
    required this.title,
    required this.subtitle,
    required this.onDigitPressed,
    required this.onBackspace,
    this.errorText,
    this.bottomContent,
    this.shuffleKey,
    this.enabled = true,
  });

  final String pin;
  final int pinLength;
  final String title;
  final String subtitle;
  final ValueChanged<String> onDigitPressed;
  final VoidCallback onBackspace;
  final String? errorText;
  final Widget? bottomContent;
  final Object? shuffleKey;
  final bool enabled;

  @override
  State<AppPinKeypad> createState() => _AppPinKeypadState();
}

class _AppPinKeypadState extends State<AppPinKeypad> {
  final Random _random = Random();
  late List<int> _digits;

  @override
  void initState() {
    super.initState();
    _digits = _generateShuffledDigits();
  }

  @override
  void didUpdateWidget(covariant AppPinKeypad oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.shuffleKey != oldWidget.shuffleKey) {
      _shuffleDigits();
    }
  }

  void _shuffleDigits() {
    final digits = _generateShuffledDigits();
    setState(() {
      _digits = digits;
    });
  }

  List<int> _generateShuffledDigits() {
    final digits = List<int>.generate(10, (index) => index);
    digits.shuffle(_random);
    return digits;
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          widget.title,
          textAlign: TextAlign.center,
          style: AppTextStyles.headingLg.copyWith(
            color: colors.textPrimary,
            fontWeight: FontWeight.w800,
            fontSize: 28.sp,
          ),
        ),
        SizedBox(height: 10.h),
        Text(
          widget.subtitle,
          textAlign: TextAlign.center,
          style: AppTextStyles.bodyMd.copyWith(
            color: colors.textSecondary,
            height: 1.5,
          ),
        ),
        SizedBox(height: 24.h),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(widget.pinLength, (index) {
            final filled = index < widget.pin.length;
            return Container(
              width: 14.w,
              height: 14.w,
              margin: EdgeInsets.symmetric(horizontal: 8.w),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: filled ? AppColors.orange500 : colors.gray100,
              ),
            );
          }),
        ),
        if (widget.errorText != null) ...[
          SizedBox(height: 10.h),
          Text(
            widget.errorText!,
            textAlign: TextAlign.center,
            style: AppTextStyles.caption.copyWith(
              color: AppColors.red500,
              fontSize: 12.sp,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
        SizedBox(height: 28.h),
        _buildDigitRows(colors),
        if (widget.bottomContent != null) ...[
          SizedBox(height: 20.h),
          widget.bottomContent!,
        ],
      ],
    );
  }

  Widget _buildDigitRows(AppColorScheme colors) {
    final bottomDigit = _digits[9];

    return Column(
      children: [
        _buildRow(_digits.sublist(0, 3), colors),
        SizedBox(height: 10.h),
        _buildRow(_digits.sublist(3, 6), colors),
        SizedBox(height: 10.h),
        _buildRow(_digits.sublist(6, 9), colors),
        SizedBox(height: 10.h),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _buildActionButton(
              colors: colors,
              icon: Icons.shuffle_rounded,
              onPressed: widget.enabled ? _shuffleDigits : null,
            ),
            SizedBox(width: 12.w),
            _buildDigitButton(bottomDigit.toString(), colors),
            SizedBox(width: 12.w),
            _buildActionButton(
              colors: colors,
              icon: Icons.backspace_outlined,
              onPressed: widget.enabled ? widget.onBackspace : null,
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildRow(List<int> digits, AppColorScheme colors) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        for (final digit in digits) ...[
          _buildDigitButton(digit.toString(), colors),
          if (digit != digits.last) SizedBox(width: 12.w),
        ],
      ],
    );
  }

  Widget _buildDigitButton(String digit, AppColorScheme colors) {
    return SizedBox(
      width: 76.w,
      height: 60.h,
      child: FilledButton(
        onPressed: widget.enabled && widget.pin.length < widget.pinLength
            ? () => widget.onDigitPressed(digit)
            : null,
        style: FilledButton.styleFrom(
          backgroundColor: colors.gray50,
          disabledBackgroundColor: colors.gray100,
          foregroundColor: colors.textPrimary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18.r),
          ),
          elevation: 0,
        ),
        child: Text(
          digit,
          style: AppTextStyles.headingMd.copyWith(
            color: colors.textPrimary,
            fontSize: 24.sp,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }

  Widget _buildActionButton({
    required AppColorScheme colors,
    required IconData icon,
    required VoidCallback? onPressed,
  }) {
    return SizedBox(
      width: 76.w,
      height: 60.h,
      child: OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(
          side: BorderSide(color: colors.borderDefault),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18.r),
          ),
        ),
        child: Icon(
          icon,
          color: colors.textSecondary,
          size: 22.w,
        ),
      ),
    );
  }
}
