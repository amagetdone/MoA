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

  @override
  Widget build(BuildContext context) {
    final goalState = ref.watch(goalViewModelProvider);
    final displayGoals = _hasChanges
        ? _reorderedGoals
        : List<Goal>.from(goalState.inProgressGoals);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '목표 우선순위'),
      body: Column(
        children: [
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
            child: Text(
              '드래그하여 우선순위를 변경하세요',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.textTertiary,
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
                        color: AppColors.textTertiary,
                      ),
                    ),
                  )
                : ReorderableListView.builder(
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
                              ReorderableDragStartListener(
                                index: index,
                                child: Padding(
                                  padding: EdgeInsets.only(right: 12.w),
                                  child: Icon(
                                    Icons.drag_handle,
                                    color: AppColors.textTertiary,
                                    size: 24.w,
                                  ),
                                ),
                              ),
                              Container(
                                width: 28.w,
                                height: 28.w,
                                decoration: BoxDecoration(
                                  color: AppColors.orange500,
                                  borderRadius: BorderRadius.circular(8.r),
                                ),
                                child: Center(
                                  child: Text(
                                    '${index + 1}',
                                    style: AppTextStyles.bodySm.copyWith(
                                      color: Colors.white,
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
                                        color: AppColors.textPrimary,
                                      ),
                                    ),
                                    SizedBox(height: 2.h),
                                    Text(
                                      CurrencyFormatter.formatKRW(
                                        goal.targetAmount,
                                      ),
                                      style: AppTextStyles.bodySm.copyWith(
                                        color: AppColors.textTertiary,
                                      ),
                                    ),
                                  ],
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
                onPressed: () async {
                  await ref
                      .read(goalViewModelProvider.notifier)
                      .reorderGoals(displayGoals);
                  await ref.read(homeViewModelProvider.notifier).loadData();
                  if (!context.mounted) return;
                  context.pop();
                },
                enabled: _hasChanges,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
