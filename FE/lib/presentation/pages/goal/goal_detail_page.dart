import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/pages/goal/widgets/goal_charts.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/goal_video_thumb.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class GoalDetailPage extends ConsumerStatefulWidget {
  const GoalDetailPage({super.key, required this.goalId});

  final String goalId;

  @override
  ConsumerState<GoalDetailPage> createState() => _GoalDetailPageState();
}

class _GoalDetailPageState extends ConsumerState<GoalDetailPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(goalViewModelProvider.notifier).loadGoalDetail(widget.goalId);
    });
  }

  @override
  Widget build(BuildContext context) {
    final goalState = ref.watch(goalViewModelProvider);
    final goal = goalState.findGoalById(widget.goalId);
    final isDetailLoading = goalState.isDetailLoading(widget.goalId);
    final history = goalState.savingsHistoryFor(widget.goalId);

    if (goalState.isLoading || (goal == null && isDetailLoading)) {
      return const Scaffold(
        backgroundColor: AppColors.bgPrimary,
        body: Center(child: CircularProgressIndicator()),
      );
    }

    if (goal == null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: AppBar(backgroundColor: AppColors.bgPrimary, elevation: 0),
        body: const Center(child: Text('목표를 찾을 수 없습니다.')),
      );
    }

    final currentWeek = goal.currentWeekNumber.clamp(
      1,
      goal.totalWeeks > 0 ? goal.totalWeeks : goal.currentWeekNumber,
    );

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: EdgeInsets.fromLTRB(4.w, 4.h, 12.w, 0),
              child: Row(
                children: [
                  IconButton(
                    icon: Icon(
                      Icons.chevron_left,
                      size: 28.r,
                      color: AppColors.textPrimary,
                    ),
                    onPressed: () => context.pop(),
                  ),
                  Expanded(
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.baseline,
                      textBaseline: TextBaseline.alphabetic,
                      children: [
                        Flexible(
                          child: Text(
                            goal.name,
                            style: AppTextStyles.headingMd.copyWith(
                              fontWeight: FontWeight.w800,
                              color: AppColors.textPrimary,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        SizedBox(width: 8.w),
                        Text(
                          '$currentWeek주차 / 총 ${goal.totalWeeks}주',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textTertiary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Icon(
                    Icons.notifications_outlined,
                    size: 24.r,
                    color: AppColors.textPrimary,
                  ),
                ],
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: EdgeInsets.fromLTRB(16.w, 12.h, 16.w, 28.h),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _HeroCard(
                      goal: goal,
                      showVideoLoading: isDetailLoading && goal.sketchVideoAssetPath == null,
                    ),
                    SizedBox(height: 14.h),
                    if (goal.description != null && goal.description!.isNotEmpty) ...[
                      MoreCard(
                        padding: EdgeInsets.all(18.w),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '목표 설명',
                              style: AppTextStyles.bodyMd.copyWith(
                                fontWeight: FontWeight.w700,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            SizedBox(height: 6.h),
                            Text(
                              goal.description!,
                              style: AppTextStyles.bodyMd.copyWith(
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      SizedBox(height: 14.h),
                    ],
                    GoalLineChart(history: history, goal: goal),
                    SizedBox(height: 14.h),
                    GoalBarChart(history: history, goal: goal),
                    if (goal.weeklyMission != null || goal.weeklyBudget != null) ...[
                      SizedBox(height: 14.h),
                      _WeeklySummaryCard(goal: goal),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HeroCard extends StatelessWidget {
  const _HeroCard({
    required this.goal,
    this.showVideoLoading = false,
  });

  final Goal goal;
  final bool showVideoLoading;

  @override
  Widget build(BuildContext context) {
    final remaining = CurrencyFormatter.formatKRW(goal.remainingAmount);
    final current = CurrencyFormatter.formatKRW(goal.currentAmount);
    final target = CurrencyFormatter.formatKRW(goal.targetAmount);

    return MoreCard(
      padding: EdgeInsets.zero,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              ClipRRect(
                borderRadius: BorderRadius.vertical(top: Radius.circular(16.r)),
                child: GoalVideoThumb(
                  key: ValueKey('detail_${goal.id}'),
                  goalId: goal.id,
                  assetPath: goal.sketchVideoAssetPath,
                  iconName: goal.iconName,
                  progressRate: goal.progressRate,
                  size: 200.w,
                  borderRadius: 0,
                  fillWidth: true,
                  playAnimation: true,
                  forceLoading: showVideoLoading,
                ),
              ),
              Positioned(
                top: 12.h,
                left: 14.w,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _Badge(
                      label: '우선순위 ${goal.priority}',
                      bgColor: AppColors.orange500,
                      textColor: Colors.white,
                    ),
                    SizedBox(height: 6.h),
                    _Badge(
                      label: goal.remainingDays >= 0
                          ? 'D-${goal.remainingDays}'
                          : 'D+${-goal.remainingDays}',
                      bgColor: Colors.black.withValues(alpha: 0.35),
                      textColor: goal.remainingDays < 0
                          ? AppColors.red500
                          : Colors.white,
                    ),
                  ],
                ),
              ),
            ],
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(18.w, 18.h, 18.w, 18.h),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 38.w,
                      height: 38.w,
                      decoration: BoxDecoration(
                        color: AppColors.green500.withValues(alpha: 0.12),
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: AppColors.green500.withValues(alpha: 0.3),
                        ),
                      ),
                      child: Icon(
                        Icons.savings_outlined,
                        color: AppColors.green500,
                        size: 20.w,
                      ),
                    ),
                    SizedBox(width: 12.w),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            crossAxisAlignment: CrossAxisAlignment.baseline,
                            textBaseline: TextBaseline.alphabetic,
                            children: [
                              Text(
                                current,
                                style: AppTextStyles.headingMd.copyWith(
                                  fontWeight: FontWeight.w800,
                                  color: AppColors.green500,
                                ),
                              ),
                              Text(
                                '총 $target',
                                style: AppTextStyles.bodySm.copyWith(
                                  color: AppColors.textTertiary,
                                ),
                              ),
                            ],
                          ),
                          SizedBox(height: 2.h),
                          Text(
                            '현재 저축액',
                            style: AppTextStyles.bodySm.copyWith(
                              color: AppColors.textTertiary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                SizedBox(height: 14.h),
                MoreProgressBar(
                  progress: goal.progressRate,
                  color: AppColors.green500,
                  backgroundColor: AppColors.gray500.withValues(alpha: 0.12),
                  height: 8.h,
                ),
                SizedBox(height: 8.h),
                Text(
                  '목표까지 $remaining 남음',
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _WeeklySummaryCard extends StatelessWidget {
  const _WeeklySummaryCard({required this.goal});

  final Goal goal;

  @override
  Widget build(BuildContext context) {
    final weeklyMission = goal.weeklyMission;
    final weeklyBudget = goal.weeklyBudget;

    return MoreCard(
      padding: EdgeInsets.all(18.w),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '주간 계획 요약',
            style: AppTextStyles.bodyMd.copyWith(
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          SizedBox(height: 12.h),
          if (weeklyMission != null) ...[
            _SummaryRow(
              label: '주간 미션 저축액',
              value: CurrencyFormatter.formatKRW(weeklyMission.weeklySavingsAmount),
            ),
            _SummaryRow(
              label: '목표까지 남은 주차',
              value: '${weeklyMission.weeksToGoal}주',
            ),
          ],
          if (weeklyBudget != null) ...[
            _SummaryRow(
              label: '주간 총수입',
              value: CurrencyFormatter.formatKRW(weeklyBudget.totalIncomeAmount),
            ),
            _SummaryRow(
              label: '주간 저축액',
              value: CurrencyFormatter.formatKRW(weeklyBudget.savingAmount),
            ),
            _SummaryRow(
              label: '자유 금액',
              value: CurrencyFormatter.formatKRW(weeklyBudget.freeAmount),
            ),
          ],
        ],
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: 8.h),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary),
          ),
          Text(
            value,
            style: AppTextStyles.bodyMd.copyWith(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({
    required this.label,
    required this.bgColor,
    required this.textColor,
  });

  final String label;
  final Color bgColor;
  final Color textColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 4.h),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(999.r),
      ),
      child: Text(
        label,
        style: AppTextStyles.caption.copyWith(
          color: textColor,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
