import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/bank_icon.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class AccountManagePage extends ConsumerWidget {
  const AccountManagePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(accountViewModelProvider);

    if (state.isLoading) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: MoreAppBar(title: '계좌 관리'),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (state.errorMessage != null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: MoreAppBar(title: '계좌 관리'),
        body: Center(
          child: Text(
            '계좌 목록을 불러오지 못했습니다.\n${state.errorMessage!}',
            textAlign: TextAlign.center,
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(title: '계좌 관리'),
      body: ListView(
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
        children: [
          // Account list
          ...state.accounts.map((account) {
            return Padding(
              padding: EdgeInsets.only(bottom: 12.h),
              child: _AccountCard(
                account: account,
                onDelete: () => _showDeleteDialog(context, ref, account),
              ),
            );
          }),

          SizedBox(height: 8.h),

          // Add account button
          GestureDetector(
            onTap: () => context.push(AppRoutes.accountAdd),
            child: Container(
              width: double.infinity,
              padding: EdgeInsets.symmetric(vertical: 20.h),
              decoration: BoxDecoration(
                border: Border.all(color: AppColors.borderDefault),
                borderRadius: BorderRadius.circular(12.r),
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
          SizedBox(height: 24.h),
        ],
      ),
    );
  }

  void _showDeleteDialog(BuildContext context, WidgetRef ref, Account account) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(
          '계좌 삭제',
          style: AppTextStyles.headingMd.copyWith(color: AppColors.textPrimary),
        ),
        content: Text(
          '${account.bankName} ${account.accountNumber}\n계좌를 삭제하시겠습니까?',
          style: AppTextStyles.bodyMd.copyWith(color: AppColors.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(
              '취소',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              ref
                  .read(accountViewModelProvider.notifier)
                  .removeAccount(account.id);
            },
            child: Text(
              '삭제',
              style: AppTextStyles.bodyMd.copyWith(color: AppColors.red500),
            ),
          ),
        ],
      ),
    );
  }
}

class _AccountCard extends StatelessWidget {
  final Account account;
  final VoidCallback onDelete;

  const _AccountCard({required this.account, required this.onDelete});

  @override
  Widget build(BuildContext context) {
    return MoreCard(
      child: Row(
        children: [
          BankIcon(bankName: account.bankName, size: 40.w),
          SizedBox(width: 14.w),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      account.bankName,
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.w600,
                        fontSize: 14.sp,
                      ),
                    ),
                    if (account.isPrimary) ...[
                      SizedBox(width: 6.w),
                      Container(
                        padding: EdgeInsets.symmetric(
                          horizontal: 6.w,
                          vertical: 2.h,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.orange50,
                          borderRadius: BorderRadius.circular(4.r),
                        ),
                        child: Text(
                          '주계좌',
                          style: AppTextStyles.caption.copyWith(
                            color: AppColors.orange500,
                            fontWeight: FontWeight.w600,
                            fontSize: 10.sp,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
                SizedBox(height: 2.h),
                Text(
                  account.accountNumber,
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.textTertiary,
                    fontSize: 12.sp,
                  ),
                ),
                SizedBox(height: 4.h),
                Text(
                  CurrencyFormatter.formatKRW(account.balance),
                  style: AppTextStyles.bodyMd.copyWith(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w600,
                    fontSize: 14.sp,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: onDelete,
            icon: Icon(
              Icons.delete_outline,
              color: AppColors.textTertiary,
              size: 22.w,
            ),
            splashRadius: 20.r,
          ),
        ],
      ),
    );
  }
}
