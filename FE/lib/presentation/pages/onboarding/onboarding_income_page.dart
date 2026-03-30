import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/onboarding_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';

class OnboardingIncomePage extends ConsumerStatefulWidget {
  const OnboardingIncomePage({super.key});

  @override
  ConsumerState<OnboardingIncomePage> createState() =>
      _OnboardingIncomePageState();
}

class _OnboardingIncomePageState extends ConsumerState<OnboardingIncomePage> {
  final TextEditingController _incomeController = TextEditingController();
  bool _isEditing = false;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadIncome();
  }

  Future<void> _loadIncome() async {
    try {
      final income =
          await ref.read(onboardingViewModelProvider.notifier).loadIncome();
      if (mounted) {
        setState(() {
          _incomeController.text = CurrencyFormatter.parseFormatted(
                      income.toString()) !=
                  null
              ? _formatNumber(income)
              : '0';
        });
      }
    } catch (_) {
      // GET 실패 시 분석 수입으로 대체
      final analysisIncome =
          ref.read(onboardingViewModelProvider).analysisResult?.averageIncome;
      if (mounted && analysisIncome != null) {
        setState(() {
          _incomeController.text = _formatNumber(analysisIncome);
        });
      }
    }
  }

  String _formatNumber(int amount) {
    return amount.toString().replaceAllMapped(
          RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (m) => '${m[1]},',
        );
  }

  Future<void> _proceed() async {
    final income = CurrencyFormatter.parseFormatted(_incomeController.text) ?? 0;
    setState(() => _isLoading = true);
    try {
      await ref.read(onboardingViewModelProvider.notifier).saveIncome(income);
      if (mounted) {
        context.go(AppRoutes.onboardingAnalysis);
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('수입 저장에 실패했습니다.')),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  void dispose() {
    _incomeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final onboardingState = ref.watch(onboardingViewModelProvider);
    final isSaving = _isLoading || onboardingState.isLoading;

    return Scaffold(
      backgroundColor: colors.bgSurface,
      appBar: MoreAppBar(
        title: '계획 수립 중',
        showBack: true,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: double.infinity,
              padding: EdgeInsets.all(20.w),
              decoration: BoxDecoration(
                color: colors.gray50,
                borderRadius: BorderRadius.circular(16.r),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '수입을 확인해주세요.',
                    style: AppTextStyles.headingMd.copyWith(
                      color: colors.gray900,
                      fontSize: 18.sp,
                    ),
                  ),
                  SizedBox(height: 8.h),
                  Text(
                    '분석한 월 평균 수입이에요. 다르다면 직접 수정해주세요.',
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.gray500,
                      fontSize: 13.sp,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 32.h),
            Text(
              '월 평균 수입',
              style: AppTextStyles.bodySm.copyWith(
                color: colors.gray700,
                fontWeight: FontWeight.w600,
                fontSize: 13.sp,
              ),
            ),
            SizedBox(height: 8.h),
            TextField(
              controller: _incomeController,
              keyboardType: TextInputType.number,
              readOnly: !_isEditing,
              inputFormatters: [CurrencyInputFormatter()],
              style: AppTextStyles.headingLg.copyWith(
                color: colors.gray900,
                fontSize: 24.sp,
              ),
              decoration: InputDecoration(
                suffixText: '원',
                suffixStyle: AppTextStyles.headingMd.copyWith(
                  color: colors.gray500,
                  fontSize: 18.sp,
                ),
                filled: true,
                fillColor: colors.gray50,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12.r),
                  borderSide: BorderSide.none,
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12.r),
                  borderSide: BorderSide(
                    color: AppColors.orange500,
                    width: 1.5,
                  ),
                ),
                contentPadding: EdgeInsets.symmetric(
                  horizontal: 16.w,
                  vertical: 16.h,
                ),
              ),
              onChanged: (_) => setState(() {}),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: Row(
            children: [
              Expanded(
                child: SizedBox(
                  height: 56.h,
                  child: OutlinedButton(
                    onPressed: isSaving
                        ? null
                        : () {
                            if (_isEditing) {
                              setState(() => _isEditing = false);
                            } else {
                              _proceed();
                            }
                          },
                    style: OutlinedButton.styleFrom(
                      side: BorderSide(color: colors.borderDefault),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16.r),
                      ),
                    ),
                    child: Text(
                      '이대로 진행',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: colors.gray700,
                        fontWeight: FontWeight.w600,
                        fontSize: 15.sp,
                      ),
                    ),
                  ),
                ),
              ),
              SizedBox(width: 12.w),
              Expanded(
                child: SizedBox(
                  height: 56.h,
                  child: ElevatedButton(
                    onPressed: isSaving
                        ? null
                        : () {
                            if (_isEditing) {
                              setState(() => _isEditing = false);
                              _proceed();
                            } else {
                              setState(() => _isEditing = true);
                            }
                          },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.orange500,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16.r),
                      ),
                      elevation: 0,
                    ),
                    child: isSaving
                        ? SizedBox(
                            width: 20.w,
                            height: 20.w,
                            child: const CircularProgressIndicator(
                              color: Colors.white,
                              strokeWidth: 2,
                            ),
                          )
                        : Text(
                            _isEditing ? '수정 완료' : '수정하기',
                            style: AppTextStyles.bodyMd.copyWith(
                              color: Colors.white,
                              fontWeight: FontWeight.w600,
                              fontSize: 15.sp,
                            ),
                          ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
