import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_date_field.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';

class GoalCreatePage extends ConsumerStatefulWidget {
  final bool isOnboarding;

  const GoalCreatePage({super.key, this.isOnboarding = true});

  @override
  ConsumerState<GoalCreatePage> createState() => _GoalCreatePageState();
}

class _GoalCreatePageState extends ConsumerState<GoalCreatePage> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _amountController = TextEditingController();
  final TextEditingController _descriptionController = TextEditingController();
  DateTime? _startDate = DateTime.now();
  DateTime? _endDate;

  @override
  void dispose() {
    _nameController.dispose();
    _amountController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  bool get _isFormValid {
    return _nameController.text.isNotEmpty &&
        _amountController.text.isNotEmpty &&
        _startDate != null &&
        _endDate != null;
  }

  Future<void> _onSubmit() async {
    if (!_isFormValid) return;

    final targetAmount =
        int.tryParse(_amountController.text.replaceAll(',', '').trim()) ?? 0;
    final currentGoals = ref.read(goalViewModelProvider).inProgressGoals;
    final goal = Goal(
      id: 'goal_${DateTime.now().millisecondsSinceEpoch}',
      name: _nameController.text.trim(),
      description: _descriptionController.text.trim().isEmpty
          ? null
          : _descriptionController.text.trim(),
      targetAmount: targetAmount,
      currentAmount: 0,
      startDate: _startDate!,
      endDate: _endDate!,
      priority: currentGoals.length + 1,
      status: GoalStatus.inProgress,
    );

    await ref.read(goalViewModelProvider.notifier).addGoal(goal);
    await ref.read(homeViewModelProvider.notifier).loadData();

    if (!mounted) return;

    if (widget.isOnboarding) {
      context.go(AppRoutes.onboardingLoading);
    } else {
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: widget.isOnboarding ? '첫 번째 목표 설정' : '새 목표 만들기',
        showBack: true,
        onBack: widget.isOnboarding
            ? () => context.go(AppRoutes.onboardingAccount)
            : null,
        skipText: widget.isOnboarding ? '건너뛰기' : null,
        onSkip: widget.isOnboarding
            ? () => context.go(AppRoutes.onboardingLoading)
            : null,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Goal name
            MoreTextField(
              label: '목표 이름',
              hint: '예: 여행 자금 모으기',
              controller: _nameController,
              onChanged: (_) => setState(() {}),
            ),
            SizedBox(height: 16.h),

            // Description (optional, auto-expanding)
            Text(
              '목표 설명 (선택)',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            SizedBox(height: 8.h),
            Container(
              decoration: BoxDecoration(
                color: AppColors.bgSurface,
                borderRadius: BorderRadius.circular(16.r),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.06),
                    offset: Offset(0, 2.h),
                    blurRadius: 8.r,
                  ),
                ],
              ),
              child: TextField(
                controller: _descriptionController,
                minLines: 1,
                maxLines: null,
                style: AppTextStyles.bodyMd.copyWith(color: AppColors.gray900),
                decoration: InputDecoration(
                  hintText: '목표에 대한 설명을 입력해주세요',
                  hintStyle: AppTextStyles.bodyMd.copyWith(
                    color: AppColors.textTertiary,
                  ),
                  filled: true,
                  fillColor: Colors.transparent,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(16.r),
                    borderSide: BorderSide.none,
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(16.r),
                    borderSide: BorderSide.none,
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(16.r),
                    borderSide: const BorderSide(
                      color: AppColors.buttonPrimary,
                      width: 2,
                    ),
                  ),
                  contentPadding: EdgeInsets.symmetric(
                    horizontal: 16.w,
                    vertical: 12.h,
                  ),
                ),
              ),
            ),
            SizedBox(height: 24.h),

            // Target amount
            MoreTextField(
              label: '목표 금액',
              hint: '0',
              controller: _amountController,
              keyboardType: TextInputType.number,
              suffix: '원',
              onChanged: (_) => setState(() {}),
            ),
            SizedBox(height: 24.h),

            // Start date & End date in one row
            Row(
              children: [
                Expanded(
                  child: MoreDateField(
                    label: '시작일',
                    value: _startDate,
                    onChanged: (date) {
                      setState(() {
                        _startDate = date;
                      });
                    },
                  ),
                ),
                SizedBox(width: 16.w),
                Expanded(
                  child: MoreDateField(
                    label: '종료일',
                    value: _endDate,
                    onChanged: (date) {
                      setState(() {
                        _endDate = date;
                      });
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: widget.isOnboarding ? '다음' : '생성하기',
        onPressed: _onSubmit,
        enabled: _isFormValid,
        progress: widget.isOnboarding ? 3 / 6 : null,
      ),
    );
  }
}
