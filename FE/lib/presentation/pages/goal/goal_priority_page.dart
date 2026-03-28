import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class GoalPriorityPage extends ConsumerStatefulWidget {
  const GoalPriorityPage({super.key});

  @override
  ConsumerState<GoalPriorityPage> createState() => _GoalPriorityPageState();
}

class _GoalPriorityPageState extends ConsumerState<GoalPriorityPage> {
  List<Goal> _reorderedGoals = [];
  bool _hasChanges = false;

  Future<void> _savePriority(List<Goal> displayGoals) async {
    await ref.read(goalViewModelProvider.notifier).reorderGoals(displayGoals);
    await ref.read(homeViewModelProvider.notifier).loadData();
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('우선순위를 저장했어요. 변경 내용은 다음 주 저축 계획부터 반영됩니다.'),
      ),
    );
    context.pop();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final goalState = ref.watch(goalViewModelProvider);
    final displayGoals = _hasChanges
        ? _reorderedGoals
        : List<Goal>.from(goalState.inProgressGoals);

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(title: '목표 우선순위'),
      body: Column(
        children: [
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
            child: Container(
              width: double.infinity,
              padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 12.h),
              decoration: BoxDecoration(
                color: AppColors.orange50.withValues(alpha: 0.75),
                borderRadius: BorderRadius.circular(14.r),
                border: Border.all(
                  color: AppColors.orange300.withValues(alpha: 0.45),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '드래그하여 우선순위를 변경하세요',
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textPrimary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  SizedBox(height: 4.h),
                  Text(
                    '저장한 우선순위는 이번 주가 아니라 다음 주 저축 계획부터 적용돼요.',
                    style: AppTextStyles.caption.copyWith(
                      color: colors.textSecondary,
                      height: 1.4,
                    ),
                  ),
                ],
              ),
            ),
          ),
          Expanded(
            child: goalState.isLoading
                ? const SizedBox.shrink()
                : displayGoals.isEmpty
                ? Center(
                    child: Text(
                      '진행 중인 목표가 없습니다.',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: colors.textTertiary,
                      ),
                    ),
                  )
                : ReorderableListView.builder(
                    buildDefaultDragHandles: false,
                    padding: EdgeInsets.symmetric(horizontal: 16.w),
                    itemCount: displayGoals.length,
                    onReorder: (oldIndex, newIndex) {
                      setState(() {
                        if (!_hasChanges) {
                          _reorderedGoals = List<Goal>.from(displayGoals);
                        }
                        if (newIndex > oldIndex) {
                          newIndex -= 1;
                        }
                        final item = _reorderedGoals.removeAt(oldIndex);
                        _reorderedGoals.insert(newIndex, item);
                        _hasChanges = true;
                      });
                    },
                    proxyDecorator: (child, index, animation) {
                      return Material(
                        elevation: 4,
                        borderRadius: BorderRadius.circular(12.r),
                        shadowColor: Colors.black26,
                        child: child,
                      );
                    },
                    itemBuilder: (context, index) {
                      final goal = displayGoals[index];
                      return Padding(
                        key: ValueKey(goal.id),
                        padding: EdgeInsets.only(bottom: 12.h),
                        child: MoreCard(
                          child: Row(
                            children: [
                              Container(
                                width: 28.w,
                                height: 28.w,
                                decoration: BoxDecoration(
                                  color: index == 0
                                      ? AppColors.orange500
                                      : colors.gray100,
                                  borderRadius: BorderRadius.circular(8.r),
                                ),
                                child: Center(
                                  child: Text(
                                    '${index + 1}',
                                    style: AppTextStyles.bodySm.copyWith(
                                      color: index == 0
                                          ? Colors.white
                                          : colors.textPrimary,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                ),
                              ),
                              SizedBox(width: 12.w),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      goal.name,
                                      style: AppTextStyles.bodyMd.copyWith(
                                        fontWeight: FontWeight.w600,
                                        color: colors.textPrimary,
                                      ),
                                    ),
                                    SizedBox(height: 2.h),
                                    Text(
                                      CurrencyFormatter.formatKRW(
                                        goal.targetAmount,
                                      ),
                                      style: AppTextStyles.bodySm.copyWith(
                                        color: colors.textTertiary,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              ReorderableDragStartListener(
                                index: index,
                                child: Icon(
                                  Icons.drag_handle,
                                  color: colors.textTertiary,
                                  size: 24.w,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
          ),
          SafeArea(
            child: Padding(
              padding: EdgeInsets.all(16.w),
              child: MoreBottomButton(
                text: '저장',
                onPressed: () => _savePriority(displayGoals),
                enabled: _hasChanges,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
