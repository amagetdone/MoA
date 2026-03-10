import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
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
  late List<Goal> _reorderedGoals;
  bool _hasChanges = false;

  @override
  void initState() {
    super.initState();
    final goalState = ref.read(goalViewModelProvider);
    _reorderedGoals = goalState.goals
        .where((g) => g.status == GoalStatus.inProgress)
        .toList()
      ..sort((a, b) => a.priority.compareTo(b.priority));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '목표 우선순위'),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Text(
              '드래그하여 우선순위를 변경하세요',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ),
          Expanded(
            child: _reorderedGoals.isEmpty
                ? Center(
                    child: Text(
                      '진행 중인 목표가 없습니다.',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  )
                : ReorderableListView.builder(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: _reorderedGoals.length,
                    onReorder: (oldIndex, newIndex) {
                      setState(() {
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
                        borderRadius: BorderRadius.circular(12),
                        shadowColor: Colors.black26,
                        child: child,
                      );
                    },
                    itemBuilder: (context, index) {
                      final goal = _reorderedGoals[index];
                      return Padding(
                        key: ValueKey(goal.id),
                        padding: const EdgeInsets.only(bottom: 12),
                        child: MoreCard(
                          child: Row(
                            children: [
                              ReorderableDragStartListener(
                                index: index,
                                child: Padding(
                                  padding: const EdgeInsets.only(right: 12),
                                  child: Icon(
                                    Icons.drag_handle,
                                    color: AppColors.textTertiary,
                                  ),
                                ),
                              ),
                              Container(
                                width: 28,
                                height: 28,
                                decoration: BoxDecoration(
                                  color: AppColors.orange500,
                                  borderRadius: BorderRadius.circular(8),
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
                              const SizedBox(width: 12),
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
                                    const SizedBox(height: 2),
                                    Text(
                                      CurrencyFormatter.formatKRW(
                                          goal.targetAmount),
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
              padding: const EdgeInsets.all(16),
              child: MoreBottomButton(
                text: '저장',
                onPressed: () {
                  ref
                      .read(goalViewModelProvider.notifier)
                      .reorderGoals(_reorderedGoals);
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