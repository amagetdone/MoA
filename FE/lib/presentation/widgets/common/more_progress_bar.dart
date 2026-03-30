import 'package:flutter/material.dart';
import 'package:more_app/core/theme/app_colors.dart';

class MoreProgressBar extends StatelessWidget {
  final double progress;
  final Color? color;
  final Color? backgroundColor;
  final double height;

  const MoreProgressBar({
    super.key,
    required this.progress,
    this.color,
    this.backgroundColor,
    this.height = 8,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Container(
      width: double.infinity,
      height: height,
      decoration: BoxDecoration(
        color: backgroundColor ?? colors.borderDefault,
        borderRadius: BorderRadius.circular(height / 2),
      ),
      child: FractionallySizedBox(
        alignment: Alignment.centerLeft,
        widthFactor: progress.clamp(0.0, 1.0),
        child: Container(
          decoration: BoxDecoration(
            color: color ?? AppColors.orange500,
            borderRadius: BorderRadius.circular(height / 2),
          ),
        ),
      ),
    );
  }
}
