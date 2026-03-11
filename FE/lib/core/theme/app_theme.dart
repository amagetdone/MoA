import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';

abstract class AppTheme {
  static const String _fontFamily = 'Pretendard';

  static ThemeData get light => ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    fontFamily: _fontFamily,
    colorSchemeSeed: AppColors.orange500,
    scaffoldBackgroundColor: AppColors.bgSurface,
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.bgSurface,
      foregroundColor: AppColors.textPrimary,
      elevation: 0,
      centerTitle: true,
    ),
    cardTheme: CardThemeData(
      color: AppColors.bgSurface,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16.r),
        side: const BorderSide(color: AppColors.borderDefault),
      ),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.buttonPrimary,
        foregroundColor: AppColors.white,
        minimumSize: Size(double.infinity, 52.h),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12.r),
        ),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: AppColors.bgSurface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12.r),
        borderSide: const BorderSide(color: AppColors.borderDefault),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12.r),
        borderSide: const BorderSide(color: AppColors.borderDefault),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12.r),
        borderSide: const BorderSide(color: AppColors.buttonPrimary, width: 2),
      ),
      contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 14.h),
    ),
  );
}
