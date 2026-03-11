import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';

abstract class AppTextStyles {
  static const String _fontFamily = 'Pretendard';

  // ── Heading ──

  // Heading XL · 26/36 · Bold
  static TextStyle get headingXl => TextStyle(
    fontFamily: _fontFamily,
    fontSize: 26.sp,
    height: 36 / 26,
    fontWeight: FontWeight.w700,
    color: AppColors.textPrimary,
  );

  // Heading LG · 22/30 · Bold
  static TextStyle get headingLg => TextStyle(
    fontFamily: _fontFamily,
    fontSize: 22.sp,
    height: 30 / 22,
    fontWeight: FontWeight.w700,
    color: AppColors.textPrimary,
  );

  // Heading MD · 18/26 · Bold
  static TextStyle get headingMd => TextStyle(
    fontFamily: _fontFamily,
    fontSize: 18.sp,
    height: 26 / 18,
    fontWeight: FontWeight.w700,
    color: AppColors.textPrimary,
  );

  // ── Body ──

  // Body LG · 16/24 · Medium
  static TextStyle get bodyLg => TextStyle(
    fontFamily: _fontFamily,
    fontSize: 16.sp,
    height: 24 / 16,
    fontWeight: FontWeight.w500,
    color: AppColors.textPrimary,
  );

  // Body MD · 16/24 · Regular
  static TextStyle get bodyMd => TextStyle(
    fontFamily: _fontFamily,
    fontSize: 16.sp,
    height: 24 / 16,
    fontWeight: FontWeight.w400,
    color: AppColors.textPrimary,
  );

  // Body SM · 14/20 · Regular
  static TextStyle get bodySm => TextStyle(
    fontFamily: _fontFamily,
    fontSize: 14.sp,
    height: 20 / 14,
    fontWeight: FontWeight.w400,
    color: AppColors.textPrimary,
  );

  // ── Caption ──

  // Caption · 12/18 · Regular
  static TextStyle get caption => TextStyle(
    fontFamily: _fontFamily,
    fontSize: 12.sp,
    height: 18 / 12,
    fontWeight: FontWeight.w400,
    color: AppColors.textTertiary,
  );
}
