import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/pages/goal/widgets/goal_card.dart';

class GoalListPage extends ConsumerWidget {
  const GoalListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final goalState = ref.watch(goalViewModelProvider);
    final viewModel = ref.read(goalViewModelProvider.notifier);
    final filteredGoals = goalState.filteredGoals;
    final totalCount = goalState.goals.length;

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: goalState.isLoading
          ? const Center(child: CircularProgressIndicator())
          : goalState.errorMessage != null
              ? _buildError(context, goalState.errorMessage!, viewModel)
              : SafeArea(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // ── 상단 내비 바 ──────────────────────────────────
                      Padding(
                        padding: EdgeInsets.symmetric(
                          horizontal: 4.w,
                          vertical: 4.h,
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            // 뒤로가기 (탭에서는 숨김, push로 들어올 때만 표시)
                            if (Navigator.of(context).canPop())
                              IconButton(
                                icon: Icon(
                                  Icons.chevron_left,
                                  size: 28.r,
                                  color: AppColors.textPrimary,
                                ),
                                onPressed: () => context.pop(),
                              )
                            else
                              SizedBox(width: 48.w),
                            // 알림
                            IconButton(
                              icon: Icon(
                                Icons.notifications_outlined,
                                size: 24.r,
                                color: AppColors.textPrimary,
                              ),
                              onPressed: () {},
                            ),
                          ],
                        ),
                      ),

                      // ── 타이틀 + 서브타이틀 ─────────────────────────
                      Padding(
                        padding: EdgeInsets.fromLTRB(24.w, 4.h, 24.w, 0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '나의 목표',
                              style: AppTextStyles.headingLg.copyWith(
                                fontWeight: FontWeight.w800,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            SizedBox(height: 4.h),
                            Text(
                              '총 $totalCount개의 목표를 계획했어요.',
                              style: AppTextStyles.bodySm.copyWith(
                                color: AppColors.textTertiary,
                              ),
                            ),
                          ],
                        ),
                      ),

                      SizedBox(height: 16.h),

                      // ── 필터 칩 + 우선순위 설정 ──────────────────────
                      Padding(
                        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 0),
                        child: Row(
                          children: [
                            _FilterChip(
                              label: '진행중인 목표',
                              selected: !goalState.showAchieved,
                              onTap: () => viewModel.toggleFilter(false),
                            ),
                            SizedBox(width: 8.w),
                            _FilterChip(
                              label: '달성한 목표',
                              selected: goalState.showAchieved,
                              onTap: () => viewModel.toggleFilter(true),
                            ),
                            const Spacer(),
                            GestureDetector(
                              onTap: () => context.push(AppRoutes.goalPriority),
                              child: Text(
                                '목표 우선순위 설정 >',
                                style: AppTextStyles.bodySm.copyWith(
                                  color: AppColors.textTertiary,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),

                      SizedBox(height: 12.h),

                      // ── 목표 리스트 ────────────────────────────────
                      Expanded(
                        child: filteredGoals.isEmpty
                            ? _buildEmpty(goalState.showAchieved)
                            : ListView.separated(
                                padding: EdgeInsets.fromLTRB(
                                  24.w,
                                  4.h,
                                  24.w,
                                  28.h,
                                ),
                                itemCount: filteredGoals.length,
                                separatorBuilder: (_, __) =>
                                    SizedBox(height: 12.h),
                                itemBuilder: (context, index) =>
                                    GoalCard(goal: filteredGoals[index]),
                              ),
                      ),
                    ],
                  ),
                ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: AppColors.orange500,
        foregroundColor: Colors.white,
        onPressed: () => context.push(AppRoutes.goalCreate),
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildError(
    BuildContext context,
    String message,
    GoalViewModel viewModel,
  ) {
    return Center(
      child: Padding(
        padding: EdgeInsets.all(24.w),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('목표 목록을 불러오지 못했습니다.', style: AppTextStyles.bodyLg),
            SizedBox(height: 8.h),
            Text(
              message,
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
    );
  }

  Widget _buildEmpty(bool showAchieved) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.flag_outlined,
            size: 64.w,
            color: AppColors.textTertiary.withValues(alpha: 0.5),
          ),
          SizedBox(height: 16.h),
          Text(
            showAchieved ? '아직 달성한 목표가 없어요' : '진행 중인 목표가 없어요',
            style: AppTextStyles.bodyLg.copyWith(color: AppColors.textTertiary),
          ),
          SizedBox(height: 8.h),
          Text(
            '새로운 목표를 추가해보세요!',
            style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary),
          ),
        ],
      ),
    );
  }
}

// ── 필터 칩 ────────────────────────────────────────────────
class _FilterChip extends StatelessWidget {
  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
        decoration: BoxDecoration(
          color: selected ? AppColors.orange500 : Colors.white,
          borderRadius: BorderRadius.circular(999.r),
          border: Border.all(
            color: selected ? AppColors.orange500 : AppColors.borderDefault,
          ),
          boxShadow: selected
              ? [
                  BoxShadow(
                    color: AppColors.orange500.withValues(alpha: 0.25),
                    blurRadius: 6,
                    offset: const Offset(0, 2),
                  ),
                ]
              : null,
        ),
        child: Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: selected ? Colors.white : AppColors.textTertiary,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}
