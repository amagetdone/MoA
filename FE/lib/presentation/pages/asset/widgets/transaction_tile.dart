import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/transaction.dart';

class TransactionTile extends StatelessWidget {
  final Transaction transaction;
  final VoidCallback? onTap;

  const TransactionTile({
    super.key,
    required this.transaction,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isExpense = transaction.type == TransactionType.expense;
    final isIncome = transaction.type == TransactionType.income;
    final amountColor = isExpense
        ? AppColors.red500
        : isIncome
            ? AppColors.green500
            : AppColors.textPrimary;
    final amountPrefix = isExpense
        ? '-'
        : isIncome
            ? '+'
            : '';
    final timeFormat = DateFormat('HH:mm');

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 14.h),
        child: Row(
          children: [
            // Category icon
            Container(
              width: 40.w,
              height: 40.w,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _getCategoryColor().withValues(alpha: 0.15),
              ),
              child: Icon(
                _getCategoryIcon(),
                size: 20.w,
                color: _getCategoryColor(),
              ),
            ),
            SizedBox(width: 12.w),

            // Title and time
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    transaction.title,
                    style: AppTextStyles.bodyMd.copyWith(
                      color: AppColors.textPrimary,
                      fontSize: 14.sp,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  SizedBox(height: 2.h),
                  Text(
                    timeFormat.format(transaction.dateTime),
                    style: AppTextStyles.caption.copyWith(
                      color: AppColors.textTertiary,
                      fontSize: 11.sp,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(width: 8.w),

            // Amount
            Text(
              '$amountPrefix${CurrencyFormatter.formatKRW(transaction.amount)}',
              style: AppTextStyles.bodyMd.copyWith(
                color: amountColor,
                fontWeight: FontWeight.w600,
                fontSize: 14.sp,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Color _getCategoryColor() {
    switch (transaction.category) {
      case '교육비':
        return const Color(0xFFB39DDB); // 연보라
      case '교통비':
        return const Color(0xFF90CAF9); // 연파랑
      case '구독료':
        return const Color(0xFF80CBC4); // 연민트
      case '금융/보험':
        return const Color(0xFFA5D6A7); // 연초록
      case '기타서비스':
        return const Color(0xFFB0BEC5); // 연회색
      case '문화/여가/여행':
        return const Color(0xFFF48FB1); // 연핑크
      case '미용':
        return const Color(0xFFF8BBD0); // 연로즈
      case '생활/반려동물':
        return const Color(0xFFC5E1A5); // 연라임
      case '세금/공과금':
        return const Color(0xFFFFCC80); // 연황금
      case '쇼핑/패션':
        return const Color(0xFFCE93D8); // 연퍼플
      case '식비':
        return const Color(0xFFFFAB91); // 연살몬
      case '의료/건강':
        return const Color(0xFF80DEEA); // 연하늘
      case '주거/통신':
        return const Color(0xFF90A4AE); // 연블루그레이
      case '카페':
        return const Color(0xFFBCAAA4); // 연브라운
      case '편의점':
        return const Color(0xFFFFF176); // 연노랑
      default:
        return const Color(0xFFB0BEC5); // 연회색
    }
  }

  IconData _getCategoryIcon() {
    switch (transaction.category) {
      case '교육비':
        return Icons.school;
      case '교통비':
        return Icons.directions_bus;
      case '구독료':
        return Icons.subscriptions;
      case '금융/보험':
        return Icons.account_balance_wallet;
      case '기타서비스':
        return Icons.build;
      case '문화/여가/여행':
        return Icons.flight;
      case '미용':
        return Icons.content_cut;
      case '생활/반려동물':
        return Icons.pets;
      case '세금/공과금':
        return Icons.receipt_long;
      case '쇼핑/패션':
        return Icons.shopping_bag;
      case '식비':
        return Icons.restaurant;
      case '의료/건강':
        return Icons.local_hospital;
      case '주거/통신':
        return Icons.phone_android;
      case '카페':
        return Icons.local_cafe;
      case '편의점':
        return Icons.store;
      default:
        return Icons.receipt;
    }
  }
}
