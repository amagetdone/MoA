import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/goal_video_thumb.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class GoalCard extends ConsumerWidget {
  final Goal goal;
  final VoidCallback? onTap;

  const GoalCard({super.key, required this.goal, this.onTap});

  /// startDate 기준 현재 몇 주차인지 계산
  int get _currentWeek {
    return goal.currentWeekNumber;
  }

  String get _weekLabel {
    final d = goal.remainingDays;
    final dLabel = d >= 0 ? 'D-$d' : 'D+${-d}';
    return '$_currentWeek주차 ($dLabel)';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final percent = (goal.progressRate * 100).toInt().toString();
    final isExpired = goal.remainingDays < 0;

    return MoreCard(
      onTap: onTap ?? () => context.push(AppRoutes.goalDetailLocation(goal.id)),
      padding: EdgeInsets.fromLTRB(16.w, 10.w, 12.w, 16.w),
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // ── 영상 썸네일 (흰 배경 + 테두리 카드형) – 카드 세로 중앙 정렬 ──
            Center(
              child: Container(
                width: 72.w,
                height: 72.w,
                decoration: BoxDecoration(
                  color: colors.bgSurface,
                  borderRadius: BorderRadius.circular(16.r),
                  border: Border.all(color: colors.borderDefault, width: 1),
                  boxShadow: [
                    BoxShadow(
                      color: colors.cardShadow,
                      blurRadius: 6,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(15.r),
                  child: GoalVideoThumb(
                    key: ValueKey('card_${goal.id}'),
                    goalId: goal.id,
                    assetPath: goal.isAiPending || goal.isAiFailed
                        ? null
                        : goal.sketchVideoAssetPath,
                    iconName: goal.iconName,
                    progressRate: goal.progressRate,
                    size: 72.w,
                    borderRadius: 0,
                    forceLoading: goal.isAiPending,
                  ),
                ),
              ),
            ),

            SizedBox(width: 14.w),

            // ── 우측 콘텐츠 ──
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // 주차 + … 메뉴 (line height 압축)
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Expanded(
                        child: Text(
                          _weekLabel,
                          style: AppTextStyles.bodySm.copyWith(
                            color: isExpired
                                ? AppColors.red500
                                : colors.textTertiary,
                            fontWeight: FontWeight.w500,
                            height: 1.1,
                          ),
                        ),
                      ),
                      if (goal.isAiPending || goal.isAiFailed) ...[
                        SizedBox(width: 6.w),
                        _GoalAiStatusChip(goal: goal),
                      ],
                      // 터치 타겟 패딩 제거
                      SizedBox(
                        width: 28.w,
                        height: 28.w,
                        child: _KebabMenu(goal: goal),
                      ),
                    ],
                  ),

                  SizedBox(height: 1.h),

                  // 목표명 (line height 압축)
                  Text(
                    goal.name,
                    style: AppTextStyles.bodyLg.copyWith(
                      fontWeight: FontWeight.w700,
                      color: colors.textPrimary,
                      height: 1.15,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),

                  SizedBox(height: 7.h),

                  // 금액 + 달성률
                  Text(
                    '${CurrencyFormatter.formatKRW(goal.currentAmount)} / ${CurrencyFormatter.formatKRW(goal.targetAmount)} ($percent%)',
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textTertiary,
                      fontWeight: FontWeight.w500,
                      height: 1.1,
                    ),
                  ),

                  SizedBox(height: 5.h),

                  // 프로그레스 바
                  MoreProgressBar(
                    progress: goal.progressRate,
                    color: AppColors.orange500,
                    backgroundColor: colors.gray500.withValues(alpha: 0.15),
                    height: 6.h,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _GoalAiStatusChip extends StatelessWidget {
  const _GoalAiStatusChip({required this.goal});

  final Goal goal;

  @override
  Widget build(BuildContext context) {
    final isPending = goal.isAiPending;
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 4.h),
      decoration: BoxDecoration(
        color: isPending
            ? AppColors.orange500.withValues(alpha: 0.12)
            : AppColors.red500.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999.r),
      ),
      child: Text(
        isPending ? '생성 중' : '재시도 필요',
        style: AppTextStyles.caption.copyWith(
          color: isPending ? AppColors.orange500 : AppColors.red500,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

// ── 케밥(…) 메뉴 ──────────────────────────────────────────
class _KebabMenu extends ConsumerWidget {
  const _KebabMenu({required this.goal});
  final Goal goal;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    return PopupMenuButton<String>(
      icon: Icon(Icons.more_horiz, size: 18.w, color: colors.textTertiary),
      padding: EdgeInsets.zero,
      iconSize: 18.w,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12.r)),
      elevation: 4,
      onSelected: (value) async {
        if (value == 'edit') {
          context.push(AppRoutes.goalEditLocation(goal.id));
        } else if (value == 'delete') {
          final confirmed = await showDialog<bool>(
            context: context,
            builder: (ctx) => AlertDialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16.r),
              ),
              title: const Text('목표를 삭제할까요?'),
              content: Text(
                '${goal.name} 목표를 삭제하면 복구할 수 없어요.\n정말 삭제할까요?',
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(ctx, false),
                  child: const Text('취소'),
                ),
                TextButton(
                  onPressed: () => Navigator.pop(ctx, true),
                  child: Text('삭제', style: TextStyle(color: AppColors.red500)),
                ),
              ],
            ),
          );
          if (confirmed == true) {
            await ref.read(goalViewModelProvider.notifier).removeGoal(goal.id);
          }
        }
      },
      itemBuilder: (_) => [
        PopupMenuItem(
          value: 'edit',
          child: Row(
            children: [
              const Expanded(child: Text('수정')),
              Icon(
                Icons.edit_outlined,
                size: 18.w,
                color: colors.textTertiary,
              ),
            ],
          ),
        ),
        PopupMenuItem(
          value: 'delete',
          child: Row(
            children: [
              const Expanded(child: Text('삭제')),
              Icon(
                Icons.delete_outline,
                size: 18.w,
                color: colors.textTertiary,
              ),
            ],
          ),
        ),
      ],
    );
  }
}
