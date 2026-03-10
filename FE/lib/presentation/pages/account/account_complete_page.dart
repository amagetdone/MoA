import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/bank_icon.dart';

class AccountCompletePage extends StatelessWidget {
  final bool isOnboarding;

  const AccountCompletePage({
    super.key,
    this.isOnboarding = true,
  });

  @override
  Widget build(BuildContext context) {
    const bankName = '카카오뱅크';
    const accountNumber = '3333-01-1234567';

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: const MoreAppBar(title: '연동 완료', showBack: false),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 96,
                height: 96,
                decoration: BoxDecoration(
                  color: AppColors.orange50,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.check_circle,
                  size: 64,
                  color: AppColors.orange500,
                ),
              ),
              const SizedBox(height: 32),
              Text(
                '계좌 연동이 완료되었습니다!',
                style: AppTextStyles.headingLg,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: AppColors.gray50,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const BankIcon(bankName: bankName, size: 40),
                    const SizedBox(width: 16),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          bankName,
                          style: AppTextStyles.bodyLg.copyWith(fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          accountNumber,
                          style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: isOnboarding ? '다음' : '확인',
        onPressed: () {
          if (isOnboarding) {
            context.go(AppRoutes.onboardingGoal);
          } else {
            context.pop();
          }
        },
      ),
    );
  }
}
