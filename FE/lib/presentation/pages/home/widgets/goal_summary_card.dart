import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class GoalSummaryCard extends StatelessWidget {
  final Goal goal;

  const GoalSummaryCard({super.key, required this.goal});

  IconData _getGoalIcon(String? iconName) {
    switch (iconName) {
      case 'travel':
        return Icons.flight;
      case 'car':
        return Icons.directions_car;
      case 'house':
        return Icons.home;
      case 'education':
        return Icons.school;
      case 'wedding':
        return Icons.favorite;
      case 'emergency':
        return Icons.shield;
      case 'shopping':
        return Icons.shopping_bag;
      case 'laptop':
        return Icons.laptop_mac;
      case 'gift':
        return Icons.card_giftcard;
      case 'health':
        return Icons.health_and_safety;
      case 'electronics':
        return Icons.devices;
      default:
        return Icons.savings;
    }
  }

  @override
  Widget build(BuildContext context) {
    final remainingDays = goal.remainingDays;
    final dDayText = remainingDays >= 0
        ? 'D-$remainingDays'
        : 'D+${-remainingDays}';

    return MoreCard(
      onTap: () => context.push(AppRoutes.goalDetailLocation(goal.id)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: AppColors.orange50,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(
                  _getGoalIcon(goal.iconName),
                  size: 20,
                  color: AppColors.orange500,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  goal.name,
                  style: AppTextStyles.bodyLg.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text.rich(
                TextSpan(
                  children: [
                    TextSpan(
                      text: CurrencyFormatter.formatKRW(goal.currentAmount),
                      style: AppTextStyles.bodyLg.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    TextSpan(
                      text:
                          ' / ${CurrencyFormatter.formatKRW(goal.targetAmount)}',
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          MoreProgressBar(progress: goal.progressRate),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: Text(
              dDayText,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
