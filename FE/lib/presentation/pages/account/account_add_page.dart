import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/bank_icon.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';

class AccountAddPage extends ConsumerStatefulWidget {
  final bool isOnboarding;

  const AccountAddPage({super.key, this.isOnboarding = true});

  @override
  ConsumerState<AccountAddPage> createState() => _AccountAddPageState();
}

class _AccountAddPageState extends ConsumerState<AccountAddPage> {
  String? _selectedBank;
  final TextEditingController _accountNumberController =
      TextEditingController();

  @override
  void dispose() {
    _accountNumberController.dispose();
    super.dispose();
  }

  bool get _isFormValid =>
      _selectedBank != null && _accountNumberController.text.isNotEmpty;

  void _onNext() {
    if (!_isFormValid) {
      return;
    }

    final route = widget.isOnboarding
        ? AppRoutes.onboardingAccountVerify
        : AppRoutes.accountVerify;

    context.push(
      route,
      extra: {
        'bankName': _selectedBank,
        'accountNumber': _accountNumberController.text,
        'isOnboarding': widget.isOnboarding,
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(accountViewModelProvider);
    final bankNames = state.bankNames;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: MoreAppBar(
        title: '계좌 추가',
        showBack: true,
        skipText: widget.isOnboarding ? '건너뛰기' : null,
        onSkip: widget.isOnboarding
            ? () => context.go(AppRoutes.onboardingGoal)
            : null,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Linked accounts list
            if (state.accounts.isNotEmpty) ...[
              ...state.accounts.map((account) {
                return Padding(
                  padding: EdgeInsets.only(bottom: 12.h),
                  child: Container(
                    padding: EdgeInsets.all(16.w),
                    decoration: BoxDecoration(
                      color: AppColors.gray50,
                      borderRadius: BorderRadius.circular(12.r),
                    ),
                    child: Row(
                      children: [
                        BankIcon(bankName: account.bankName, size: 36.w),
                        SizedBox(width: 12.w),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                account.bankName,
                                style: AppTextStyles.bodyMd.copyWith(
                                  color: AppColors.gray900,
                                  fontWeight: FontWeight.w600,
                                  fontSize: 14.sp,
                                ),
                              ),
                              SizedBox(height: 2.h),
                              Text(
                                account.accountNumber,
                                style: AppTextStyles.bodySm.copyWith(
                                  color: AppColors.gray500,
                                  fontSize: 12.sp,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              }),
              SizedBox(height: 8.h),
            ],

            // Add new account card
            GestureDetector(
              onTap: () {
                // Show bank selection
              },
              child: Container(
                width: double.infinity,
                padding: EdgeInsets.symmetric(vertical: 20.h),
                decoration: BoxDecoration(
                  border: Border.all(color: AppColors.borderDefault),
                  borderRadius: BorderRadius.circular(12.r),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.add_circle_outline, size: 24.w, color: AppColors.orange500),
                    SizedBox(width: 8.w),
                    Text(
                      '새로운 계좌 연결하기',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.orange500,
                        fontWeight: FontWeight.w600,
                        fontSize: 15.sp,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            SizedBox(height: 24.h),

            // Bank selection
            Text(
              '은행 선택',
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.gray900,
                fontSize: 18.sp,
              ),
            ),
            SizedBox(height: 20.h),
            if (state.isLoading)
              const Center(child: CircularProgressIndicator())
            else if (state.errorMessage != null)
              Text(
                '은행 목록을 불러오지 못했습니다.\n${state.errorMessage!}',
                style: AppTextStyles.bodySm.copyWith(color: AppColors.red500),
              )
            else
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 4,
                  crossAxisSpacing: 12.w,
                  mainAxisSpacing: 12.h,
                  childAspectRatio: 1.0,
                ),
                itemCount: bankNames.length,
                itemBuilder: (context, index) {
                  final bankName = bankNames[index];
                  final isSelected = _selectedBank == bankName;
                  return GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedBank = bankName;
                      });
                    },
                    child: Container(
                      decoration: BoxDecoration(
                        color: isSelected
                            ? AppColors.orange500.withValues(alpha: 0.1)
                            : AppColors.gray50,
                        borderRadius: BorderRadius.circular(12.r),
                        border: Border.all(
                          color: isSelected
                              ? AppColors.orange500
                              : Colors.transparent,
                          width: 2,
                        ),
                      ),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          BankIcon(bankName: bankName, size: 32.w),
                          SizedBox(height: 6.h),
                          Text(
                            bankName,
                            style: AppTextStyles.caption.copyWith(
                              color: isSelected
                                  ? AppColors.orange500
                                  : AppColors.gray700,
                              fontWeight: isSelected
                                  ? FontWeight.w600
                                  : FontWeight.w400,
                              fontSize: 11.sp,
                            ),
                            textAlign: TextAlign.center,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            if (_selectedBank != null) ...[
              SizedBox(height: 32.h),
              MoreTextField(
                label: '계좌번호',
                hint: '계좌번호를 입력해주세요',
                controller: _accountNumberController,
                keyboardType: TextInputType.number,
                onChanged: (_) => setState(() {}),
              ),
            ],
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: MoreBottomButton(
            text: '다음',
            onPressed: _onNext,
            enabled: _isFormValid,
          ),
        ),
      ),
    );
  }
}
