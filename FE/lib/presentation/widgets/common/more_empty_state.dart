import 'package:flutter/material.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreEmptyState extends StatelessWidget {
  final String message;
  final String? buttonText;
  final VoidCallback? onTap;
  final IconData icon;

  const MoreEmptyState({
    super.key,
    required this.message,
    this.buttonText,
    this.onTap,
    this.icon = Icons.add_circle_outline,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 40, horizontal: 24),
        decoration: BoxDecoration(
          color: AppColors.bgSurface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.borderDefault),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 48, color: AppColors.textTertiary),
            const SizedBox(height: 12),
            Text(
              message,
              style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary),
              textAlign: TextAlign.center,
            ),
            if (buttonText != null) ...[
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                decoration: BoxDecoration(
                  color: AppColors.orange50,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  buttonText!,
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.orange500,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
