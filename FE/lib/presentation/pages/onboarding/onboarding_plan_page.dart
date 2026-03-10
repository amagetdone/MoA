import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class OnboardingPlanPage extends ConsumerWidget {
  const OnboardingPlanPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(spendingViewModelProvider);
    final weeklyBudget = state.weeklyBudget;
    final monthlySavingsGoal = state.monthlySavingsGoal;
    final weeklySavingsGoal = (monthlySavingsGoal / 4).round();
    final categories = state.categories;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: const MoreAppBar(title: '목표 계획', showBack: true),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (state.isLoading)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 48),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (state.errorMessage != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 24),
                child: Text(
                  '계획 데이터를 불러오지 못했습니다.\n${state.errorMessage!}',
                  style: AppTextStyles.bodySm.copyWith(color: AppColors.red500),
                ),
              ),
            Text(
              '주간 미션을 확인하세요',
              style: AppTextStyles.headingLg.copyWith(color: AppColors.gray900),
            ),
            const SizedBox(height: 8),
            Text(
              '분석 결과를 바탕으로 맞춤 계획을 세웠어요',
              style: AppTextStyles.bodySm.copyWith(color: AppColors.gray500),
            ),
            const SizedBox(height: 24),
            MoreCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: AppColors.orange500.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(
                          Icons.account_balance_wallet_outlined,
                          color: AppColors.orange500,
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Text(
                        '주간 자유소비 예산',
                        style: AppTextStyles.bodyLg.copyWith(
                          color: AppColors.gray700,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(
                    _formatAmount(weeklyBudget.round()),
                    style: AppTextStyles.headingLg.copyWith(
                      color: AppColors.orange500,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            MoreCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: const Color(0xFF00BCD4).withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(
                          Icons.savings_outlined,
                          color: Color(0xFF00BCD4),
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Text(
                        '주간 저축 목표',
                        style: AppTextStyles.bodyLg.copyWith(
                          color: AppColors.gray700,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(
                    _formatAmount(weeklySavingsGoal),
                    style: AppTextStyles.headingLg.copyWith(
                      color: const Color(0xFF00BCD4),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            MoreCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: const Color(0xFF4CAF50).withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(
                          Icons.pie_chart_outline,
                          color: Color(0xFF4CAF50),
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Text(
                        '카테고리별 예산 배분',
                        style: AppTextStyles.bodyLg.copyWith(
                          color: AppColors.gray700,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  ...categories.map((category) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Row(
                        children: [
                          Container(
                            width: 8,
                            height: 8,
                            decoration: BoxDecoration(
                              color: category.color,
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              category.name,
                              style: AppTextStyles.bodySm.copyWith(
                                color: AppColors.gray700,
                              ),
                            ),
                          ),
                          Text(
                            _formatAmount(category.budget.round()),
                            style: AppTextStyles.bodySm.copyWith(
                              color: AppColors.gray900,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    );
                  }),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: MoreBottomButton(
            text: '시작하기',
            onPressed: () => context.go(AppRoutes.home),
            enabled: true,
          ),
        ),
      ),
    );
  }

  static String _formatAmount(int amount) {
    final str = amount.toString();
    final buffer = StringBuffer();
    var count = 0;
    for (var i = str.length - 1; i >= 0; i--) {
      buffer.write(str[i]);
      count++;
      if (count % 3 == 0 && i > 0) {
        buffer.write(',');
      }
    }
    return '${buffer.toString().split('').reversed.join()}원';
  }
}
