import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class OnboardingIncomePage extends StatefulWidget {
  const OnboardingIncomePage({super.key});

  @override
  State<OnboardingIncomePage> createState() => _OnboardingIncomePageState();
}

class _OnboardingIncomePageState extends State<OnboardingIncomePage> {
  final TextEditingController _incomeController =
      TextEditingController(text: '3,500,000');
  final TextEditingController _fixedExpenseController =
      TextEditingController(text: '850,000');

  @override
  void dispose() {
    _incomeController.dispose();
    _fixedExpenseController.dispose();
    super.dispose();
  }

  bool get _isFormValid {
    return _incomeController.text.isNotEmpty &&
        _fixedExpenseController.text.isNotEmpty;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: MoreAppBar(
        title: '수입 확인',
        showBack: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '월 수입을 확인해주세요',
              style: AppTextStyles.headingLg.copyWith(
                color: AppColors.gray900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '정확한 분석을 위해 수입과 고정 지출을 확인해주세요',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray500,
              ),
            ),
            const SizedBox(height: 32),

            // Monthly income
            Text(
              '월 수입',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray700,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            _AmountInputField(
              controller: _incomeController,
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 32),

            // Fixed expenses
            Text(
              '고정 지출',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray700,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              '월세, 보험, 통신비 등 매달 고정으로 나가는 비용',
              style: AppTextStyles.caption.copyWith(
                color: AppColors.gray500,
              ),
            ),
            const SizedBox(height: 8),
            _AmountInputField(
              controller: _fixedExpenseController,
              onChanged: (_) => setState(() {}),
            ),

            const SizedBox(height: 32),

            // Summary
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.gray50,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                children: [
                  _SummaryRow(
                    label: '월 수입',
                    value: '${_incomeController.text}원',
                  ),
                  const SizedBox(height: 8),
                  _SummaryRow(
                    label: '고정 지출',
                    value: '-${_fixedExpenseController.text}원',
                    valueColor: AppColors.gray500,
                  ),
                  const Divider(height: 24),
                  _SummaryRow(
                    label: '가용 금액',
                    value: '2,650,000원',
                    isBold: true,
                    valueColor: AppColors.orange500,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: MoreBottomButton(
            text: '다음',
            onPressed: () {
              context.go(AppRoutes.onboardingAnalysis);
            },
            enabled: _isFormValid,
          ),
        ),
      ),
    );
  }
}

class _AmountInputField extends StatelessWidget {
  final TextEditingController controller;
  final ValueChanged<String>? onChanged;

  const _AmountInputField({
    required this.controller,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      keyboardType: TextInputType.number,
      style: AppTextStyles.headingMd.copyWith(
        color: AppColors.gray900,
      ),
      decoration: InputDecoration(
        suffixText: '원',
        suffixStyle: AppTextStyles.headingMd.copyWith(
          color: AppColors.gray500,
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
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 16,
        ),
      ),
      onChanged: onChanged,
    );
  }
}

class _SummaryRow extends StatelessWidget {
  final String label;
  final String value;
  final bool isBold;
  final Color? valueColor;

  const _SummaryRow({
    required this.label,
    required this.value,
    this.isBold = false,
    this.valueColor,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: AppColors.gray500,
            fontWeight: isBold ? FontWeight.w600 : FontWeight.w400,
          ),
        ),
        Text(
          value,
          style: (isBold ? AppTextStyles.bodyLg : AppTextStyles.bodySm).copyWith(
            color: valueColor ?? AppColors.gray900,
            fontWeight: isBold ? FontWeight.w700 : FontWeight.w500,
          ),
        ),
      ],
    );
  }
}
