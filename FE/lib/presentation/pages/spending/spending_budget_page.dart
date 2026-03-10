import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';

class SpendingBudgetPage extends ConsumerStatefulWidget {
  const SpendingBudgetPage({super.key});

  @override
  ConsumerState<SpendingBudgetPage> createState() => _SpendingBudgetPageState();
}

class _SpendingBudgetPageState extends ConsumerState<SpendingBudgetPage> {
  late TextEditingController _incomeController;
  late TextEditingController _savingsGoalController;
  late TextEditingController _fixedExpenseController;
  late List<TextEditingController> _categoryBudgetControllers;

  @override
  void initState() {
    super.initState();
    final state = ref.read(spendingViewModelProvider);

    _incomeController = TextEditingController(
      text: _formatNumber(state.monthlyIncome),
    );
    _savingsGoalController = TextEditingController(
      text: _formatNumber(state.monthlySavingsGoal),
    );
    _fixedExpenseController = TextEditingController(
      text: _formatNumber(state.monthlyFixedExpense),
    );
    _categoryBudgetControllers = state.categories.map((category) {
      return TextEditingController(
        text: _formatNumber(category.budget),
      );
    }).toList();
  }

  @override
  void dispose() {
    _incomeController.dispose();
    _savingsGoalController.dispose();
    _fixedExpenseController.dispose();
    for (final controller in _categoryBudgetControllers) {
      controller.dispose();
    }
    super.dispose();
  }

  String _formatNumber(int value) {
    return value.toString().replaceAllMapped(
      RegExp(r'(\d)(?=(\d{3})+(?!\d))'),
      (match) => '${match[1]},',
    );
  }

  int _parseNumber(String text) {
    final cleaned = text.replaceAll(RegExp(r'[^0-9]'), '');
    return int.tryParse(cleaned) ?? 0;
  }

  int get _calculatedWeeklyBudget {
    final income = _parseNumber(_incomeController.text);
    final savings = _parseNumber(_savingsGoalController.text);
    final fixed = _parseNumber(_fixedExpenseController.text);
    final available = income - savings - fixed;
    if (available <= 0) return 0;
    return (available / 4.345).round(); // average weeks per month
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(spendingViewModelProvider);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(
        title: '주간 예산 설정',
      ),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Monthly income
                  MoreTextField(
                    label: '월 수입',
                    hint: '월 수입을 입력하세요',
                    controller: _incomeController,
                    keyboardType: TextInputType.number,
                    suffix: '원',
                    onChanged: (_) => setState(() {}),
                  ),
                  const SizedBox(height: 16),

                  // Savings goal
                  MoreTextField(
                    label: '저축 목표',
                    hint: '저축 목표를 입력하세요',
                    controller: _savingsGoalController,
                    keyboardType: TextInputType.number,
                    suffix: '원',
                    onChanged: (_) => setState(() {}),
                  ),
                  const SizedBox(height: 16),

                  // Fixed expenses
                  MoreTextField(
                    label: '고정 지출',
                    hint: '고정 지출을 입력하세요',
                    controller: _fixedExpenseController,
                    keyboardType: TextInputType.number,
                    suffix: '원',
                    onChanged: (_) => setState(() {}),
                  ),
                  const SizedBox(height: 20),

                  const Divider(color: AppColors.borderDefault),
                  const SizedBox(height: 20),

                  // Category budgets section
                  Text(
                    '카테고리별 예산',
                    style: AppTextStyles.headingMd.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Category budget rows
                  ...List.generate(state.categories.length, (index) {
                    final category = state.categories[index];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Row(
                        children: [
                          Container(
                            width: 36,
                            height: 36,
                            decoration: BoxDecoration(
                              color: category.color.withValues(alpha: 0.15),
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              category.icon,
                              size: 18,
                              color: category.color,
                            ),
                          ),
                          const SizedBox(width: 12),
                          SizedBox(
                            width: 60,
                            child: Text(
                              category.name,
                              style: AppTextStyles.bodyMd.copyWith(
                                color: AppColors.textPrimary,
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: MoreTextField(
                              label: '예산',
                              hint: '0',
                              controller: index < _categoryBudgetControllers.length
                                  ? _categoryBudgetControllers[index]
                                  : TextEditingController(),
                              keyboardType: TextInputType.number,
                              suffix: '원',
                            ),
                          ),
                        ],
                      ),
                    );
                  }),
                  const SizedBox(height: 16),

                  // Calculated weekly budget
                  MoreCard(
                    color: AppColors.orange50,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          '계산된 주간 예산',
                          style: AppTextStyles.bodyLg.copyWith(
                            color: AppColors.textPrimary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Text(
                          CurrencyFormatter.formatKRW(_calculatedWeeklyBudget),
                          style: AppTextStyles.headingMd.copyWith(
                            color: AppColors.orange500,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),

          // Bottom save button
          MoreBottomButton(
            text: '저장',
            onPressed: () {
              context.pop();
            },
          ),
        ],
      ),
    );
  }
}
