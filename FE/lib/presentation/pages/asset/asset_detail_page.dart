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
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class AssetDetailPage extends ConsumerWidget {
  final String transactionId;

  const AssetDetailPage({super.key, required this.transactionId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(accountViewModelProvider);

    // API 미연동 시 transactionId 패턴 기반 더미 데이터 사용
    final Transaction? foundTransaction = state.transactions.cast<Transaction?>().firstWhere(
      (t) => t!.id == transactionId,
      orElse: () => null,
    );

    final bool isCredit = transactionId.contains('credit') || foundTransaction?.memo?.contains('신용') == true;
    final bool isIncome = transactionId.contains('income') || foundTransaction?.type == TransactionType.income;

    final Transaction transaction = foundTransaction ?? Transaction(
      id: transactionId,
      title: '홍길동',
      amount: isCredit ? 30000 : 90000,
      type: isIncome ? TransactionType.income : TransactionType.expense,
      category: '카페',
      dateTime: DateTime(2025, 3, 5, 14, 0),
      balanceAfter: isIncome ? 3250000 : 3160000,
      memo: isCredit ? '신용' : null,
    );
    
    final dateFormat = DateFormat('yyyy.MM.dd. HH:mm');

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
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
                  // 상단 타이틀 및 배지
                  Text(
                    transaction.title,
                    style: AppTextStyles.headingLg.copyWith(fontSize: 24.sp),
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
                          '카페', 
                          style: AppTextStyles.caption.copyWith(color: AppColors.orange500, fontWeight: FontWeight.bold),
                        ),
                      ),
                      SizedBox(width: 8.w),
                      const Icon(Icons.circle, size: 14, color: Colors.blueAccent),
                      SizedBox(width: 4.w),
                      Text(
                        isCredit ? '토스카드 1234-1234-1234-1234' : '토스뱅크',
                        style: AppTextStyles.caption.copyWith(color: const Color(0xFFAAAAAA), fontSize: 13.sp),
                      ),
                    ],
                  ),
                  SizedBox(height: 24.h),
                  const Divider(color: Color(0xFFEEEEEE), thickness: 1, height: 1),
                  SizedBox(height: 24.h),

                  // 정보 섹션
                  _DetailRow(label: '거래 시간', value: dateFormat.format(transaction.dateTime)),
                  SizedBox(height: 20.h),
                  _DetailRow(label: '거래 구분', value: '# ${isIncome ? '입금' : '출금'}'),
                  
                  if (isCredit) ...[
                    SizedBox(height: 20.h),
                    _DetailRow(label: '카드 구분', value: '# 신용'),
                  ],
                  
                  SizedBox(height: 20.h),
                  const Divider(color: Color(0xFFEEEEEE), thickness: 1),
                  SizedBox(height: 24.h),

                  // 거래 금액
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('거래 금액', style: AppTextStyles.bodyMd.copyWith(color: AppColors.textSecondary)),
                      Text(
                        CurrencyFormatter.formatKRW(transaction.amount),
                        style: AppTextStyles.bodyLg.copyWith(
                          fontWeight: FontWeight.bold,
                          color: isCredit ? const Color(0xFF64B5F6) : (isIncome ? const Color(0xFF81C784) : Colors.black87),
                        ),
                      ),
                    ],
                  ),
                  
                  // 입/출금일 경우에만 잔액 표시
                  if (!isCredit) ...[
                    SizedBox(height: 20.h),
                    _DetailRow(label: '거래 후 잔액', value: CurrencyFormatter.formatKRW(transaction.balanceAfter)),
                  ],
                ],
              ),
            ),
          ),
          // 하단 확인 버튼
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
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: AppTextStyles.bodyMd.copyWith(color: AppColors.textSecondary)),
        Text(value, style: AppTextStyles.bodyMd.copyWith(color: Colors.black87, fontWeight: FontWeight.w500)),
      ],
    );
  }
}
