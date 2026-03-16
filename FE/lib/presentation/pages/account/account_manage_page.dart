import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class AccountManagePage extends ConsumerWidget {
  final bool isOnboarding;

  const AccountManagePage({super.key, this.isOnboarding = false});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '',
        showBack: true,
        onBack: isOnboarding
            ? () => context.go(AppRoutes.onboarding)
            : () => context.pop(),
        skipText: isOnboarding ? '건너뛰기' : null,
        onSkip: isOnboarding ? () => context.go(AppRoutes.home) : null,
        actions: isOnboarding
            ? null
            : [
                IconButton(
                  icon: const Icon(Icons.notifications_none_outlined, size: 24),
                  onPressed: () {},
                ),
              ],
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              isOnboarding ? '계좌 추가' : '계좌 관리',
              style: AppTextStyles.headingLg.copyWith(fontSize: 24.sp, fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 6.h),
            Text(
              '총 1개의 계좌가 연결되었어요.',
              style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary, fontSize: 14.sp),
            ),
            SizedBox(height: 24.h),

            Text('연결된 계좌', style: AppTextStyles.bodyMd.copyWith(color: AppColors.textSecondary, fontWeight: FontWeight.bold)),
            SizedBox(height: 12.h),

            MoreCard(
              padding: EdgeInsets.all(20.w),
              child: Row(
                children: [
                  Container(
                    width: 48.w,
                    height: 48.w,
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFE812),
                      borderRadius: BorderRadius.circular(12.r),
                    ),
                    child: Center(
                      child: Text('B', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 24.sp, color: Colors.black)),
                    ),
                  ),
                  SizedBox(width: 16.w),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('카카오뱅크', style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold)),
                      SizedBox(height: 4.h),
                      Text('3333-12-3456789', style: AppTextStyles.bodySm.copyWith(color: AppColors.textSecondary)),
                    ],
                  ),
                ],
              ),
            ),
            SizedBox(height: 12.h),

            // 새로운 계좌 연결하기 카드
            GestureDetector(
              onTap: () {
                final route = isOnboarding
                    ? AppRoutes.onboardingAccountSelect
                    : AppRoutes.accountAdd;
                context.push(route);
              },
              child: MoreCard(
                padding: EdgeInsets.symmetric(vertical: 40.h),
                child: Center(
                  child: Column(
                    children: [
                      Container(
                        width: 56.w,
                        height: 56.w,
                        decoration: BoxDecoration(
                          color: AppColors.orange500,
                          borderRadius: BorderRadius.circular(12.r),
                        ),
                        child: Icon(Icons.add, color: Colors.white, size: 36.w),
                      ),
                      SizedBox(height: 16.h),
                      Text('새로운 계좌 연결하기', style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold, fontSize: 16.sp)),
                      SizedBox(height: 6.h),
                      Text(
                        '계좌를 추가하고 자산을 관리하세요 !',
                        style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary, fontSize: 13.sp),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: isOnboarding
          ? MoreBottomButton(
              text: '다음',
              progress: 4 / 7,
              onPressed: () {
                final hasAccounts = ref.read(accountViewModelProvider).accounts.isNotEmpty;
                if (hasAccounts) {
                  context.go(AppRoutes.onboardingLoading);
                } else {
                  context.go(AppRoutes.home);
                }
              },
            )
          : null,
    );
  }
}
