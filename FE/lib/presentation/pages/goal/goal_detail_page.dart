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

class GoalDetailPage extends ConsumerWidget {
  final String goalId;
  const GoalDetailPage({super.key, required this.goalId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final goalState = ref.watch(goalViewModelProvider);

    if (goalState.isLoading) {
      return const Scaffold(
        backgroundColor: AppColors.bgPrimary,
        body: Center(child: CircularProgressIndicator()),
      );
    }

    final goal = goalState.findGoalById(goalId);
    if (goal == null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: AppBar(backgroundColor: AppColors.bgPrimary, elevation: 0),
        body: const Center(child: Text('목표를 찾을 수 없습니다.')),
      );
    }

    final history = goalState.savingsHistoryFor(goalId);

    // 주차 계산
    final totalWeeks =
        goal.endDate.difference(goal.startDate).inDays ~/ 7;
    final currentWeek =
        (DateTime.now().difference(goal.startDate).inDays ~/ 7 + 1)
            .clamp(1, totalWeeks > 0 ? totalWeeks : 1);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: SafeArea(
        child: Column(
          children: [
            // ── 커스텀 헤더 ──────────────────────────────────────
            Padding(
              padding: EdgeInsets.fromLTRB(4.w, 4.h, 12.w, 0),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
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
                          '${currentWeek}주차 진행중 / 총 ${totalWeeks}주',
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

            // ── 스크롤 바디 ──────────────────────────────────────
            Expanded(
              child: SingleChildScrollView(
                padding: EdgeInsets.fromLTRB(16.w, 12.h, 16.w, 28.h),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // ── 히어로 카드 (영상 + 저축 정보) ────────────
                    _HeroCard(goal: goal),

                    SizedBox(height: 14.h),

                    // ── 목표 설명 ─────────────────────────────────
                    if (goal.description != null &&
                        goal.description!.isNotEmpty) ...[
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

                    // ── 달성 현황 차트 ────────────────────────────
                    GoalLineChart(history: history, goal: goal),
                    SizedBox(height: 14.h),

                    // ── 저축 이력 차트 ────────────────────────────
                    GoalBarChart(history: history, goal: goal),
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

// ─── 히어로 카드 ────────────────────────────────────────────
class _HeroCard extends StatelessWidget {
  const _HeroCard({required this.goal});
  final Goal goal;

  @override
  Widget build(BuildContext context) {
    final remaining = CurrencyFormatter.formatKRW(goal.remainingAmount);
    final current  = CurrencyFormatter.formatKRW(goal.currentAmount);
    final target   = CurrencyFormatter.formatKRW(goal.targetAmount);

    return MoreCard(
      padding: EdgeInsets.zero,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // ── 영상 히어로 + 배지 오버레이 ────────────────────
          Stack(
            clipBehavior: Clip.none,
            children: [
              // 영상: 카드 상단 둥근 모서리 clip (full-width 히어로)
              ClipRRect(
                borderRadius: BorderRadius.vertical(top: Radius.circular(16.r)),
                child: GoalVideoThumb(
                  assetPath: goal.sketchVideoAssetPath,
                  iconName: goal.iconName,
                  progressRate: goal.progressRate,
                  size: 200.w,   // height 기준
                  borderRadius: 0,
                  fillWidth: true,
                ),
              ),

              // 배지 (좌상단 오버레이)
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

          // ── 저축 정보 ────────────────────────────────────
          Padding(
            padding: EdgeInsets.fromLTRB(18.w, 18.h, 18.w, 18.h),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 아이콘 + 현재 저축액 / 총 목표
                Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
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

                // 프로그레스 바
                MoreProgressBar(
                  progress: goal.progressRate,
                  color: AppColors.green500,
                  backgroundColor: AppColors.gray500.withValues(alpha: 0.12),
                  height: 8.h,
                ),

                SizedBox(height: 8.h),

                // 남은 금액 (좌측 정렬, 피그마 기준)
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

// ─── 배지 ────────────────────────────────────────────────────
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
