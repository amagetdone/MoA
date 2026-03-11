import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class GoalCard extends StatelessWidget {
  final Goal goal;
  final VoidCallback? onTap;

  const GoalCard({super.key, required this.goal, this.onTap});

  IconData _mapIcon(String? iconName) {
    switch (iconName) {
      case 'travel':
        return Icons.flight;
      case 'car':
        return Icons.directions_car;
      case 'home':
        return Icons.home;
      case 'education':
        return Icons.school;
      case 'wedding':
        return Icons.favorite;
      case 'health':
        return Icons.fitness_center;
      case 'shopping':
        return Icons.shopping_bag;
      case 'laptop':
        return Icons.laptop_mac;
      case 'emergency':
        return Icons.shield;
      case 'investment':
        return Icons.trending_up;
      case 'gift':
        return Icons.card_giftcard;
      default:
        return Icons.savings;
    }
  }

  @override
  Widget build(BuildContext context) {
    return MoreCard(
      onTap:
          onTap ??
          () {
            context.push(AppRoutes.goalDetailLocation(goal.id));
          },
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 44.w,
                height: 44.w,
                decoration: BoxDecoration(
                  color: AppColors.orange50,
                  borderRadius: BorderRadius.circular(12.r),
                ),
                child: Icon(
                  _mapIcon(goal.iconName),
                  color: AppColors.orange500,
                  size: 24.w,
                ),
              ),
              SizedBox(width: 12.w),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      goal.name,
                      style: AppTextStyles.bodyLg.copyWith(
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    SizedBox(height: 2.h),
                    Text(
                      goal.remainingDays > 0
                          ? 'D-${goal.remainingDays}'
                          : '기한 만료',
                      style: AppTextStyles.bodySm.copyWith(
                        color: goal.remainingDays > 0
                            ? AppColors.textTertiary
                            : AppColors.red500,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          SizedBox(height: 16.h),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                CurrencyFormatter.formatKRW(goal.currentAmount),
                style: AppTextStyles.bodyMd.copyWith(
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary,
                ),
              ),
              Text(
                CurrencyFormatter.formatKRW(goal.targetAmount),
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
            ],
          ),
          SizedBox(height: 8.h),
          MoreProgressBar(
            progress: goal.progressRate,
            color: AppColors.orange500,
            backgroundColor: AppColors.gray50,
            height: 8,
          ),
          SizedBox(height: 6.h),
          Align(
            alignment: Alignment.centerRight,
            child: Text(
              '${(goal.progressRate * 100).toInt()}% 달성',
              style: AppTextStyles.caption.copyWith(color: AppColors.orange500),
            ),
          ),
        ],
      ),
    );
  }
}
