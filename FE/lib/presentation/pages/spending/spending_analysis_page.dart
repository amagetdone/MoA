import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_chip.dart';
import 'package:more_app/presentation/pages/spending/widgets/spending_line_chart.dart';
import 'package:more_app/presentation/pages/spending/widgets/category_list.dart';
import 'package:more_app/presentation/pages/spending/widgets/spending_chart.dart';

class SpendingAnalysisPage extends ConsumerWidget {
  const SpendingAnalysisPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(spendingViewModelProvider);
    final viewModel = ref.read(spendingViewModelProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '소비',
        showBack: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.tune),
            onPressed: () {
              context.push('/spending/budget');
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Period toggle chips
            Row(
              children: [
                MoreChip(
                  label: '주간',
                  selected: state.isWeekly,
                  onTap: () => viewModel.togglePeriod(true),
                ),
                const SizedBox(width: 8),
                MoreChip(
                  label: '월간',
                  selected: !state.isWeekly,
                  onTap: () => viewModel.togglePeriod(false),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Summary section
            MoreCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '지출 내역',
                    style: AppTextStyles.headingMd.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    CurrencyFormatter.formatKRW(state.totalSpent),
                    style: AppTextStyles.headingXl.copyWith(
                      color: AppColors.orange500,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Line chart
            SpendingLineChart(
              data: state.isWeekly
                  ? state.dailySpendings
                  : state.monthlySpendings,
              isWeekly: state.isWeekly,
            ),
            const SizedBox(height: 16),

            // Category section header
            Padding(
              padding: const EdgeInsets.only(left: 4, bottom: 12),
              child: Text(
                '카테고리별 지출',
                style: AppTextStyles.headingMd.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
            ),

            // Category list
            CategoryList(categories: state.categories),
            const SizedBox(height: 16),

            // Donut chart
            SpendingDonutChart(categories: state.categories),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}
