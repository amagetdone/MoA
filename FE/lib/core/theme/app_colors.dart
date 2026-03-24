import 'package:flutter/material.dart';

/// Primitive accent colors (theme-independent)
abstract class AppColors {
  // Orange
  static const Color orange50 = Color(0xFFFFF8F0);
  static const Color orange100 = Color(0xFFFFF6ED);
  static const Color orange300 = Color(0xFFE8A86B);
  static const Color orange500 = Color(0xFFE8873B);
  static const Color orange600 = Color(0xFFD07730);

  // Green
  static const Color green500 = Color(0xFF00B852);

  // Red
  static const Color red500 = Color(0xFFE53535);

  /// Theme-dependent colors accessor
  static AppColorScheme of(BuildContext context) {
    return Theme.of(context).extension<AppColorScheme>()!;
  }
}

class AppColorScheme extends ThemeExtension<AppColorScheme> {
  // Primitive grays (theme-dependent)
  final Color white;
  final Color gray50;
  final Color gray100;
  final Color gray500;
  final Color gray700;
  final Color gray900;

  // Background
  final Color bgPrimary;
  final Color bgSurface;
  final Color bgAccent;
  final Color bgInsight;

  // Text
  final Color textPrimary;
  final Color textSecondary;
  final Color textTertiary;
  final Color textAccent;

  // Border
  final Color borderDefault;

  // Button
  final Color buttonPrimary;
  final Color buttonPressed;
  final Color buttonDisabled;

  // Status
  final Color statusSuccess;
  final Color statusError;

  // Misc
  final Color cardShadow;
  final Color dimText;
  final Color inputBg;

  const AppColorScheme({
    required this.white,
    required this.gray50,
    required this.gray100,
    required this.gray500,
    required this.gray700,
    required this.gray900,
    required this.bgPrimary,
    required this.bgSurface,
    required this.bgAccent,
    required this.bgInsight,
    required this.textPrimary,
    required this.textSecondary,
    required this.textTertiary,
    required this.textAccent,
    required this.borderDefault,
    required this.buttonPrimary,
    required this.buttonPressed,
    required this.buttonDisabled,
    required this.statusSuccess,
    required this.statusError,
    required this.cardShadow,
    required this.dimText,
    required this.inputBg,
  });

  // ── Light (current design) ──
  static const light = AppColorScheme(
    white: Color(0xFFFFFFFF),
    gray50: Color(0xFFF5F5F5),
    gray100: Color(0xFFE0E0E0),
    gray500: Color(0xFF9E9E9E),
    gray700: Color(0xFF4D4D4D),
    gray900: Color(0xFF1A1A1A),
    bgPrimary: Color(0xFFF5F5F5),
    bgSurface: Color(0xFFFFFFFF),
    bgAccent: Color(0xFFFFF8F0),
    bgInsight: Color(0xFFFFF6ED),
    textPrimary: Color(0xFF1A1A1A),
    textSecondary: Color(0xFF4D4D4D),
    textTertiary: Color(0xFF9E9E9E),
    textAccent: Color(0xFFE8873B),
    borderDefault: Color(0xFFE0E0E0),
    buttonPrimary: Color(0xFFE8873B),
    buttonPressed: Color(0xFFD07730),
    buttonDisabled: Color(0xFFE0E0E0),
    statusSuccess: Color(0xFF00B852),
    statusError: Color(0xFFE53535),
    cardShadow: Color(0x08000000),
    dimText: Color(0x61000000),
    inputBg: Color(0xFFFFFFFF),
  );

  // ── Dark (Toss-style) ──
  static const dark = AppColorScheme(
    white: Color(0xFF222226),
    gray50: Color(0xFF222226),
    gray100: Color(0xFF313136),
    gray500: Color(0xFF6E6E73),
    gray700: Color(0xFFA0A0A0),
    gray900: Color(0xFFF0F0F0),
    bgPrimary: Color(0xFF17171A),
    bgSurface: Color(0xFF222226),
    bgAccent: Color(0xFF1E1610),
    bgInsight: Color(0xFF2A1A0A),
    textPrimary: Color(0xFFF0F0F0),
    textSecondary: Color(0xFFA0A0A0),
    textTertiary: Color(0xFF6E6E73),
    textAccent: Color(0xFFE8873B),
    borderDefault: Color(0xFF333338),
    buttonPrimary: Color(0xFFE8873B),
    buttonPressed: Color(0xFFD07730),
    buttonDisabled: Color(0xFF404045),
    statusSuccess: Color(0xFF30D158),
    statusError: Color(0xFFFF453A),
    cardShadow: Color(0x00000000),
    dimText: Color(0x99FFFFFF),
    inputBg: Color(0xFF2A2A2F),
  );

