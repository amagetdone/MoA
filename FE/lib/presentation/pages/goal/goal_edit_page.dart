import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';

class GoalEditPage extends ConsumerStatefulWidget {
  const GoalEditPage({
    super.key,
    required this.goalId,
  });

  final String goalId;

  @override
  ConsumerState<GoalEditPage> createState() => _GoalEditPageState();
}

class _GoalEditPageState extends ConsumerState<GoalEditPage> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _descriptionController = TextEditingController();
  bool _initialized = false;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(goalViewModelProvider.notifier).loadGoalDetail(widget.goalId);
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final goalState = ref.watch(goalViewModelProvider);
    final goal = goalState.findGoalById(widget.goalId);
    final isLoading = goalState.isDetailLoading(widget.goalId) && goal == null;

    if (isLoading) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(title: '목표 수정'),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (goal == null) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(title: '목표 수정'),
        body: Center(
          child: Text(
            '수정할 목표를 찾지 못했습니다.',
            style: AppTextStyles.bodyMd.copyWith(color: colors.textSecondary),
          ),
        ),
      );
    }

    _syncInitialValue(goal);

    final canSubmit = _canSubmit(goal);

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(title: '목표 수정'),
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(24.w, 12.h, 24.w, 24.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            MoreCard(
              padding: EdgeInsets.all(18.w),
              color: AppColors.orange500.withValues(alpha: 0.06),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(
                    Icons.auto_awesome_rounded,
                    color: AppColors.orange500,
                    size: 20.w,
                  ),
                  SizedBox(width: 10.w),
                  Expanded(
                    child: Text(
                      '제목이나 설명을 바꾸면 AI 이미지가 새로 생성됩니다.',
                      style: AppTextStyles.bodySm.copyWith(
                        color: colors.textPrimary,
                        fontWeight: FontWeight.w700,
                        height: 1.45,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 20.h),
            MoreTextField(
              label: '목표 이름',
              hint: '예: 여행 자금 모으기',
              controller: _nameController,
              onChanged: (_) => setState(() {}),
            ),
            SizedBox(height: 18.h),
            MoreTextField(
              label: '목표 설명',
              hint: '목표에 대한 설명을 입력해주세요',
              controller: _descriptionController,
              maxLines: 5,
              onChanged: (_) => setState(() {}),
            ),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: _isSaving ? '저장 중...' : '수정하기',
        enabled: canSubmit && !_isSaving,
        onPressed: () => _submit(goal),
      ),
    );
  }

  void _syncInitialValue(Goal goal) {
    if (_initialized) {
      return;
    }
    _nameController.text = goal.name;
    _descriptionController.text = goal.description ?? '';
    _initialized = true;
  }

  bool _canSubmit(Goal goal) {
    final currentName = _normalize(_nameController.text);
    final currentDescription = _normalize(_descriptionController.text);
    final originalName = _normalize(goal.name);
    final originalDescription = _normalize(goal.description ?? '');

    return currentName.isNotEmpty &&
        (currentName != originalName || currentDescription != originalDescription);
  }

  String _normalize(String value) => value.trim();

  Future<void> _submit(Goal goal) async {
    if (_isSaving || !_canSubmit(goal)) {
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16.r),
          ),
          title: const Text('이미지를 다시 생성할까요?'),
          content: const Text(
            '목표 제목이나 설명을 수정하면 AI 이미지가 새로 생성돼요.\n진행 중이던 이미지는 교체됩니다.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('취소'),
            ),
            TextButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text(
                '수정하기',
                style: TextStyle(color: AppColors.orange500),
              ),
            ),
          ],
        );
      },
    );

    if (confirmed != true || !mounted) {
      return;
    }

    final updatedGoal = goal.copyWith(
      name: _normalize(_nameController.text),
      description: _normalize(_descriptionController.text).isEmpty
          ? null
          : _normalize(_descriptionController.text),
    );

    setState(() => _isSaving = true);
    try {
      await ref.read(goalViewModelProvider.notifier).updateGoal(updatedGoal);
      if (!mounted) {
        return;
      }
      context.go(
        AppRoutes.goalLoadingLocation(
          goal.id,
          source: 'edit',
        ),
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('목표 수정에 실패했습니다: $error')),
      );
    } finally {
      if (mounted) {
        setState(() => _isSaving = false);
      }
    }
  }
}
