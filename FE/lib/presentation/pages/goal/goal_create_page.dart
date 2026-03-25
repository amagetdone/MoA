import 'dart:async';

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
import 'package:more_app/core/utils/currency_formatter.dart';
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
  int _selectedPriority = 1;

  @override
  void dispose() {
    _nameController.dispose();
    _amountController.dispose();
    _descriptionController.dispose();
    _priorityController.dispose();
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
      priority: widget.isOnboarding ? 1 : _selectedPriority,
      status: GoalStatus.inProgress,
    );

    try {
      final createdGoal = await ref.read(goalViewModelProvider.notifier).addGoal(goal);
      unawaited(ref.read(homeViewModelProvider.notifier).loadData());

      if (!mounted) return;

      if (widget.isOnboarding) {
        context.go(AppRoutes.onboardingLoading, extra: {'nextRoute': AppRoutes.onboardingPlan});
      } else {
        context.go(
          AppRoutes.goalLoadingLocation(
            createdGoal.id,
            source: 'create',
          ),
        );
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('목표 생성에 실패했습니다: $e')),
      );
      return;
    }
  }

  final TextEditingController _priorityController = TextEditingController(text: '1');

  int get _maxPriority {
    final currentGoals = ref.read(goalViewModelProvider).inProgressGoals;
    return currentGoals.length + 1;
  }

  void _changePriority(int delta) {
    final next = _selectedPriority + delta;
    if (next < 1 || next > _maxPriority) return;
    setState(() {
      _selectedPriority = next;
      _priorityController.text = '$next';
    });
  }

  Widget _buildPrioritySelector(AppColorScheme colors) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 16.w),
      decoration: BoxDecoration(
        color: colors.bgSurface,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: colors.cardShadow,
            offset: Offset(0, 2.h),
            blurRadius: 8.r,
          ),
        ],
      ),
      child: Row(
        children: [
          // Number input
          Expanded(
            child: TextField(
              controller: _priorityController,
              keyboardType: TextInputType.number,
              style: AppTextStyles.bodyMd.copyWith(color: colors.textPrimary),
              decoration: InputDecoration(
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                contentPadding: EdgeInsets.symmetric(vertical: 14.h),
                isDense: true,
                hintText: '1',
                hintStyle: AppTextStyles.bodyMd.copyWith(color: colors.textTertiary),
              ),
              onChanged: (value) {
                final parsed = int.tryParse(value);
                if (parsed != null && parsed >= 1 && parsed <= _maxPriority) {
                  setState(() => _selectedPriority = parsed);
                }
              },
              onSubmitted: (_) {
                final clamped = _selectedPriority.clamp(1, _maxPriority);
                setState(() {
                  _selectedPriority = clamped;
                  _priorityController.text = '$clamped';
                });
              },
            ),
          ),
          // Up / Down arrows stacked
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildArrowButton(
                icon: Icons.keyboard_arrow_up,
                onTap: () => _changePriority(1),
                enabled: _selectedPriority < _maxPriority,
                colors: colors,
              ),
              _buildArrowButton(
                icon: Icons.keyboard_arrow_down,
                onTap: () => _changePriority(-1),
                enabled: _selectedPriority > 1,
                colors: colors,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildArrowButton({
    required IconData icon,
    required VoidCallback onTap,
    required bool enabled,
    required AppColorScheme colors,
  }) {
    return GestureDetector(
      onTap: enabled ? onTap : null,
      child: SizedBox(
        width: 32.w,
        height: 24.h,
        child: Icon(
          icon,
          size: 20.w,
          color: enabled ? colors.textSecondary : colors.borderDefault,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: MoreAppBar(
        title: widget.isOnboarding ? '' : '새 목표 만들기',
        showBack: true,
        onBack: widget.isOnboarding
            ? () => context.go(AppRoutes.onboardingAnalysis)
            : null,
        skipText: widget.isOnboarding ? '건너뛰기' : null,
        onSkip: widget.isOnboarding
            ? () => context.go(AppRoutes.home)
            : null,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (widget.isOnboarding) ...[
              Text(
                '첫 번째 목표 설정',
                style: AppTextStyles.headingLg.copyWith(
                  color: colors.textPrimary,
                  fontSize: 24.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: 6.h),
              Text(
                '달성하고 싶은 목표를 설정해주세요.',
                style: AppTextStyles.bodySm.copyWith(
                  color: colors.textTertiary,
                  fontSize: 14.sp,
                ),
              ),
              SizedBox(height: 24.h),
            ],
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
                color: colors.textSecondary,
              ),
            ),
            SizedBox(height: 8.h),
            Container(
              decoration: BoxDecoration(
                color: colors.bgSurface,
                borderRadius: BorderRadius.circular(16.r),
                boxShadow: [
                  BoxShadow(
                    color: colors.cardShadow,
                    offset: Offset(0, 2.h),
                    blurRadius: 8.r,
                  ),
                ],
              ),
              child: TextField(
                controller: _descriptionController,
                minLines: 1,
                maxLines: null,
                style: AppTextStyles.bodyMd.copyWith(color: colors.gray900),
                decoration: InputDecoration(
                  hintText: '목표에 대한 설명을 입력해주세요',
                  hintStyle: AppTextStyles.bodyMd.copyWith(
                    color: colors.textTertiary,
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
                    borderSide: BorderSide(
                      color: colors.buttonPrimary,
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
              inputFormatters: [CurrencyInputFormatter()],
              suffix: '원',
              onChanged: (_) => setState(() {}),
            ),
            SizedBox(height: 24.h),

            // Priority selector (only for non-onboarding)
            if (!widget.isOnboarding) ...[
              Text(
                '목표 우선순위',
                style: AppTextStyles.bodySm.copyWith(
                  color: colors.textSecondary,
                ),
              ),
              SizedBox(height: 8.h),
              _buildPrioritySelector(colors),
              SizedBox(height: 24.h),
            ],

            // Start date & End date in one row
            Row(
              children: [
                Expanded(
                  child: MoreDateField(
                    label: '시작일',
                    value: _startDate,
                    firstDate: DateTime.now(),
                    onChanged: (date) {
                      setState(() {
                        _startDate = date;
                        if (_endDate != null && _endDate!.isBefore(date)) {
                          _endDate = null;
                        }
                      });
                    },
                  ),
                ),
                SizedBox(width: 16.w),
                Expanded(
                  child: MoreDateField(
                    label: '종료일',
                    value: _endDate,
                    firstDate: _startDate ?? DateTime.now(),
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
        progress: widget.isOnboarding ? 6 / 7 : null,
      ),
    );
  }
}
