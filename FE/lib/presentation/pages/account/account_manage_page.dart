import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class AccountManagePage extends ConsumerStatefulWidget {
  final bool isOnboarding;

  const AccountManagePage({super.key, this.isOnboarding = false});

  @override
  ConsumerState<AccountManagePage> createState() => _AccountManagePageState();
}

class _AccountManagePageState extends ConsumerState<AccountManagePage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(accountViewModelProvider.notifier).loadData();
    });
  }

  @override
  Widget build(BuildContext context) {
    final accountState = ref.watch(accountViewModelProvider);
    final accounts = accountState.accounts;

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '',
        showBack: true,
        onBack: widget.isOnboarding
            ? () => context.go(AppRoutes.onboarding)
            : () => context.pop(),
        skipText: widget.isOnboarding ? '건너뛰기' : null,
        onSkip: widget.isOnboarding ? () => context.go(AppRoutes.home) : null,
        showNotification: !widget.isOnboarding,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(20.w, 0, 20.w, 20.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.isOnboarding ? '계좌 추가' : '계좌 관리',
              style: AppTextStyles.headingMd.copyWith(fontSize: 20.sp, fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 4.h),
            Text(
              accounts.isEmpty
                  ? '연결된 계좌가 없어요.'
                  : '총 ${accounts.length}개의 계좌가 연결되었어요.',
              style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary, fontSize: 12.sp),
            ),
            SizedBox(height: 20.h),

            if (accounts.isNotEmpty) ...[
              Text('연결된 계좌', style: AppTextStyles.bodyMd.copyWith(color: AppColors.textSecondary, fontWeight: FontWeight.bold)),
              SizedBox(height: 12.h),
              ...accounts.map((account) => Padding(
                padding: EdgeInsets.only(bottom: 12.h),
                child: Opacity(
                  opacity: account.verified ? 1.0 : 0.45,
                  child: MoreCard(
                    padding: EdgeInsets.all(20.w),
                    child: Row(
                      children: [
                        Container(
                          width: 48.w,
                          height: 48.w,
                          decoration: BoxDecoration(
                            color: account.verified ? AppColors.orange500 : AppColors.gray500,
                            borderRadius: BorderRadius.circular(12.r),
                          ),
                          child: Center(
                            child: Text(
                              account.bankName.isNotEmpty ? account.bankName[0] : 'B',
                              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 24.sp, color: Colors.white),
                            ),
                          ),
                        ),
                        SizedBox(width: 16.w),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Text(account.bankName, style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold)),
                                  if (!account.verified) ...[
                                    SizedBox(width: 8.w),
                                    Container(
                                      padding: EdgeInsets.symmetric(horizontal: 6.w, vertical: 2.h),
                                      decoration: BoxDecoration(
                                        color: AppColors.gray100,
                                        borderRadius: BorderRadius.circular(4.r),
                                      ),
                                      child: Text(
                                        '연결 해제됨',
                                        style: AppTextStyles.caption.copyWith(
                                          color: AppColors.gray500,
                                          fontSize: 11.sp,
                                        ),
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                              SizedBox(height: 4.h),
                              Text(account.accountNumber, style: AppTextStyles.bodySm.copyWith(color: AppColors.textSecondary)),
                            ],
                          ),
                        ),
                        if (account.verified)
                          GestureDetector(
                            onTap: () {
                              showDialog(
                                context: context,
                                builder: (ctx) => AlertDialog(
                                  title: const Text('계좌 연결 해제'),
                                  content: Text('${account.bankName} 계좌 연결을 해제하시겠습니까?'),
                                  actions: [
                                    TextButton(
                                      onPressed: () => Navigator.of(ctx).pop(),
                                      child: const Text('취소'),
                                    ),
                                    TextButton(
                                      onPressed: () async {
                                        Navigator.of(ctx).pop();
                                        await ref.read(accountViewModelProvider.notifier).removeAccount(account.id);
                                        ref.read(homeViewModelProvider.notifier).loadData();
                                      },
                                      child: Text('해제', style: TextStyle(color: Colors.red)),
                                    ),
                                  ],
                                ),
                              );
                            },
                            child: Icon(Icons.delete_outline, color: AppColors.textTertiary, size: 24.w),
                          )
                        else
                          Opacity(
                            opacity: 1.0,
                            child: GestureDetector(
                              onTap: () async {
                                try {
                                  await ref
                                      .read(accountRepositoryProvider)
                                      .requestVerification(account.accountNumber);
                                  if (!context.mounted) return;
                                  context.push(
                                    AppRoutes.accountVerify,
                                    extra: {
                                      'bankName': account.bankName,
                                      'accountNumber': account.accountNumber,
                                      'isOnboarding': false,
                                    },
                                  );
                                } catch (e) {
                                  if (!context.mounted) return;
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('인증 요청 중 오류가 발생했습니다.')),
                                  );
                                }
                              },
                              child: Container(
                                padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 8.h),
                                decoration: BoxDecoration(
                                  color: AppColors.orange500,
                                  borderRadius: BorderRadius.circular(8.r),
                                ),
                                child: Text(
                                  '다시 연결',
                                  style: AppTextStyles.bodySm.copyWith(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w600,
                                    fontSize: 12.sp,
                                  ),
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              )),
            ],

            // 새로운 계좌 연결하기 카드
            GestureDetector(
              onTap: () {
                final route = widget.isOnboarding
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
      bottomNavigationBar: widget.isOnboarding
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
