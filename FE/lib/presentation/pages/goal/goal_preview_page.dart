import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/goal_video_thumb.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class GoalPreviewPage extends ConsumerStatefulWidget {
  const GoalPreviewPage({
    super.key,
    required this.goalId,
  });

  final String goalId;

  @override
  ConsumerState<GoalPreviewPage> createState() => _GoalPreviewPageState();
}

class _GoalPreviewPageState extends ConsumerState<GoalPreviewPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (widget.goalId.isEmpty) {
        context.go(AppRoutes.goalTab);
        return;
      }
      ref.read(goalViewModelProvider.notifier).loadGoalDetail(widget.goalId);
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final goalState = ref.watch(goalViewModelProvider);
    final goal = goalState.findGoalById(widget.goalId);
    final isDetailLoading = goalState.isDetailLoading(widget.goalId);

    if (goal == null && isDetailLoading) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(title: ''),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (goal == null) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(title: ''),
        body: Center(
          child: Text(
            '미리볼 목표를 찾지 못했습니다.',
            style: AppTextStyles.bodyMd.copyWith(color: colors.textSecondary),
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(
        title: '',
        showBack: false,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(24.w, 8.h, 24.w, 24.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              goal.isAiFailed ? '이미지 생성 실패' : '이미지 생성 완료',
              style: AppTextStyles.headingLg.copyWith(
                color: colors.textPrimary,
                fontSize: 24.sp,
                fontWeight: FontWeight.w800,
              ),
            ),
            SizedBox(height: 8.h),
            Text(
              goal.isAiFailed
                  ? '${goal.name} 목표 이미지를 아직 완성하지 못했어요.'
                  : '${goal.name} 목표 이미지가 완성되었어요.',
              style: AppTextStyles.bodyMd.copyWith(
                color: colors.textSecondary,
                height: 1.5,
              ),
            ),
            SizedBox(height: 24.h),
            MoreCard(
              padding: EdgeInsets.all(16.w),
              child: Column(
                children: [
                  GoalVideoThumb(
                    key: ValueKey('preview_${goal.id}'),
                    goalId: goal.id,
                    assetPath: goal.isAiSucceeded ? goal.sketchVideoAssetPath : null,
                    iconName: goal.iconName,
                    progressRate: 1,
                    size: 340.h,
                    fillWidth: true,
                    forceLoading: isDetailLoading && !goal.isAiFailed,
                  ),
                  SizedBox(height: 18.h),
                  Text(
                    goal.isAiFailed
                        ? '기본 스케치 영상을 보여주고 있습니다.'
                        : '완성된 이미지를 확인한 뒤 목표 상세에서 진행 현황을 볼 수 있어요.',
                    textAlign: TextAlign.center,
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textSecondary,
                      height: 1.45,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 20.h),
            TextButton(
              onPressed: () => context.go(AppRoutes.goalTab),
              child: Text(
                '목표 목록으로 돌아가기',
                style: AppTextStyles.bodySm.copyWith(
                  color: colors.textTertiary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: goal.isAiFailed ? '목표 상세에서 다시 시도하기' : '목표 상세 보기',
        onPressed: () => context.go(AppRoutes.goalDetailLocation(goal.id)),
      ),
    );
  }
}
