import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
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
  List<TextEditingController> _categoryBudgetControllers = [];
  bool _controllersSynced = false;

  @override
  void initState() {
    super.initState();
    _incomeController = TextEditingController();
    _savingsGoalController = TextEditingController();
    _fixedExpenseController = TextEditingController();
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

  void _syncControllers(SpendingState state) {
    if (_controllersSynced || state.isLoading) {
      return;
    }

    _incomeController.text = _formatNumber(state.monthlyIncome);
    _savingsGoalController.text = _formatNumber(state.monthlySavingsGoal);
    _fixedExpenseController.text = _formatNumber(state.monthlyFixedExpense);

    for (final controller in _categoryBudgetControllers) {
      controller.dispose();
    }
    _categoryBudgetControllers = state.categories
        .map(
          (category) =>
              TextEditingController(text: _formatNumber(category.budget)),
        )
        .toList();
    _controllersSynced = true;
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
    if (available <= 0) {
      return 0;
    }
    return (available / 4.345).round();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(spendingViewModelProvider);
    _syncControllers(state);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '주간 예산 설정'),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : state.errorMessage != null
          ? Center(
              child: Text(
                '예산 데이터를 불러오지 못했습니다.\n${state.errorMessage!}',
                textAlign: TextAlign.center,
              ),
            )
          : Column(
              children: [
                Expanded(
                  child: SingleChildScrollView(
                    padding: EdgeInsets.symmetric(
                      horizontal: 16.w,
                      vertical: 12.h,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '주간 예산을 조정할 수 있어요. 조절된 주간 예산에 따라 소비 분석과 계획 수립을 다시 진행해요.',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textTertiary,
                            fontSize: 13.sp,
                          ),
                        ),
                        SizedBox(height: 16.h),
                        MoreTextField(
                          label: '총 예산(수입)',
                          hint: '월 수입을 입력하세요',
                          controller: _incomeController,
                          keyboardType: TextInputType.number,
                          suffix: '원',
                          onChanged: (_) => setState(() {}),
                        ),
                        SizedBox(height: 16.h),
                        MoreTextField(
                          label: '저축비',
                          hint: '저축 목표를 입력하세요',
                          controller: _savingsGoalController,
                          keyboardType: TextInputType.number,
                          suffix: '원',
                          onChanged: (_) => setState(() {}),
                        ),
                        SizedBox(height: 16.h),
                        MoreTextField(
                          label: '고정비',
                          hint: '고정 지출을 입력하세요',
                          controller: _fixedExpenseController,
                          keyboardType: TextInputType.number,
                          suffix: '원',
                          onChanged: (_) => setState(() {}),
                        ),
                        SizedBox(height: 20.h),
                        Divider(color: AppColors.borderDefault),
                        SizedBox(height: 20.h),
                        Text(
                          '카테고리별 예산',
                          style: AppTextStyles.headingMd.copyWith(
                            color: AppColors.textPrimary,
                            fontSize: 16.sp,
                          ),
                        ),
                        SizedBox(height: 16.h),
                        ...List.generate(state.categories.length, (index) {
                          final category = state.categories[index];
                          return Padding(
                            padding: EdgeInsets.only(bottom: 12.h),
                            child: Row(
                              children: [
                                Container(
                                  width: 36.w,
                                  height: 36.w,
                                  decoration: BoxDecoration(
                                    color: category.color.withValues(
                                      alpha: 0.15,
                                    ),
                                    shape: BoxShape.circle,
                                  ),
                                  child: Icon(
                                    category.icon,
                                    size: 18.w,
                                    color: category.color,
                                  ),
                                ),
                                SizedBox(width: 12.w),
                                SizedBox(
                                  width: 60.w,
                                  child: Text(
                                    category.name,
                                    style: AppTextStyles.bodyMd.copyWith(
                                      color: AppColors.textPrimary,
                                      fontSize: 14.sp,
                                    ),
                                  ),
                                ),
                                SizedBox(width: 12.w),
                                Expanded(
                                  child: MoreTextField(
                                    label: '예산',
                                    hint: '0',
                                    controller:
                                        _categoryBudgetControllers[index],
                                    keyboardType: TextInputType.number,
                                    suffix: '원',
                                  ),
                                ),
                              ],
                            ),
                          );
                        }),
                        SizedBox(height: 16.h),
                        MoreCard(
                          color: AppColors.orange50,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                '자유롭게 쓸 수 있는 돈',
                                style: AppTextStyles.bodyLg.copyWith(
                                  color: AppColors.textPrimary,
                                  fontWeight: FontWeight.w600,
                                  fontSize: 15.sp,
                                ),
                              ),
                              Text(
                                CurrencyFormatter.formatKRW(
                                  _calculatedWeeklyBudget,
                                ),
                                style: AppTextStyles.headingMd.copyWith(
                                  color: AppColors.orange500,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 18.sp,
                                ),
                              ),
                            ],
                          ),
                        ),
                        SizedBox(height: 24.h),
                      ],
                    ),
                  ),
                ),
                MoreBottomButton(
                  text: '저장하기',
                  onPressed: () async {
                    final updatedCategories = List<SpendingCategory>.generate(
                      state.categories.length,
                      (index) => state.categories[index].copyWith(
                        budget: _parseNumber(
                          _categoryBudgetControllers[index].text,
                        ),
                      ),
                    );

                    await ref
                        .read(spendingViewModelProvider.notifier)
                        .saveBudgetPlan(
                          monthlyIncome: _parseNumber(_incomeController.text),
                          monthlySavingsGoal: _parseNumber(
                            _savingsGoalController.text,
                          ),
                          monthlyFixedExpense: _parseNumber(
                            _fixedExpenseController.text,
                          ),
                          categories: updatedCategories,
                        );
                    await ref.read(homeViewModelProvider.notifier).loadData();

                    if (!context.mounted) {
                      return;
                    }
                    context.pop();
                  },
                ),
              ],
            ),
    );
  }
}
