import 'package:flutter/material.dart';
import 'package:more_app/core/theme/app_colors.dart';

abstract class AppTextStyles {
  static const String _fontFamily = 'Pretendard';

  // ── Heading ──

  // Heading XL · 26/36 · Bold
  static const TextStyle headingXl = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 26,
    height: 36 / 26,
    fontWeight: FontWeight.w700,
    color: AppColors.textPrimary,
  );

  // Heading LG · 22/30 · Bold
  static const TextStyle headingLg = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 22,
    height: 30 / 22,
    fontWeight: FontWeight.w700,
    color: AppColors.textPrimary,
  );

  // Heading MD · 18/26 · Bold
  static const TextStyle headingMd = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 18,
    height: 26 / 18,
    fontWeight: FontWeight.w700,
    color: AppColors.textPrimary,
  );

  // ── Body ──

  // Body LG · 16/24 · Medium
  static const TextStyle bodyLg = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 16,
    height: 24 / 16,
    fontWeight: FontWeight.w500,
    color: AppColors.textPrimary,
  );

  // Body MD · 16/24 · Regular
  static const TextStyle bodyMd = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 16,
    height: 24 / 16,
    fontWeight: FontWeight.w400,
    color: AppColors.textPrimary,
  );

  // Body SM · 14/20 · Regular
  static const TextStyle bodySm = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 14,
    height: 20 / 14,
    fontWeight: FontWeight.w400,
    color: AppColors.textPrimary,
  );

  // ── Caption ──

  // Caption · 12/18 · Regular
  static const TextStyle caption = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 12,
    height: 18 / 12,
    fontWeight: FontWeight.w400,
    color: AppColors.textTertiary,
  );
}
