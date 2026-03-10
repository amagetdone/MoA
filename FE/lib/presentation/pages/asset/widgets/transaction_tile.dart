import 'package:flutter/material.dart';
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
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        child: Row(
          children: [
            // Category icon
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _getCategoryColor().withOpacity(0.15),
              ),
              child: Icon(
                _getCategoryIcon(),
                size: 20,
                color: _getCategoryColor(),
              ),
            ),
            const SizedBox(width: 12),

            // Title and time
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    transaction.title,
                    style: AppTextStyles.bodyMd.copyWith(
                      color: AppColors.textPrimary,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    timeFormat.format(transaction.dateTime),
                    style: AppTextStyles.caption.copyWith(
                      color: AppColors.textTertiary,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),

            // Amount
            Text(
              '$amountPrefix${CurrencyFormatter.formatKRW(transaction.amount)}',
              style: AppTextStyles.bodyMd.copyWith(
                color: amountColor,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Color _getCategoryColor() {
    switch (transaction.category) {
      case '식비':
        return AppColors.orange500;
      case '교통':
        return Colors.blue;
      case '쇼핑':
        return Colors.purple;
      case '월급':
      case '급여':
        return AppColors.green500;
      case '이체':
        return Colors.teal;
      case '생활':
        return Colors.indigo;
      case '문화':
        return Colors.pink;
      default:
        return AppColors.textTertiary;
    }
  }

  IconData _getCategoryIcon() {
    switch (transaction.category) {
      case '식비':
        return Icons.restaurant;
      case '교통':
        return Icons.directions_bus;
      case '쇼핑':
        return Icons.shopping_bag;
      case '월급':
      case '급여':
        return Icons.account_balance;
      case '이체':
        return Icons.swap_horiz;
      case '생활':
        return Icons.home;
      case '문화':
        return Icons.movie;
      default:
        return Icons.receipt;
    }
  }
}
