import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class AssetDetailPage extends StatelessWidget {
  final Transaction transaction;

  const AssetDetailPage({super.key, required this.transaction});

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final isIncome = transaction.type == TransactionType.income;
    final isCard = transaction.memo == 'VARIABLE' || transaction.memo == 'FIXED';
    final dateFormat = DateFormat('yyyy.MM.dd. HH:mm');

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(
        title: '',
        showBack: true,
        showNotification: true,
      ),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 12.h),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    transaction.title,
                    style: AppTextStyles.headingLg.copyWith(color: colors.textPrimary, fontSize: 24.sp),
                  ),
                  SizedBox(height: 8.h),
                  Row(
                    children: [
                      Container(
                        padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 3.h),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFCC91).withValues(alpha: 0.4),
                          borderRadius: BorderRadius.circular(10.r),
                        ),
                        child: Text(
                          transaction.category,
                          style: AppTextStyles.caption.copyWith(color: AppColors.orange500, fontWeight: FontWeight.bold),
                        ),
                      ),
                      if (transaction.memo != null) ...[
                        SizedBox(width: 8.w),
                        Container(
                          padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 3.h),
                          decoration: BoxDecoration(
                            color: colors.gray100,
                            borderRadius: BorderRadius.circular(10.r),
                          ),
                          child: Text(
                            transaction.memo == 'FIXED' ? '고정비' : transaction.memo == 'VARIABLE' ? '변동비' : transaction.memo!,
                            style: AppTextStyles.caption.copyWith(color: colors.gray500, fontWeight: FontWeight.bold),
                          ),
                        ),
                      ],
                    ],
                  ),
                  SizedBox(height: 24.h),
                  Divider(color: colors.borderDefault, thickness: 1, height: 1),
                  SizedBox(height: 24.h),

                  _DetailRow(label: '거래 시간', value: dateFormat.format(transaction.dateTime)),
                  SizedBox(height: 20.h),
                  _DetailRow(label: '거래 구분', value: '# ${isIncome ? '입금' : '출금'}'),

                  if (isCard) ...[
                    SizedBox(height: 20.h),
                    _DetailRow(label: '비용 유형', value: '# ${transaction.memo == 'FIXED' ? '고정비' : '변동비'}'),
                  ],

                  SizedBox(height: 20.h),
                  Divider(color: colors.borderDefault, thickness: 1),
                  SizedBox(height: 24.h),

                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('거래 금액', style: AppTextStyles.bodyMd.copyWith(color: colors.textSecondary)),
                      Text(
                        CurrencyFormatter.formatKRW(transaction.amount),
                        style: AppTextStyles.bodyLg.copyWith(
                          fontWeight: FontWeight.bold,
                          color: isIncome ? AppColors.green500 : colors.textPrimary,
                        ),
                      ),
                    ],
                  ),

                  if (transaction.balanceAfter > 0) ...[
                    SizedBox(height: 20.h),
                    _DetailRow(label: '거래 후 잔액', value: CurrencyFormatter.formatKRW(transaction.balanceAfter)),
                  ],
                ],
              ),
            ),
          ),
          MoreBottomButton(
            text: '확인',
            onPressed: () => Navigator.pop(context),
          ),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;

  const _DetailRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: AppTextStyles.bodyMd.copyWith(color: colors.textSecondary)),
        Text(value, style: AppTextStyles.bodyMd.copyWith(color: colors.textPrimary, fontWeight: FontWeight.w500)),
      ],
    );
  }
}
