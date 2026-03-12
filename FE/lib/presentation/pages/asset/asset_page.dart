import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/presentation/pages/asset/widgets/transaction_tile.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class AssetPage extends ConsumerWidget {
  const AssetPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(accountViewModelProvider);
    final groupedTransactions = _groupTransactionsByDate(state.transactions);

    if (state.isLoading) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: MoreAppBar(
          title: '내 자산',
          actions: [
            IconButton(
              icon: Icon(Icons.search, size: 24.w),
              onPressed: () {
                // TODO: search
              },
            ),
          ],
        ),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (state.errorMessage != null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: const MoreAppBar(title: '내 자산'),
        body: Center(
          child: Text(
            '자산 데이터를 불러오지 못했습니다.\n${state.errorMessage!}',
            textAlign: TextAlign.center,
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '내 자산',
        actions: [
          IconButton(
            icon: Icon(Icons.settings, size: 24.w),
            onPressed: () {
              // TODO: filter settings
            },
          ),
          IconButton(
            icon: Icon(Icons.search, size: 24.w),
            onPressed: () {
              // TODO: search
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Banner card
            MoreCard(
              color: AppColors.orange50,
              onTap: () => context.push(AppRoutes.spendingTab),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      '소비패턴 알아보기',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.orange500,
                        fontWeight: FontWeight.w600,
                        fontSize: 14.sp,
                      ),
                    ),
                  ),
                  Icon(
                    Icons.chevron_right,
                    color: AppColors.orange500,
                    size: 20.w,
                  ),
                ],
              ),
            ),
            SizedBox(height: 20.h),

            // Total asset display
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 4.w),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '총 자산',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.textTertiary,
                      fontSize: 13.sp,
                    ),
                  ),
                  SizedBox(height: 4.h),
                  Text(
                    CurrencyFormatter.formatKRW(state.totalAsset),
                    style: AppTextStyles.headingLg.copyWith(
                      color: AppColors.textPrimary,
                      fontSize: 24.sp,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 24.h),

            // Date-grouped transaction list
            ...groupedTransactions.entries.map((entry) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: EdgeInsets.symmetric(
                      horizontal: 4.w,
                      vertical: 8.h,
                    ),
                    child: Text(
                      entry.key,
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                        fontWeight: FontWeight.w600,
                        fontSize: 12.sp,
                      ),
                    ),
                  ),
                  MoreCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: [
                        for (int i = 0; i < entry.value.length; i++) ...[
                          if (i > 0)
                            Divider(
                              height: 1,
                              color: AppColors.borderDefault,
                            ),
                          TransactionTile(
                            transaction: entry.value[i],
                            onTap: () {
                              context.push(
                                AppRoutes.assetDetailLocation(
                                  entry.value[i].id,
                                ),
                              );
                            },
                          ),
                        ],
                      ],
                    ),
                  ),
                  SizedBox(height: 12.h),
                ],
              );
            }),
            SizedBox(height: 24.h),
          ],
        ),
      ),
    );
  }

  Map<String, List<Transaction>> _groupTransactionsByDate(
    List<Transaction> transactions,
  ) {
    final Map<String, List<Transaction>> grouped = {};
    final dateFormat = DateFormat('M월 d일 (E)', 'ko_KR');

    for (final transaction in transactions) {
      final dateKey = dateFormat.format(transaction.dateTime);
      grouped.putIfAbsent(dateKey, () => []);
      grouped[dateKey]!.add(transaction);
    }

    return grouped;
  }
}
