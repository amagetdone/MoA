import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/spending_budget_viewmodel.dart';
import 'package:more_app/domain/entities/budget.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class SpendingBudgetPage extends ConsumerStatefulWidget {
  const SpendingBudgetPage({super.key});

  @override
  ConsumerState<SpendingBudgetPage> createState() => _SpendingBudgetPageState();
}

class _SpendingBudgetPageState extends ConsumerState<SpendingBudgetPage> {
  int? _editedIncome;
  bool _isFixedExpanded = false;
  bool _isVariableExpanded = false;

  String _formatWithComma(String value) {
    if (value.isEmpty) return '';
    final number = int.tryParse(value.replaceAll(',', '')) ?? 0;
    return number.toString().replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
      (Match m) => '${m[1]},',
    );
  }

  void _showEditBudgetDialog(int currentIncome) {
    final colors = AppColors.of(context);
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
    controller.text = _formatWithComma(currentIncome.toString());

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: colors.bgSurface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16.r)),
        title: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('총 예산 수정', style: AppTextStyles.headingMd.copyWith(color: colors.textPrimary)),
            IconButton(
              onPressed: () => controller.text = _formatWithComma(currentIncome.toString()),
              icon: Icon(Icons.refresh, color: colors.textTertiary),
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('한 달 총 수입(예산)을 입력해주세요.', style: AppTextStyles.bodySm.copyWith(color: colors.textTertiary)),
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
            child: Text('취소', style: TextStyle(color: colors.textTertiary)),
          ),
          TextButton(
            onPressed: () {
              final newValue = int.tryParse(controller.text.replaceAll(',', ''));
              if (newValue != null) setState(() => _editedIncome = newValue);
              Navigator.pop(context);
            },
            child: const Text('저장', style: TextStyle(color: AppColors.orange500, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    ).whenComplete(() {
      controller.removeListener(formatListener);
      controller.dispose();
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(spendingBudgetViewModelProvider);

    if (state.isLoading) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(title: '', showBack: true, showNotification: true),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    final setup = state.setup;
    final displayIncome = _editedIncome ?? setup.monthlyIncome;
    // 수입 수정 중이면 로컬 계산, 아니면 서버의 실제 잔여 여유자금 사용
    final extraFunds = _editedIncome != null
        ? displayIncome - setup.savingsAmount - setup.fixedExpenseAmount - setup.variableExpenseAmount
        : setup.remainingFreeAmount;

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(title: '', showBack: true, showNotification: true),
      body: Column(
        children: [
          Expanded(
            child: Scrollbar(
              thumbVisibility: false,
              child: SingleChildScrollView(
                physics: const BouncingScrollPhysics(),
                padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 12.h),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('주간 예산 설정', style: AppTextStyles.headingMd.copyWith(color: colors.textPrimary, fontSize: 20.sp, fontWeight: FontWeight.bold)),
                    SizedBox(height: 4.h),
                    Text(
                      '주간 예산에 따라 소비분석과 계획 수립을 진행합니다.',
                      style: AppTextStyles.caption.copyWith(
                        color: colors.textTertiary,
                        fontSize: 12.sp,
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
                                  color: colors.textPrimary,
                                ),
                              ),
                              Flexible(
                                child: GestureDetector(
                                  onTap: () => _showEditBudgetDialog(displayIncome),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(Icons.edit_outlined, size: 20, color: colors.textPrimary),
                                      SizedBox(width: 8.w),
                                      Flexible(
                                        child: FittedBox(
                                          fit: BoxFit.scaleDown,
                                          child: Text(
                                            CurrencyFormatter.formatKRW(displayIncome),
                                            style: AppTextStyles.headingLg.copyWith(
                                              color: colors.textPrimary,
                                              fontSize: 20.sp,
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
                          Divider(color: colors.borderDefault, thickness: 1),
                          SizedBox(height: 32.h),

                          _BudgetSectionHeader(
                            label: '저축액',
                            description: '목표를 위해 저축하는 금액',
                            amount: setup.savingsAmount,
                            color: const Color(0xFF81C784),
                          ),
                          SizedBox(height: 40.h),

                          _BudgetSectionHeader(
                            label: '고정지출',
                            description: '매주 고정으로 나가는 비용',
                            amount: setup.fixedExpenseAmount,
                            color: AppColors.orange500,
                            isExpandable: true,
                            isExpanded: _isFixedExpanded,
                            onToggle: () => setState(() => _isFixedExpanded = !_isFixedExpanded),
                          ),
                          if (_isFixedExpanded && setup.fixedEssentials.isNotEmpty) ...[
                            SizedBox(height: 16.h),
                            _EssentialCategoryList(essentials: setup.fixedEssentials, sectionTotal: setup.fixedExpenseAmount),
                          ],
                          SizedBox(height: 40.h),

                          _BudgetSectionHeader(
                            label: '변동지출',
                            description: '매주 변동되는 비용 (병원비 등)',
                            amount: setup.variableExpenseAmount,
                            color: const Color(0xFF333333),
                            isExpandable: true,
                            isExpanded: _isVariableExpanded,
                            onToggle: () => setState(() => _isVariableExpanded = !_isVariableExpanded),
                          ),
                          if (_isVariableExpanded && setup.variableEssentials.isNotEmpty) ...[
                            SizedBox(height: 16.h),
                            _EssentialCategoryList(essentials: setup.variableEssentials, sectionTotal: setup.variableExpenseAmount),
                          ],
                          SizedBox(height: 32.h),
                          const Divider(color: Color(0xFF333333), thickness: 1.5),
                          SizedBox(height: 32.h),

                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                '= 여유 자금',
                                style: AppTextStyles.bodyMd.copyWith(
                                  color: colors.textPrimary,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 18.sp,
                                ),
                              ),
                              FittedBox(
                                fit: BoxFit.scaleDown,
                                child: Text(
                                  CurrencyFormatter.formatKRW(extraFunds < 0 ? 0 : extraFunds),
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
                              color: colors.textTertiary,
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
          ),
          MoreBottomButton(
            text: state.isSaving ? '저장 중...' : '저장하기',
            onPressed: state.isSaving
                ? null
                : () async {
                    if (_editedIncome != null) {
                      try {
                        await ref
                            .read(spendingBudgetViewModelProvider.notifier)
                            .saveIncome(_editedIncome!);
                        _editedIncome = null;
                      } catch (e) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text('저장에 실패했습니다: $e')),
                          );
                        }
                        return;
                      }
                    }
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
  final VoidCallback? onToggle;

  const _BudgetSectionHeader({
    required this.label,
    required this.description,
    required this.amount,
    required this.color,
    this.isExpandable = false,
    this.isExpanded = false,
    this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GestureDetector(
          onTap: isExpandable ? onToggle : null,
          behavior: HitTestBehavior.opaque,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(Icons.arrow_right_alt, color: colors.textPrimary, size: 22.w),
                  SizedBox(width: 6.w),
                  Text(
                    label,
                    style: AppTextStyles.bodyMd.copyWith(color: colors.textPrimary, fontWeight: FontWeight.bold, fontSize: 17.sp),
                  ),
                  if (isExpandable) ...[
                    SizedBox(width: 4.w),
                    Icon(
                      isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
                      color: colors.textTertiary,
                      size: 20.w,
                    ),
                  ],
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
        ),
        SizedBox(height: 4.h),
        Text(
          description,
          style: AppTextStyles.caption.copyWith(color: colors.textTertiary, fontSize: 12.sp),
        ),
      ],
    );
  }
}

class _EssentialCategoryList extends StatelessWidget {
  final List<BudgetEssential> essentials;
  final int sectionTotal;

  const _EssentialCategoryList({required this.essentials, required this.sectionTotal});

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Column(
      children: essentials.map((e) {
        final ratio = sectionTotal > 0
            ? (e.plannedAmount / sectionTotal).clamp(0.0, 1.0)
            : 0.0;
        final isOver = e.plannedAmount > 0 && e.actualSpentAmount > e.plannedAmount;

        return Padding(
          padding: EdgeInsets.only(bottom: 16.h),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    e.categoryName,
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textSecondary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  Text(
                    '${CurrencyFormatter.formatKRW(e.plannedAmount)} / ${CurrencyFormatter.formatKRW(sectionTotal)}',
                    style: AppTextStyles.caption.copyWith(
                      color: isOver ? AppColors.red500 : colors.textTertiary,
                      fontSize: 11.sp,
                    ),
                  ),
                ],
              ),
              SizedBox(height: 6.h),
              MoreProgressBar(
                progress: ratio,
                color: isOver ? AppColors.red500 : AppColors.orange500,
                backgroundColor: colors.gray50,
                height: 8.h,
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}