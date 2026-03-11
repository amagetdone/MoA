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
  DateTime? _startDate;
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
      backgroundColor: Colors.white,
      appBar: MoreAppBar(
        title: widget.isOnboarding ? '첫 번째 목표 설정' : '새 목표 만들기',
        showBack: true,
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

            // Start date
            MoreDateField(
              label: '시작일',
              value: _startDate,
              onChanged: (date) {
                setState(() {
                  _startDate = date;
                });
              },
            ),
            SizedBox(height: 24.h),

            // End date
            MoreDateField(
              label: '종료일',
              value: _endDate,
              onChanged: (date) {
                setState(() {
                  _endDate = date;
                });
              },
            ),
            SizedBox(height: 24.h),

            // Description (optional)
            Text(
              '설명 (선택)',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray700,
                fontWeight: FontWeight.w600,
              ),
            ),
            SizedBox(height: 8.h),
            TextField(
              controller: _descriptionController,
              maxLines: 4,
              style: AppTextStyles.bodyMd.copyWith(color: AppColors.gray900),
              decoration: InputDecoration(
                hintText: '목표에 대한 설명을 입력해주세요',
                hintStyle: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.gray100,
                ),
                filled: true,
                fillColor: AppColors.gray50,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12.r),
                  borderSide: BorderSide.none,
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12.r),
                  borderSide: BorderSide(
                    color: AppColors.orange500,
                    width: 1.5,
                  ),
                ),
                contentPadding: EdgeInsets.all(16.w),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: widget.isOnboarding ? '다음' : '생성하기',
        onPressed: _onSubmit,
        enabled: _isFormValid,
      ),
    );
  }
}
