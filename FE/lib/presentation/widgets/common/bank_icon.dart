import 'package:flutter/material.dart';
import 'package:more_app/core/constants/bank_catalog.dart';
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
    final colors = AppColors.of(context);
    final bank = BankCatalog.findByName(bankName);
    final logoPath = bank?.logoPath;

    if (logoPath != null && logoPath.isNotEmpty) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(size / 4),
        child: SizedBox(
          width: size,
          height: size,
          child: Image.asset(
            logoPath,
            fit: BoxFit.cover,
            errorBuilder: (context, error, stackTrace) =>
                _buildFallback(colors),
          ),
        ),
      );
    }

    return _buildFallback(colors);
  }

  Widget _buildFallback(AppColorScheme colors) {
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
          color: colors.white,
          fontWeight: FontWeight.w700,
          fontSize: size * 0.4,
        ),
      ),
    );
  }

  Color get _bankColor {
    switch (BankCatalog.findByName(bankName)?.name ?? bankName) {
      case '카카오뱅크':
        return const Color(0xFFFEE500);
      case '신한은행':
        return const Color(0xFF0046FF);
      case '국민은행':
        return const Color(0xFFFFBC00);
      case '농협은행':
        return const Color(0xFF02A862);
      case '우리은행':
        return const Color(0xFF0066B3);
      case '하나은행':
        return const Color(0xFF009B8D);
      case '기업은행':
        return const Color(0xFF005BAC);
      case 'iM뱅크':
        return const Color(0xFF0054A6);
      case '씨티은행':
        return const Color(0xFF1F5DB6);
      case '산업은행':
        return const Color(0xFF0054A6);
      default:
        return Colors.grey;
    }
  }
}
