import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
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
              context.push(AppRoutes.goalPriority);
            },
          ),
        ],
      ),
      body: goalState.isLoading
          ? const Center(child: CircularProgressIndicator())
          : goalState.errorMessage != null
          ? Center(
              child: Padding(
                padding: EdgeInsets.all(24.w),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text('목표 목록을 불러오지 못했습니다.', style: AppTextStyles.bodyLg),
                    SizedBox(height: 8.h),
                    Text(
                      goalState.errorMessage!,
                      textAlign: TextAlign.center,
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                    SizedBox(height: 16.h),
                    FilledButton(
                      onPressed: viewModel.loadGoals,
                      child: const Text('다시 시도'),
                    ),
                  ],
                ),
              ),
            )
          : Column(
              children: [
                Padding(
                  padding: EdgeInsets.symmetric(
                    horizontal: 16.w,
                    vertical: 12.h,
                  ),
                  child: Row(
                    children: [
                      MoreChip(
                        label: '진행중',
                        selected: !goalState.showAchieved,
                        onTap: () {
                          viewModel.toggleFilter(false);
                        },
                      ),
                      SizedBox(width: 8.w),
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
                                size: 64.w,
                                color: AppColors.textTertiary.withValues(
                                  alpha: 0.5,
                                ),
                              ),
                              SizedBox(height: 16.h),
                              Text(
                                goalState.showAchieved
                                    ? '아직 달성한 목표가 없어요'
                                    : '진행 중인 목표가 없어요',
                                style: AppTextStyles.bodyLg.copyWith(
                                  color: AppColors.textTertiary,
                                ),
                              ),
                              SizedBox(height: 8.h),
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
                          padding: EdgeInsets.symmetric(
                            horizontal: 16.w,
                            vertical: 8.h,
                          ),
                          itemCount: filteredGoals.length,
                          separatorBuilder: (context, index) =>
                              SizedBox(height: 12.h),
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
          context.push(AppRoutes.goalCreate);
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
