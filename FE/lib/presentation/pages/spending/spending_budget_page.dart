import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/core/utils/tutorial_service.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/spending_budget_viewmodel.dart';
import 'package:more_app/domain/entities/budget.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';
import 'package:showcaseview/showcaseview.dart';

class SpendingBudgetPage extends ConsumerStatefulWidget {
  const SpendingBudgetPage({super.key});

  @override
  ConsumerState<SpendingBudgetPage> createState() => _SpendingBudgetPageState();
}

class _SpendingBudgetPageState extends ConsumerState<SpendingBudgetPage> {
  bool _isFixedExpanded = false;
  bool _isVariableExpanded = false;

  BuildContext? _showcaseContext;
  final _keyIncome = GlobalKey();
  final _keySavings = GlobalKey();
  final _keyFixed = GlobalKey();
  final _keyVariable = GlobalKey();
  final _keyFreedom = GlobalKey();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final shown = await TutorialService.isShown(TutorialService.spendingBudget);
      if (!shown && mounted) {
        await TutorialService.markShown(TutorialService.spendingBudget);
        if (mounted && _showcaseContext != null) {
          ShowCaseWidget.of(_showcaseContext!).startShowCase([
            _keyIncome,
            _keySavings,
            _keyFixed,
            _keyVariable,
            _keyFreedom,
          ]);
        }
      }
    });
  }

  Future<void> _showEditBudgetDialog(int currentIncome) async {
    final newValue = await showDialog<int>(
      context: context,
      builder: (_) => _EditIncomeDialog(currentIncome: currentIncome),
    );

    if (newValue != null && mounted) {
      try {
        await ref.read(spendingBudgetViewModelProvider.notifier).saveIncome(newValue);
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('저장에 실패했습니다: $e')),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(spendingBudgetViewModelProvider);
    final setup = state.setup;
    final extraFunds = setup.remainingFreeAmount;

    return ShowCaseWidget(
      builder: (ctx) {
        _showcaseContext = ctx;
        return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(title: '', showBack: true, showNotification: false),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
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
                    Row(
                      children: [
                        Text('월 수입 · 주간 예산 설정', style: AppTextStyles.headingLg.copyWith(color: colors.textPrimary)),
                        const Spacer(),
                        GestureDetector(
                          onTap: () {
                            if (_showcaseContext != null) {
                              ShowCaseWidget.of(_showcaseContext!).startShowCase(
                                [_keyIncome, _keySavings, _keyFixed, _keyVariable, _keyFreedom],
                              );
                            }
                          },
                          child: Container(
                            width: 26.w,
                            height: 26.w,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: colors.borderDefault,
                            ),
                            child: Center(
                              child: Text(
                                '?',
                                style: TextStyle(
                                  color: colors.textSecondary,
                                  fontSize: 13.sp,
                                  fontWeight: FontWeight.w800,
                                  height: 1,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 12.h),
                    Text(
                      '월 수입을 기반으로 주간 예산 금액이 자동 배분 됩니다.',
                      style: AppTextStyles.bodySm.copyWith(
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
                          Showcase(
                            key: _keyIncome,
                            title: '월 수입',
                            titleTextStyle: AppTextStyles.bodyMd.copyWith(
                              fontWeight: FontWeight.w800,
                              fontSize: 18.sp,
                              color: AppColors.orange500,
                            ),
                            description: '월에 정기적으로 들어오는 금액을 입력해줘요.\n월 수입이 변경되면 연필 버튼을 눌러 수정해주세요.',
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  '월 수입',
                                  style: AppTextStyles.bodyMd.copyWith(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 17.sp,
                                    color: colors.textPrimary,
                                  ),
                                ),
                                Flexible(
                                  child: GestureDetector(
                                    onTap: () => _showEditBudgetDialog(setup.monthlyIncome),
                                    child: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        Icon(Icons.edit_outlined, size: 20, color: colors.textPrimary),
                                        SizedBox(width: 8.w),
                                        Flexible(
                                          child: FittedBox(
                                            fit: BoxFit.scaleDown,
                                            child: Text(
                                              CurrencyFormatter.formatKRW(setup.monthlyIncome),
                                              style: AppTextStyles.headingLg.copyWith(
                                                fontSize: 20.sp,
                                                fontWeight: FontWeight.bold,
                                                color: colors.textPrimary,
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
                          ),
                          SizedBox(height: 24.h),
                          Divider(color: colors.borderDefault, thickness: 1),
                          SizedBox(height: 16.h),

                          Text(
                            '주간 예산 금액',
                            style: AppTextStyles.bodyLg.copyWith(
                              fontWeight: FontWeight.bold,
                              fontSize: 18.sp,
                              color: colors.textPrimary,
                            ),
                          ),
                          SizedBox(height: 24.h),

                          Showcase(
                            key: _keySavings,
                            title: '저축액',
                            titleTextStyle: AppTextStyles.bodyMd.copyWith(
                              fontWeight: FontWeight.w800,
                              fontSize: 18.sp,
                              color: AppColors.orange500,
                            ),
                            description: '목표 설정했던 금액을 목표 기간에 따라 배분해 주별 저축되는 금액을 알려줘요.',
                            child: _BudgetSectionHeader(
                              label: '저축액',
                              description: '목표를 위해 저축하는 금액',
                              amount: setup.savingsAmount,
                              color: const Color(0xFF81C784),
                            ),
                          ),
                          SizedBox(height: 40.h),

                          Showcase(
                            key: _keyFixed,
                            title: '고정지출',
                            titleTextStyle: AppTextStyles.bodyMd.copyWith(
                              fontWeight: FontWeight.w800,
                              fontSize: 18.sp,
                              color: AppColors.orange500,
                            ),
                            description: '구독비 등 고정적으로 지출되는 것에 대한 금액을 주별로 배분해줘요.',
                            child: _BudgetSectionHeader(
                              label: '고정지출',
                              description: '매주 고정으로 나가는 비용',
                              amount: setup.fixedExpenseAmount,
                              color: colors.textSecondary,
                              isExpandable: true,
                              isExpanded: _isFixedExpanded,
                              onToggle: () => setState(() => _isFixedExpanded = !_isFixedExpanded),
                            ),
                          ),
                          if (_isFixedExpanded && setup.fixedEssentials.isNotEmpty) ...[
                            SizedBox(height: 16.h),
                            _EssentialCategoryList(
                              essentials: setup.fixedEssentials,
                              totalAmount: setup.fixedExpenseAmount,
                            ),
                          ],
                          SizedBox(height: 40.h),

                          Showcase(
                            key: _keyVariable,
                            title: '변동지출',
                            titleTextStyle: AppTextStyles.bodyMd.copyWith(
                              fontWeight: FontWeight.w800,
                              fontSize: 18.sp,
                              color: AppColors.orange500,
                            ),
                            description: '교통비, 식비 등 변동적으로 지출되는 것에 대한 금액을 주별로 배분해줘요.',
                            child: _BudgetSectionHeader(
                              label: '변동지출',
                              description: '매주 변동되는 비용 (병원비 등)',
                              amount: setup.variableExpenseAmount,
                              color: colors.textSecondary,
                              isExpandable: true,
                              isExpanded: _isVariableExpanded,
                              onToggle: () => setState(() => _isVariableExpanded = !_isVariableExpanded),
                            ),
                          ),
                          if (_isVariableExpanded && setup.variableEssentials.isNotEmpty) ...[
                            SizedBox(height: 16.h),
                            _EssentialCategoryList(
                              essentials: setup.variableEssentials,
                              totalAmount: setup.variableExpenseAmount,
                              isVariable: true,
                            ),
                          ],
                          SizedBox(height: 32.h),
                          Divider(color: colors.textPrimary, thickness: 1.5),
                          SizedBox(height: 32.h),

                          Showcase(
                            key: _keyFreedom,
                            title: '주간 여유 자금',
                            titleTextStyle: AppTextStyles.bodyMd.copyWith(
                              fontWeight: FontWeight.w800,
                              fontSize: 18.sp,
                              color: AppColors.orange500,
                            ),
                            description: '월 수입에서 저축액, 고정지출, 변동지출을 모두 빼고, 주에 자유롭게 사용 가능한 금액이에요.',
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  '= 주간 여유 자금',
                                  style: AppTextStyles.bodyMd.copyWith(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 18.sp,
                                    color: colors.textPrimary,
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
            text: state.isSaving ? '저장 중...' : '확인',
            onPressed: state.isSaving
                ? null
                : () async {
                    await ref.read(homeViewModelProvider.notifier).loadData();
                    if (context.mounted) context.pop();
                  },
          ),
        ],
      ),
      );
    },
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
                    style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold, fontSize: 17.sp, color: colors.textPrimary),
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
  final int totalAmount;
  final bool isVariable;

  const _EssentialCategoryList({
    required this.essentials,
    required this.totalAmount,
    this.isVariable = false,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Column(
      children: essentials.map((e) {
        final barRatio = totalAmount > 0
            ? (e.plannedAmount / totalAmount).clamp(0.0, 1.0)
            : 0.0;
        final isOver = isVariable && e.plannedAmount > 0 && e.actualSpentAmount > e.plannedAmount;

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
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Flexible(
                        child: Text(
                          '${CurrencyFormatter.formatKRW(e.plannedAmount)} / ${CurrencyFormatter.formatKRW(totalAmount)}',
                          overflow: TextOverflow.ellipsis,
                          style: AppTextStyles.caption.copyWith(
                            color: isOver ? colors.statusError : colors.textTertiary,
                            fontSize: 11.sp,
                          ),
                        ),
                      ),
                      SizedBox(width: 4.w),
                      Text(
                        '(${(barRatio * 100).toStringAsFixed(1)}%)',
                        style: AppTextStyles.caption.copyWith(
                          color: isOver ? Colors.red : AppColors.orange500,
                          fontSize: 11.sp,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              SizedBox(height: 6.h),
              MoreProgressBar(
                progress: barRatio,
                color: isOver ? Colors.red : AppColors.orange500,
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

class _EditIncomeDialog extends StatefulWidget {
  final int currentIncome;
  const _EditIncomeDialog({required this.currentIncome});

  @override
  State<_EditIncomeDialog> createState() => _EditIncomeDialogState();
}

class _EditIncomeDialogState extends State<_EditIncomeDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
    _controller.addListener(_formatListener);
    _controller.text = _formatWithComma(widget.currentIncome.toString());
  }

  @override
  void dispose() {
    _controller.removeListener(_formatListener);
    _controller.dispose();
    super.dispose();
  }

  String _formatWithComma(String value) {
    if (value.isEmpty) return '';
    final number = int.tryParse(value.replaceAll(',', '')) ?? 0;
    return number.toString().replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
      (Match m) => '${m[1]},',
    );
  }

  void _formatListener() {
    final text = _controller.text;
    final formatted = _formatWithComma(text);
    if (formatted != text) {
      _controller.value = TextEditingValue(
        text: formatted,
        selection: TextSelection.collapsed(offset: formatted.length),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return AlertDialog(
      backgroundColor: colors.bgSurface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16.r)),
      title: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text('총 예산 수정', style: AppTextStyles.headingMd.copyWith(color: colors.textPrimary)),
          IconButton(
            onPressed: () => _controller.text = _formatWithComma(widget.currentIncome.toString()),
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
            controller: _controller,
            keyboardType: TextInputType.number,
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
            final val = int.tryParse(_controller.text.replaceAll(',', ''));
            Navigator.pop(context, val);
          },
          child: const Text('저장', style: TextStyle(color: AppColors.orange500, fontWeight: FontWeight.bold)),
        ),
      ],
    );
  }
}
