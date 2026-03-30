import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';

abstract class AppTheme {
  static const String _fontFamily = 'Pretendard';

  static ThemeData get light => _build(Brightness.light, AppColorScheme.light);
  static ThemeData get dark => _build(Brightness.dark, AppColorScheme.dark);

  static ThemeData _build(Brightness brightness, AppColorScheme colors) {
    final isDark = brightness == Brightness.dark;

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      fontFamily: _fontFamily,
      colorSchemeSeed: AppColors.orange500,
      scaffoldBackgroundColor: colors.bgSurface,
      appBarTheme: AppBarTheme(
        backgroundColor: colors.bgSurface,
        foregroundColor: colors.textPrimary,
        elevation: 0,
        centerTitle: true,
        systemOverlayStyle: isDark
            ? SystemUiOverlayStyle.light
            : SystemUiOverlayStyle.dark,
      ),
      cardTheme: CardThemeData(
        color: colors.bgSurface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16.r),
          side: BorderSide(color: colors.borderDefault),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: colors.buttonPrimary,
          foregroundColor: const Color(0xFFFFFFFF),
          minimumSize: Size(double.infinity, 52.h),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12.r),
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colors.inputBg,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.r),
          borderSide: BorderSide(color: colors.borderDefault),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.r),
          borderSide: BorderSide(color: colors.borderDefault),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.r),
          borderSide: BorderSide(color: colors.buttonPrimary, width: 2),
        ),
        contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 14.h),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: colors.bgSurface,
        selectedItemColor: colors.textPrimary,
        unselectedItemColor: colors.textTertiary,
      ),
      dividerColor: colors.borderDefault,
      extensions: [colors],
    );
  }
}
