import 'package:flutter/material.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class BankIcon extends StatelessWidget {
  final String bankName;
  final double size;

  const BankIcon({
    super.key,
    required this.bankName,
    this.size = 40,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: _bankColor,
        borderRadius: BorderRadius.circular(size / 4),
      ),
      alignment: Alignment.center,
      child: Text(
        bankName.substring(0, 1),
        style: AppTextStyles.bodySm.copyWith(
          color: AppColors.white,
          fontWeight: FontWeight.w700,
          fontSize: size * 0.4,
        ),
      ),
    );
  }

  Color get _bankColor {
    switch (bankName) {
      case '카카오뱅크':
        return const Color(0xFFFEE500);
      case '토스뱅크':
        return const Color(0xFF3182F6);
      case '신한은행':
        return const Color(0xFF0046FF);
      case '국민은행':
        return const Color(0xFFFFBC00);
      case 'NH농협은행':
        return const Color(0xFF02A862);
      case '우리은행':
        return const Color(0xFF0066B3);
      case '하나은행':
        return const Color(0xFF009B8D);
      case '기업은행':
        return const Color(0xFF005BAC);
      default:
        return AppColors.gray500;
    }
  }
}
