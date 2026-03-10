import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_chip.dart';
import 'package:more_app/presentation/pages/goal/widgets/goal_card.dart';

class GoalListPage extends ConsumerWidget {
  const GoalListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final goalState = ref.watch(goalViewModelProvider);
    final viewModel = ref.read(goalViewModelProvider.notifier);
    final filteredGoals = goalState.filteredGoals;

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '목표',
        showBack: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.sort),
            onPressed: () {
              context.push('/goal/priority');
            },
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                MoreChip(
                  label: '진행중',
                  selected: !goalState.showAchieved,
                  onTap: () {
                    viewModel.toggleFilter(false);
                  },
                ),
                const SizedBox(width: 8),
                MoreChip(
                  label: '달성한',
                  selected: goalState.showAchieved,
                  onTap: () {
                    viewModel.toggleFilter(true);
                  },
                ),
              ],
            ),
          ),
          Expanded(
            child: filteredGoals.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.flag_outlined,
                          size: 64,
                          color: AppColors.textTertiary.withOpacity(0.5),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          goalState.showAchieved
                              ? '아직 달성한 목표가 없어요'
                              : '진행 중인 목표가 없어요',
                          style: AppTextStyles.bodyLg.copyWith(
                            color: AppColors.textTertiary,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          '새로운 목표를 추가해보세요!',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textTertiary,
                          ),
                        ),
                      ],
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    itemCount: filteredGoals.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final goal = filteredGoals[index];
                      return GoalCard(goal: goal);
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: AppColors.orange500,
        foregroundColor: Colors.white,
        onPressed: () {
          context.push('/goal/create');
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
