import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/bank_icon.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class AccountAddPage extends ConsumerWidget {
  final bool isOnboarding;

  const AccountAddPage({super.key, this.isOnboarding = true});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(accountViewModelProvider);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '계좌 연동',
        showBack: true,
        skipText: isOnboarding ? '건너뛰기' : null,
        onSkip: isOnboarding
            ? () => context.go(AppRoutes.onboardingGoal)
            : null,
      ),
      body: ListView(
        padding: EdgeInsets.all(24.w),
        children: [
          // Linked accounts list
          if (state.accounts.isNotEmpty) ...[
            ...state.accounts.map((account) {
              return Padding(
                padding: EdgeInsets.only(bottom: 12.h),
                child: MoreCard(
                  child: Row(
                    children: [
                      BankIcon(bankName: account.bankName, size: 40.w),
                      SizedBox(width: 14.w),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              account.bankName,
                              style: AppTextStyles.bodyMd.copyWith(
                                color: AppColors.textPrimary,
                                fontWeight: FontWeight.w600,
                                fontSize: 14.sp,
                              ),
                            ),
                            SizedBox(height: 2.h),
                            Text(
                              account.accountNumber,
                              style: AppTextStyles.bodySm.copyWith(
                                color: AppColors.textTertiary,
                                fontSize: 12.sp,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }),
            SizedBox(height: 8.h),
          ],

          // Add new account button
          GestureDetector(
            onTap: () {
              final route = isOnboarding
                  ? AppRoutes.onboardingAccountSelect
                  : AppRoutes.accountSelect;
              context.push(route, extra: {'isOnboarding': isOnboarding});
            },
            child: Container(
              width: double.infinity,
              padding: EdgeInsets.symmetric(vertical: 20.h),
              decoration: BoxDecoration(
                border: Border.all(color: AppColors.borderDefault),
                borderRadius: BorderRadius.circular(16.r),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.add_circle_outline, size: 24.w, color: AppColors.orange500),
                  SizedBox(width: 8.w),
                  Text(
                    '새로운 계좌 연결하기',
                    style: AppTextStyles.bodyMd.copyWith(
                      color: AppColors.orange500,
                      fontWeight: FontWeight.w600,
                      fontSize: 15.sp,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: MoreBottomButton(
        text: isOnboarding ? '계좌 연동 완료' : '완료',
        onPressed: () {
          if (isOnboarding) {
            context.go(AppRoutes.onboardingGoal);
          } else {
            context.pop();
          }
        },
        enabled: true,
        progress: isOnboarding ? 2 / 6 : null,
      ),
    );
  }
}
