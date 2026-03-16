import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/utils/spending_ui_mapper.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class SpendingBudgetPage extends ConsumerStatefulWidget {
  const SpendingBudgetPage({super.key});

  @override
  ConsumerState<SpendingBudgetPage> createState() => _SpendingBudgetPageState();
}

class _SpendingBudgetPageState extends ConsumerState<SpendingBudgetPage> {
  int totalBudget = 0;
  int savings = 0;
  int fixedExpenses = 0;
  int variableExpenses = 0;
  List<SpendingCategory> categories = [];
  bool _isInitialized = false;

  void _initializeState(SpendingState state) {
    if (_isInitialized || state.isLoading) return;
    
    totalBudget = state.monthlyIncome;
    savings = state.monthlySavingsGoal;
    fixedExpenses = state.monthlyFixedExpense;
    categories = [...state.categories];
    variableExpenses = totalBudget - savings - fixedExpenses;
    if (variableExpenses < 0) variableExpenses = 0;
    
    _isInitialized = true;
  }

  int get extraFunds => totalBudget - savings - fixedExpenses - variableExpenses;

  String _formatWithComma(String value) {
    if (value.isEmpty) return "";
    final number = int.tryParse(value.replaceAll(',', '')) ?? 0;
    return number.toString().replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
      (Match m) => '${m[1]},',
    );
  }

  void _showEditBudgetDialog() {
    final int originalBudget = totalBudget;
    final controller = TextEditingController();

    void formatListener() {
      final text = controller.text;
      final formatted = _formatWithComma(text);
      if (formatted != text) {
        controller.value = TextEditingValue(
          text: formatted,
          selection: TextSelection.collapsed(offset: formatted.length),
        );
      }
    }
    controller.addListener(formatListener);
    controller.text = _formatWithComma(originalBudget.toString());

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16.r)),
        title: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('총 예산 수정', style: AppTextStyles.headingMd),
            IconButton(
              onPressed: () => controller.text = _formatWithComma(originalBudget.toString()),
              icon: const Icon(Icons.refresh, color: AppColors.textTertiary),
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('한 달 총 수입(예산)을 입력해주세요.', style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary)),
            SizedBox(height: 16.h),
            TextField(
              controller: controller,
              keyboardType: TextInputType.number,
              autofocus: true,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: InputDecoration(
                suffixText: '원',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8.r)),
                contentPadding: EdgeInsets.symmetric(horizontal: 12.w),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소', style: TextStyle(color: AppColors.textTertiary)),
          ),
          TextButton(
            onPressed: () {
              final newValue = int.tryParse(controller.text.replaceAll(',', ''));
              if (newValue != null) {
                setState(() => totalBudget = newValue);
              }
              Navigator.pop(context);
            },
            child: const Text('저장', style: TextStyle(color: AppColors.orange500, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    ).whenComplete(() => controller.dispose());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(spendingViewModelProvider);
    _initializeState(state);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '',
        showBack: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications_none_outlined, size: 24),
            onPressed: () {},
          ),
        ],
      ),
      body: state.isLoading
          ? const SizedBox.shrink()
          : Column(
              children: [
                Expanded(
                  child: SingleChildScrollView(
                    physics: const BouncingScrollPhysics(),
                    padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 12.h),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('주간 예산 설정', style: AppTextStyles.headingLg),
                        SizedBox(height: 12.h),
                        Text(
                          '주간 예산을 조정할 수 있어요. 조정된 주간 예산에 따라 소비\n분석과 계획 수립을 다시 진행해요.',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textTertiary,
                            fontSize: 14.sp,
                            height: 1.4,
                          ),
                        ),
                        SizedBox(height: 24.h),

                        MoreCard(
                          padding: EdgeInsets.all(24.w),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    '총 예산 (수입)',
                                    style: AppTextStyles.bodyMd.copyWith(
                                      fontWeight: FontWeight.w600,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                  Flexible(
                                    child: GestureDetector(
                                      onTap: _showEditBudgetDialog,
                                      child: Row(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          const Icon(Icons.edit_outlined, size: 20, color: Colors.black),
                                          SizedBox(width: 8.w),
                                          Flexible(
                                            child: FittedBox(
                                              fit: BoxFit.scaleDown,
                                              child: Text(
                                                CurrencyFormatter.formatKRW(totalBudget),
                                                style: AppTextStyles.headingLg.copyWith(
                                                  fontSize: 26.sp,
                                                  fontWeight: FontWeight.bold,
                                                ),
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              SizedBox(height: 24.h),
                              const Divider(color: Color(0xFFEEEEEE), thickness: 1),
                              SizedBox(height: 32.h),

                              _BudgetSectionHeader(
                                label: '저축비',
                                description: '목표를 위해 저축하는 금액',
                                amount: savings,
                                color: const Color(0xFF81C784),
                              ),
                              SizedBox(height: 40.h),

                              _BudgetSectionHeader(
                                label: '고정비',
                                description: '매주 고정으로 나가는 비용',
                                amount: fixedExpenses,
                                color: AppColors.orange500,
                                isExpandable: true,
                                isExpanded: true,
                                child: Column(
                                  children: categories.map((cat) => 
                                    _SubCategoryItem(
                                      label: cat.name, 
                                      amount: cat.budget, 
                                      progress: fixedExpenses > 0 ? cat.budget / fixedExpenses : 0
                                    )
                                  ).toList(),
                                ),
                              ),
                              SizedBox(height: 40.h),

                              _BudgetSectionHeader(
                                label: '변동비',
                                description: '매주 변동되는 비용 (병원비 등)',
                                amount: variableExpenses,
                                color: const Color(0xFF333333),
                                isExpandable: true,
                                isExpanded: true,
                              ),
                              SizedBox(height: 32.h),
                              const Divider(color: Color(0xFF333333), thickness: 1.5),
                              SizedBox(height: 32.h),

                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    '= 여유 자금',
                                    style: AppTextStyles.bodyMd.copyWith(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 18.sp,
                                    ),
                                  ),
                                  FittedBox(
                                    fit: BoxFit.scaleDown,
                                    child: Text(
                                      CurrencyFormatter.formatKRW(extraFunds),
                                      style: AppTextStyles.headingMd.copyWith(
                                        color: AppColors.orange500,
                                        fontSize: 22.sp,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              SizedBox(height: 8.h),
                              Text(
                                '고정/변동/저축비를 제외한 자유롭게 사용 가능한 금액',
                                style: AppTextStyles.caption.copyWith(
                                  color: AppColors.textTertiary,
                                  fontSize: 11.sp,
                                ),
                              ),
                            ],
                          ),
                        ),
                        SizedBox(height: 40.h),
                      ],
                    ),
                  ),
                ),
                MoreBottomButton(
                  text: '저장하기',
                  onPressed: () async {
                    await ref.read(spendingViewModelProvider.notifier).saveBudgetPlan(
                      monthlyIncome: totalBudget,
                      monthlySavingsGoal: savings,
                      monthlyFixedExpense: fixedExpenses,
                      categories: categories,
                    );
                    await ref.read(homeViewModelProvider.notifier).loadData();
                    if (context.mounted) context.pop();
                  },
                ),
              ],
            ),
    );
  }
}

