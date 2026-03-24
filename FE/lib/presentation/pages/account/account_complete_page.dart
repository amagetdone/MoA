import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

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
    final colors = AppColors.of(context);
    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(title: '', showBack: true),
      body: Padding(
        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '연동완료',
              style: AppTextStyles.headingLg.copyWith(
                color: colors.textPrimary,
                fontSize: 24.sp,
                fontWeight: FontWeight.bold,
              ),
            ),
            SizedBox(height: 6.h),
            Text(
              '연동이 완료되었습니다.',
              style: AppTextStyles.bodySm.copyWith(
                color: colors.textTertiary,
                fontSize: 14.sp,
              ),
            ),
            SizedBox(height: 24.h),

            MoreCard(
              padding: EdgeInsets.all(20.w),
              child: Row(
                children: [
                  Container(
                    width: 48.w,
                    height: 48.w,
                    decoration: const BoxDecoration(
                      color: Color(0xFFFFE812),
                      shape: BoxShape.circle,
                    ),
                    child: Center(
                      child: Text(
                        'B',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 22.sp,
                          color: colors.textPrimary,
                        ),
                      ),
                    ),
                  ),
                  SizedBox(width: 16.w),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        bankName,
                        style: AppTextStyles.bodyMd.copyWith(
                          color: colors.textPrimary,
                          fontWeight: FontWeight.bold,
                          fontSize: 16.sp,
                        ),
                      ),
                      SizedBox(height: 4.h),
                      Text(
                        accountNumber,
                        style: AppTextStyles.bodySm.copyWith(
                          color: colors.textSecondary,
                          fontSize: 14.sp,
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
      bottomNavigationBar: MoreBottomButton(
        text: '완료',
        progress: isOnboarding ? 4 / 7 : null,
        onPressed: () async {
          await ref.read(accountViewModelProvider.notifier).loadData();
          await ref.read(homeViewModelProvider.notifier).loadData();

          if (!context.mounted) return;

          if (isOnboarding) {
            context.go(AppRoutes.onboardingAccount);
          } else {
            context.go(AppRoutes.accountManage);
          }
        },
      ),
    );
  }
}
