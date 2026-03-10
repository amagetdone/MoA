import 'package:flutter/material.dart';
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

  const TransactionFilter({
    this.period = '1개월',
    this.transactionType,
    this.accountId,
  });

  TransactionFilter copyWith({
    String? period,
    TransactionType? Function()? transactionType,
    String? Function()? accountId,
  }) {
    return TransactionFilter(
      period: period ?? this.period,
      transactionType:
          transactionType != null ? transactionType() : this.transactionType,
      accountId: accountId != null ? accountId() : this.accountId,
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

  static const _periods = ['1주일', '1개월', '3개월', '직접설정'];
  static const _transactionTypes = ['전체', '입금', '출금', '이체'];

  @override
  void initState() {
    super.initState();
    _filter = widget.initialFilter;
  }

  TransactionType? _typeFromLabel(String label) {
    return switch (label) {
      '입금' => TransactionType.income,
      '출금' => TransactionType.expense,
      '이체' => TransactionType.transfer,
      _ => null,
    };
  }

  String _typeToLabel(TransactionType? type) {
    return switch (type) {
      TransactionType.income => '입금',
      TransactionType.expense => '출금',
      TransactionType.transfer => '이체',
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
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
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
        const SizedBox(height: 20),

        // Transaction type section
        _SectionTitle(title: '거래 유형'),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
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
        const SizedBox(height: 20),

        // Account section
        _SectionTitle(title: '계좌'),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
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
        const SizedBox(height: 24),

        // Apply button
        MoreBottomButton(
          text: '적용',
          onPressed: () {
            Navigator.of(context).pop(_filter);
          },
        ),
        const SizedBox(height: 16),
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
      ),
    );
  }
}
