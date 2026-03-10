import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/bank_icon.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class AccountCompletePage extends ConsumerWidget {
  final bool isOnboarding;
  final String bankName;
  final String accountNumber;

  const AccountCompletePage({
    super.key,
    this.isOnboarding = true,
    required this.bankName,
    required this.accountNumber,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
                decoration: const BoxDecoration(
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
                    BankIcon(bankName: bankName, size: 40),
                    const SizedBox(width: 16),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          bankName,
                          style: AppTextStyles.bodyLg.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          accountNumber,
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textTertiary,
                          ),
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
        onPressed: () async {
          await ref
              .read(accountViewModelProvider.notifier)
              .addLinkedAccount(
                bankName: bankName,
                accountNumber: accountNumber,
              );
          await ref.read(homeViewModelProvider.notifier).loadData();

          if (!context.mounted) {
            return;
          }

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
