import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';
import 'package:more_app/presentation/widgets/common/more_date_field.dart';

class GoalCreatePage extends StatefulWidget {
  final bool isOnboarding;

  const GoalCreatePage({
    super.key,
    this.isOnboarding = true,
  });

  @override
  State<GoalCreatePage> createState() => _GoalCreatePageState();
}

class _GoalCreatePageState extends State<GoalCreatePage> {
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

  void _onSubmit() {
    if (!_isFormValid) return;

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
        padding: const EdgeInsets.all(24),
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
            const SizedBox(height: 24),

            // Target amount
            MoreTextField(
              label: '목표 금액',
              hint: '0',
              controller: _amountController,
              keyboardType: TextInputType.number,
              suffix: '원',
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 24),

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
            const SizedBox(height: 24),

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
            const SizedBox(height: 24),

            // Description (optional)
            Text(
              '설명 (선택)',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray700,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _descriptionController,
              maxLines: 4,
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.gray900,
              ),
              decoration: InputDecoration(
                hintText: '목표에 대한 설명을 입력해주세요',
                hintStyle: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.gray100,
                ),
                filled: true,
                fillColor: AppColors.gray50,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide(
                    color: AppColors.orange500,
                    width: 1.5,
                  ),
                ),
                contentPadding: const EdgeInsets.all(16),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: MoreBottomButton(
            text: widget.isOnboarding ? '다음' : '생성하기',
            onPressed: _onSubmit,
            enabled: _isFormValid,
          ),
        ),
      ),
    );
  }
}