class _BudgetSectionHeader extends StatelessWidget {
  final String label;
  final String description;
  final int amount;
  final Color color;
  final bool isExpandable;
  final bool isExpanded;
  final Widget? child;

  const _BudgetSectionHeader({
    required this.label,
    required this.description,
    required this.amount,
    required this.color,
    this.isExpandable = false,
    this.isExpanded = false,
    this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                Icon(Icons.arrow_right_alt, color: AppColors.textPrimary, size: 22.w),
                SizedBox(width: 6.w),
                Text(
                  label,
                  style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold, fontSize: 17.sp),
                ),
                if (isExpandable) 
                  Icon(isExpanded ? Icons.arrow_drop_down : Icons.arrow_right, size: 24.w),
              ],
            ),
            FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(
                CurrencyFormatter.formatKRW(amount),
                style: AppTextStyles.bodyMd.copyWith(color: color, fontWeight: FontWeight.bold, fontSize: 17.sp),
              ),
            ),
          ],
        ),
        SizedBox(height: 4.h),
        Text(
          description,
          style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary, fontSize: 12.sp),
        ),
        if (child != null) ...[
          SizedBox(height: 24.h),
          Padding(
            padding: EdgeInsets.only(left: 16.w),
            child: child!,
          ),
        ],
      ],
    );
  }
}

class _SubCategoryItem extends StatelessWidget {
  final String label;
  final int amount;
  final double progress;

  const _SubCategoryItem({
    required this.label,
    required this.amount,
    required this.progress,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: 20.h),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          SizedBox(
            width: 60.w,
            child: Text(
              label,
              style: AppTextStyles.bodySm.copyWith(color: AppColors.textSecondary, fontSize: 15.sp),
            ),
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  CurrencyFormatter.formatKRW(amount),
                  style: AppTextStyles.caption.copyWith(color: const Color(0xFFAAAAAA), fontSize: 13.sp),
                ),
                SizedBox(height: 8.h),
                Stack(
                  alignment: Alignment.centerLeft,
                  children: [
                    Container(
                      height: 12.h,
                      width: double.infinity,
                      decoration: BoxDecoration(color: const Color(0xFFF0F0F0), borderRadius: BorderRadius.circular(6.r)),
                    ),
                    LayoutBuilder(
                      builder: (context, constraints) {
                        return Container(
                          height: 12.h,
                          width: constraints.maxWidth * progress.clamp(0.0, 1.0),
                          decoration: BoxDecoration(color: AppColors.orange500, borderRadius: BorderRadius.circular(6.r)),
                        );
                      },
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
