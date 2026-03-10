import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';
import 'package:more_app/presentation/pages/goal/widgets/goal_charts.dart';

class GoalDetailPage extends ConsumerWidget {
  final String goalId;

  const GoalDetailPage({
    super.key,
    required this.goalId,
  });

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
  Widget build(BuildContext context, WidgetRef ref) {
    final goalState = ref.watch(goalViewModelProvider);
    final goal = goalState.goals.cast<Goal?>().firstWhere(
          (g) => g?.id == goalId,
          orElse: () => null,
        );

    if (goal == null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: const MoreAppBar(title: '목표 상세'),
        body: const Center(
          child: Text('목표를 찾을 수 없습니다.'),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(title: goal.name),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // Priority badge and D-day
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.orange50,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    '우선순위 ${goal.priority}',
                    style: AppTextStyles.caption.copyWith(
                      color: AppColors.orange500,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: goal.remainingDays > 0
                        ? AppColors.orange50
                        : const Color(0xFFFFEBEE),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    goal.remainingDays > 0
                        ? 'D-${goal.remainingDays}'
                        : '기한 만료',
                    style: AppTextStyles.caption.copyWith(
                      color: goal.remainingDays > 0
                          ? AppColors.orange500
                          : AppColors.red500,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Goal icon
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: AppColors.orange50,
                borderRadius: BorderRadius.circular(24),
              ),
              child: Icon(
                _mapIcon(goal.iconName),
                color: AppColors.orange500,
                size: 40,
              ),
            ),
            const SizedBox(height: 24),

            // Current savings amount
            Text(
              CurrencyFormatter.formatKRW(goal.currentAmount),
              style: AppTextStyles.headingXl.copyWith(
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),

            // Target amount
            Text(
              '목표 ${CurrencyFormatter.formatKRW(goal.targetAmount)}',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
            const SizedBox(height: 24),

            // Progress bar section
            MoreCard(
              child: Column(
                children: [
                  MoreProgressBar(
                    progress: goal.progressRate,
                    color: AppColors.orange500,
                    backgroundColor: AppColors.gray50,
                    height: 12,
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        '${(goal.progressRate * 100).toInt()}% 달성',
                        style: AppTextStyles.bodyMd.copyWith(
                          color: AppColors.orange500,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        '남은 금액: ${CurrencyFormatter.formatKRW(goal.remainingAmount)}',
                        style: AppTextStyles.bodySm.copyWith(
                          color: AppColors.textTertiary,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Goal description
            if (goal.description != null && goal.description!.isNotEmpty) ...[
              MoreCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '목표 설명',
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      goal.description!,
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ],

            // Line chart - achievement trend
            const GoalLineChart(),
            const SizedBox(height: 16),

            // Bar chart - savings history
            const GoalBarChart(),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}
