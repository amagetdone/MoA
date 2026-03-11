import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';

class AssetDetailPage extends ConsumerWidget {
  final String transactionId;

  const AssetDetailPage({super.key, required this.transactionId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(accountViewModelProvider);

    if (state.isLoading) {
      return const Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: MoreAppBar(title: '결제 상세'),
        body: Center(child: CircularProgressIndicator()),
      );
    }

    final transaction = state.transactions.cast<Transaction?>().firstWhere(
      (t) => t!.id == transactionId,
      orElse: () => null,
    );

    if (transaction == null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: const MoreAppBar(title: '결제 상세'),
        body: const Center(child: Text('거래 내역을 찾을 수 없습니다.')),
      );
    }

    final isExpense = transaction.type == TransactionType.expense;
    final isIncome = transaction.type == TransactionType.income;
    final amountColor = isExpense
        ? AppColors.red500
        : isIncome
        ? AppColors.green500
        : AppColors.textPrimary;

    final typeLabel = switch (transaction.type) {
      TransactionType.income => '입금',
      TransactionType.expense => '출금',
      TransactionType.transfer => '이체',
    };

    final dateFormat = DateFormat('yyyy.MM.dd HH:mm', 'ko_KR');

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '결제 상세'),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 24.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            SizedBox(height: 16.h),

            // Name with verified badge
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  transaction.title,
                  style: AppTextStyles.headingMd.copyWith(
                    color: AppColors.textPrimary,
                    fontSize: 18.sp,
                  ),
                ),
                SizedBox(width: 4.w),
                Icon(
                  Icons.verified,
                  color: AppColors.orange500,
                  size: 20.w,
                ),
              ],
            ),
            SizedBox(height: 8.h),

            // Large amount display
            Text(
              CurrencyFormatter.formatWithSign(
                isExpense ? -transaction.amount : transaction.amount,
              ),
              style: AppTextStyles.headingXl.copyWith(
                color: amountColor,
                fontSize: 28.sp,
              ),
            ),
            SizedBox(height: 32.h),

            // Info rows
            Container(
              padding: EdgeInsets.all(20.w),
              decoration: BoxDecoration(
                color: AppColors.bgSurface,
                borderRadius: BorderRadius.circular(12.r),
                border: Border.all(color: AppColors.borderDefault),
              ),
              child: Column(
                children: [
                  _InfoRow(
                    label: '거래 시간',
                    value: dateFormat.format(transaction.dateTime),
                  ),
                  Padding(
                    padding: EdgeInsets.symmetric(vertical: 12.h),
                    child: Divider(height: 1, color: AppColors.borderDefault),
                  ),
                  _InfoRow(label: '거래 구분', value: '#$typeLabel'),
                  Padding(
                    padding: EdgeInsets.symmetric(vertical: 12.h),
                    child: Divider(height: 1, color: AppColors.borderDefault),
                  ),
                  _InfoRow(
                    label: '거래 금액',
                    value: CurrencyFormatter.formatKRW(transaction.amount),
                  ),
                  Padding(
                    padding: EdgeInsets.symmetric(vertical: 12.h),
                    child: Divider(height: 1, color: AppColors.borderDefault),
                  ),
                  _InfoRow(
                    label: '거래 후 잔액',
                    value: CurrencyFormatter.formatKRW(
                      transaction.balanceAfter,
                    ),
                  ),
                  if (transaction.memo != null &&
                      transaction.memo!.isNotEmpty) ...[
                    Padding(
                      padding: EdgeInsets.symmetric(vertical: 12.h),
                      child: Divider(height: 1, color: AppColors.borderDefault),
                    ),
                    _InfoRow(label: '메모', value: transaction.memo!),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 80.w,
          child: Text(
            label,
            style: AppTextStyles.bodySm.copyWith(
              color: AppColors.textTertiary,
              fontSize: 13.sp,
            ),
          ),
        ),
        SizedBox(width: 12.w),
        Expanded(
          child: Text(
            value,
            style: AppTextStyles.bodyMd.copyWith(
              color: AppColors.textPrimary,
              fontSize: 14.sp,
            ),
            textAlign: TextAlign.end,
          ),
        ),
      ],
    );
  }
}
