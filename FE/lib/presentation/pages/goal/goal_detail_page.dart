import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/pages/goal/widgets/goal_charts.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class GoalDetailPage extends ConsumerWidget {
  final String goalId;

  const GoalDetailPage({super.key, required this.goalId});

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
  Widget build(BuildContext context, WidgetRef ref) {
    final goalState = ref.watch(goalViewModelProvider);

    if (goalState.isLoading) {
      return const Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: MoreAppBar(title: '목표 상세'),
        body: Center(child: CircularProgressIndicator()),
      );
    }

    final goal = goalState.findGoalById(goalId);
    if (goal == null) {
      return const Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: MoreAppBar(title: '목표 상세'),
        body: Center(child: Text('목표를 찾을 수 없습니다.')),
      );
    }

    final history = goalState.savingsHistoryFor(goalId);
    final sketchVideoAsync = ref.watch(goalSketchVideoProvider(goalId));

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(title: goal.name),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
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
            Text(
              CurrencyFormatter.formatKRW(goal.currentAmount),
              style: AppTextStyles.headingXl.copyWith(
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '목표 ${CurrencyFormatter.formatKRW(goal.targetAmount)}',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
            const SizedBox(height: 24),
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
            if (goal.sketchVideoAssetPath != null) ...[
              MoreCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(
                          Icons.play_circle_outline,
                          color: AppColors.orange500,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          '스케치 영상',
                          style: AppTextStyles.bodyMd.copyWith(
                            fontWeight: FontWeight.w600,
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'goalId 기준으로 로컬 저장소에 매핑된 영상을 사용합니다.',
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    sketchVideoAsync.when(
                      data: (bytes) {
                        final message = bytes == null
                            ? '저장된 영상이 없습니다.'
                            : '저장 크기: ${(bytes.lengthInBytes / 1024).toStringAsFixed(1)} KB';
                        return Text(
                          message,
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textSecondary,
                          ),
                        );
                      },
                      loading: () => const LinearProgressIndicator(
                        color: AppColors.orange500,
                      ),
                      error: (_, _) => Text(
                        '영상 로딩에 실패했습니다.',
                        style: AppTextStyles.bodySm.copyWith(
                          color: AppColors.red500,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ],
            GoalLineChart(history: history),
            const SizedBox(height: 16),
            GoalBarChart(history: history),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}