  @override
  AppColorScheme copyWith({
    Color? white,
    Color? gray50,
    Color? gray100,
    Color? gray500,
    Color? gray700,
    Color? gray900,
    Color? bgPrimary,
    Color? bgSurface,
    Color? bgAccent,
    Color? bgInsight,
    Color? textPrimary,
    Color? textSecondary,
    Color? textTertiary,
    Color? textAccent,
    Color? borderDefault,
    Color? buttonPrimary,
    Color? buttonPressed,
    Color? buttonDisabled,
    Color? statusSuccess,
    Color? statusError,
    Color? cardShadow,
    Color? dimText,
    Color? inputBg,
  }) {
    return AppColorScheme(
      white: white ?? this.white,
      gray50: gray50 ?? this.gray50,
      gray100: gray100 ?? this.gray100,
      gray500: gray500 ?? this.gray500,
      gray700: gray700 ?? this.gray700,
      gray900: gray900 ?? this.gray900,
      bgPrimary: bgPrimary ?? this.bgPrimary,
      bgSurface: bgSurface ?? this.bgSurface,
      bgAccent: bgAccent ?? this.bgAccent,
      bgInsight: bgInsight ?? this.bgInsight,
      textPrimary: textPrimary ?? this.textPrimary,
      textSecondary: textSecondary ?? this.textSecondary,
      textTertiary: textTertiary ?? this.textTertiary,
      textAccent: textAccent ?? this.textAccent,
      borderDefault: borderDefault ?? this.borderDefault,
      buttonPrimary: buttonPrimary ?? this.buttonPrimary,
      buttonPressed: buttonPressed ?? this.buttonPressed,
      buttonDisabled: buttonDisabled ?? this.buttonDisabled,
      statusSuccess: statusSuccess ?? this.statusSuccess,
      statusError: statusError ?? this.statusError,
      cardShadow: cardShadow ?? this.cardShadow,
      dimText: dimText ?? this.dimText,
      inputBg: inputBg ?? this.inputBg,
    );
  }

  @override
  AppColorScheme lerp(covariant AppColorScheme? other, double t) {
    if (other is! AppColorScheme) return this;
    return AppColorScheme(
      white: Color.lerp(white, other.white, t)!,
      gray50: Color.lerp(gray50, other.gray50, t)!,
      gray100: Color.lerp(gray100, other.gray100, t)!,
      gray500: Color.lerp(gray500, other.gray500, t)!,
      gray700: Color.lerp(gray700, other.gray700, t)!,
      gray900: Color.lerp(gray900, other.gray900, t)!,
      bgPrimary: Color.lerp(bgPrimary, other.bgPrimary, t)!,
      bgSurface: Color.lerp(bgSurface, other.bgSurface, t)!,
      bgAccent: Color.lerp(bgAccent, other.bgAccent, t)!,
      bgInsight: Color.lerp(bgInsight, other.bgInsight, t)!,
      textPrimary: Color.lerp(textPrimary, other.textPrimary, t)!,
      textSecondary: Color.lerp(textSecondary, other.textSecondary, t)!,
      textTertiary: Color.lerp(textTertiary, other.textTertiary, t)!,
      textAccent: Color.lerp(textAccent, other.textAccent, t)!,
      borderDefault: Color.lerp(borderDefault, other.borderDefault, t)!,
      buttonPrimary: Color.lerp(buttonPrimary, other.buttonPrimary, t)!,
      buttonPressed: Color.lerp(buttonPressed, other.buttonPressed, t)!,
      buttonDisabled: Color.lerp(buttonDisabled, other.buttonDisabled, t)!,
      statusSuccess: Color.lerp(statusSuccess, other.statusSuccess, t)!,
      statusError: Color.lerp(statusError, other.statusError, t)!,
      cardShadow: Color.lerp(cardShadow, other.cardShadow, t)!,
      dimText: Color.lerp(dimText, other.dimText, t)!,
      inputBg: Color.lerp(inputBg, other.inputBg, t)!,
    );
  }
}
