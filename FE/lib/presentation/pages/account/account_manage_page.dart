import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '계좌 관리'),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        children: [
          // Account list
          ...state.accounts.map((account) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _AccountCard(
                account: account,
                onDelete: () => _showDeleteDialog(context, ref, account),
              ),
            );
          }),

          const SizedBox(height: 8),

          // Add account button
          SizedBox(
            width: double.infinity,
            height: 52,
            child: OutlinedButton.icon(
              onPressed: () => context.push(AppRoutes.accountAdd),
              icon: const Icon(
                Icons.add,
                size: 20,
                color: AppColors.orange500,
              ),
              label: Text(
                '새 계좌 연결하기',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.orange500,
                  fontWeight: FontWeight.w600,
                ),
              ),
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: AppColors.orange500),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  void _showDeleteDialog(
    BuildContext context,
    WidgetRef ref,
    Account account,
  ) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(
          '계좌 삭제',
          style: AppTextStyles.headingMd.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        content: Text(
          '${account.bankName} ${account.accountNumber}\n계좌를 삭제하시겠습니까?',
          style: AppTextStyles.bodyMd.copyWith(
            color: AppColors.textSecondary,
          ),
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
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.red500,
              ),
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

  const _AccountCard({
    required this.account,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return MoreCard(
      child: Row(
        children: [
          BankIcon(
            bankName: account.bankName,
            size: 40,
          ),
          const SizedBox(width: 14),
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
                      ),
                    ),
                    if (account.isPrimary) ...[
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.orange50,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          '주계좌',
                          style: AppTextStyles.caption.copyWith(
                            color: AppColors.orange500,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  account.accountNumber,
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  CurrencyFormatter.formatKRW(account.balance),
                  style: AppTextStyles.bodyMd.copyWith(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: onDelete,
            icon: const Icon(
              Icons.delete_outline,
              color: AppColors.textTertiary,
              size: 22,
            ),
            splashRadius: 20,
          ),
        ],
      ),
    );
  }
}
