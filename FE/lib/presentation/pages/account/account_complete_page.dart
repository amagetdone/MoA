import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
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
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(title: '연동완료', showBack: false),
      body: Center(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 96.w,
                height: 96.w,
                decoration: const BoxDecoration(
                  color: AppColors.orange50,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.check_circle,
                  size: 64.w,
                  color: AppColors.orange500,
                ),
              ),
              SizedBox(height: 32.h),
              Text(
                '연동이 완료되었습니다',
                style: AppTextStyles.headingLg.copyWith(
                  fontSize: 22.sp,
                ),
                textAlign: TextAlign.center,
              ),
              SizedBox(height: 24.h),
              Container(
                width: double.infinity,
                padding: EdgeInsets.all(20.w),
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
                child: Row(
                  children: [
                    BankIcon(bankName: bankName, size: 40.w),
                    SizedBox(width: 16.w),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            bankName,
                            style: AppTextStyles.bodyLg.copyWith(
                              fontWeight: FontWeight.w600,
                              fontSize: 16.sp,
                            ),
                          ),
                          SizedBox(height: 4.h),
                          Text(
                            accountNumber,
                            style: AppTextStyles.bodySm.copyWith(
                              color: AppColors.textTertiary,
                              fontSize: 13.sp,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: '다음',
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
            context.go(AppRoutes.onboardingAccount);
          } else {
            context.go(AppRoutes.accountAdd);
          }
        },
      ),
    );
  }
}
