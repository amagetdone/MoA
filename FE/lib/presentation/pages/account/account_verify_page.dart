import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/bank_icon.dart';

class AccountVerifyPage extends StatefulWidget {
  final bool isOnboarding;

  const AccountVerifyPage({
    super.key,
    this.isOnboarding = true,
  });

  @override
  State<AccountVerifyPage> createState() => _AccountVerifyPageState();
}

class _AccountVerifyPageState extends State<AccountVerifyPage> {
  final List<TextEditingController> _digitControllers =
      List.generate(4, (_) => TextEditingController());
  final List<FocusNode> _focusNodes = List.generate(4, (_) => FocusNode());

  String get _bankName => 'カカ오뱅크';
  String get _accountNumber => '3333-01-1234567';

  @override
  void dispose() {
    for (final controller in _digitControllers) {
      controller.dispose();
    }
    for (final node in _focusNodes) {
      node.dispose();
    }
    super.dispose();
  }

  String get _maskedAccountNumber {
    final number = _accountNumber;
    if (number.length <= 4) return number;
    final visible = number.substring(0, 4);
    final masked = '*' * (number.length - 4);
    return '$visible$masked';
  }

  bool get _isCodeComplete {
    return _digitControllers.every(
      (controller) => controller.text.isNotEmpty,
    );
  }

  void _onVerify() {
    if (!_isCodeComplete) return;

    if (widget.isOnboarding) {
      context.go(AppRoutes.onboardingAccountComplete);
    } else {
      context.push(AppRoutes.accountComplete);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: const MoreAppBar(title: '1원 인증'),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const SizedBox(height: 24),
            BankIcon(bankName: _bankName, size: 48),
            const SizedBox(height: 16),
            Text(
              _bankName,
              style: AppTextStyles.headingMd.copyWith(color: AppColors.textPrimary),
            ),
            const SizedBox(height: 8),
            Text(
              _maskedAccountNumber,
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
                letterSpacing: 1.5,
              ),
            ),
            const SizedBox(height: 40),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.gray50,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                '입금자명에 표시된 숫자 4자리를 입력해주세요',
                style: AppTextStyles.bodySm.copyWith(color: AppColors.textSecondary),
                textAlign: TextAlign.center,
              ),
            ),
            const SizedBox(height: 40),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(4, (index) {
                return Container(
                  width: 56,
                  height: 64,
                  margin: const EdgeInsets.symmetric(horizontal: 6),
                  child: TextField(
                    controller: _digitControllers[index],
                    focusNode: _focusNodes[index],
                    keyboardType: TextInputType.number,
                    textAlign: TextAlign.center,
                    maxLength: 1,
                    style: AppTextStyles.headingLg,
                    decoration: InputDecoration(
                      counterText: '',
                      filled: true,
                      fillColor: AppColors.gray50,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide.none,
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: const BorderSide(
                          color: AppColors.orange500,
                          width: 2,
                        ),
                      ),
                    ),
                    onChanged: (value) {
                      if (value.isNotEmpty && index < 3) {
                        _focusNodes[index + 1].requestFocus();
                      }
                      setState(() {});
                    },
                  ),
                );
              }),
            ),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: '인증하기',
        onPressed: _onVerify,
        enabled: _isCodeComplete,
      ),
    );
  }
}
