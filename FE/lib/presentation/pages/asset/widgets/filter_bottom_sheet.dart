import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_sheet.dart';
import 'package:more_app/presentation/widgets/common/more_chip.dart';

class TransactionFilter {
  final String period;
  final TransactionType? transactionType;
  final String? accountId;
  final String creditCardFilter;

  const TransactionFilter({
    this.period = '1개월',
    this.transactionType,
    this.accountId,
    this.creditCardFilter = '미포함',
  });

  TransactionFilter copyWith({
    String? period,
    TransactionType? Function()? transactionType,
    String? Function()? accountId,
    String? creditCardFilter,
  }) {
    return TransactionFilter(
      period: period ?? this.period,
      transactionType:
          transactionType != null ? transactionType() : this.transactionType,
      accountId: accountId != null ? accountId() : this.accountId,
      creditCardFilter: creditCardFilter ?? this.creditCardFilter,
    );
  }
}

class FilterBottomSheet extends StatefulWidget {
  final TransactionFilter initialFilter;
  final List<Account> accounts;

  const FilterBottomSheet({
    super.key,
    required this.initialFilter,
    required this.accounts,
  });

  static Future<TransactionFilter?> show(
    BuildContext context, {
    required TransactionFilter initialFilter,
    required List<Account> accounts,
  }) {
    return MoreBottomSheet.show<TransactionFilter>(
      context: context,
      title: '필터',
      child: FilterBottomSheet(
        initialFilter: initialFilter,
        accounts: accounts,
      ),
    );
  }

  @override
  State<FilterBottomSheet> createState() => _FilterBottomSheetState();
}

class _FilterBottomSheetState extends State<FilterBottomSheet> {
  late TransactionFilter _filter;

  static const _periods = ['1개월', '3개월', '6개월', '기간설정'];
  static const _transactionTypes = ['전체', '입금형', '출금형'];
  static const _creditCardOptions = ['미포함', '포함', '신용카드만'];

  @override
  void initState() {
    super.initState();
    _filter = widget.initialFilter;
  }

  TransactionType? _typeFromLabel(String label) {
    return switch (label) {
      '입금형' => TransactionType.income,
      '출금형' => TransactionType.expense,
      _ => null,
    };
  }

  String _typeToLabel(TransactionType? type) {
    return switch (type) {
      TransactionType.income => '입금형',
      TransactionType.expense => '출금형',
      TransactionType.transfer => '전체',
      null => '전체',
    };
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        // Period section
        _SectionTitle(title: '조회 기간'),
        SizedBox(height: 8.h),
        Wrap(
          spacing: 8.w,
          runSpacing: 8.h,
          children: _periods.map((period) {
            return MoreChip(
              label: period,
              selected: _filter.period == period,
              onTap: () {
                setState(() {
                  _filter = _filter.copyWith(period: period);
                });
              },
            );
          }).toList(),
        ),
        SizedBox(height: 20.h),

        // Transaction type section
        _SectionTitle(title: '거래 유형'),
        SizedBox(height: 8.h),
        Wrap(
          spacing: 8.w,
          runSpacing: 8.h,
          children: _transactionTypes.map((typeLabel) {
            final type = _typeFromLabel(typeLabel);
            final isSelected = _typeToLabel(_filter.transactionType) == typeLabel;
            return MoreChip(
              label: typeLabel,
              selected: isSelected,
              onTap: () {
                setState(() {
                  _filter = _filter.copyWith(
                    transactionType: () => type,
                  );
                });
              },
            );
          }).toList(),
        ),
        SizedBox(height: 20.h),

        // Credit card section
        _SectionTitle(title: '신용카드 거래내역 포함 여부'),
        SizedBox(height: 8.h),
        Wrap(
          spacing: 8.w,
          runSpacing: 8.h,
          children: _creditCardOptions.map((option) {
            return MoreChip(
              label: option,
              selected: _filter.creditCardFilter == option,
              onTap: () {
                setState(() {
                  _filter = _filter.copyWith(creditCardFilter: option);
                });
              },
            );
          }).toList(),
        ),
        SizedBox(height: 20.h),

        // Account section
        _SectionTitle(title: '거래 계좌'),
        SizedBox(height: 8.h),
        Wrap(
          spacing: 8.w,
          runSpacing: 8.h,
          children: [
            MoreChip(
              label: '전체',
              selected: _filter.accountId == null,
              onTap: () {
                setState(() {
                  _filter = _filter.copyWith(
                    accountId: () => null,
                  );
                });
              },
            ),
            ...widget.accounts.map((account) {
              return MoreChip(
                label: account.accountName,
                selected: _filter.accountId == account.id,
                onTap: () {
                  setState(() {
                    _filter = _filter.copyWith(
                      accountId: () => account.id,
                    );
                  });
                },
              );
            }),
          ],
        ),
        SizedBox(height: 24.h),

        // Apply button
        MoreBottomButton(
          text: '완료',
          onPressed: () {
            Navigator.of(context).pop(_filter);
          },
        ),
        SizedBox(height: 16.h),
      ],
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;

  const _SectionTitle({required this.title});

  @override
  Widget build(BuildContext context) {
    return Text(
      title,
      style: AppTextStyles.bodySm.copyWith(
        color: AppColors.textSecondary,
        fontWeight: FontWeight.w600,
        fontSize: 13.sp,
      ),
    );
  }
}
