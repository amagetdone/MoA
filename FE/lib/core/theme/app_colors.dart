import 'package:flutter/material.dart';

abstract class AppColors {
  // ── Primitive ──

  static const Color white = Color(0xFFFFFFFF);

  // Gray
  static const Color gray50 = Color(0xFFF5F5F5);
  static const Color gray100 = Color(0xFFE0E0E0);
  static const Color gray500 = Color(0xFF9E9E9E);
  static const Color gray700 = Color(0xFF4D4D4D);
  static const Color gray900 = Color(0xFF1A1A1A);

  // Orange
  static const Color orange50 = Color(0xFFFFF8F0);
  static const Color orange300 = Color(0xFFE8A86B);
  static const Color orange500 = Color(0xFFE8873B);
  static const Color orange600 = Color(0xFFD07730);

  // Green
  static const Color green500 = Color(0xFF00B852);

  // Red
  static const Color red500 = Color(0xFFE53535);

  // ── Semantic ──

  // Background
  static const Color bgPrimary = gray50;
  static const Color bgSurface = white;
  static const Color bgAccent = orange50;

  // Text
  static const Color textPrimary = gray900;
  static const Color textSecondary = gray700;
  static const Color textTertiary = gray500;
  static const Color textAccent = orange500;

  // Border
  static const Color borderDefault = gray100;

  // Button
  static const Color buttonPrimary = orange500;
  static const Color buttonPressed = orange600;

  // Status
  static const Color statusSuccess = green500;
  static const Color statusError = red500;
}
