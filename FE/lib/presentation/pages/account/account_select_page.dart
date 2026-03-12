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

class AccountSelectPage extends ConsumerStatefulWidget {
  final bool isOnboarding;

  const AccountSelectPage({super.key, this.isOnboarding = true});

  @override
  ConsumerState<AccountSelectPage> createState() => _AccountSelectPageState();
}

class _AccountSelectPageState extends ConsumerState<AccountSelectPage> {
  String? _selectedBank;
  final TextEditingController _accountNumberController =
      TextEditingController();
  bool _showAllBanks = false;

  @override
  void dispose() {
    _accountNumberController.dispose();
    super.dispose();
  }

  bool get _isFormValid =>
      _selectedBank != null && _accountNumberController.text.isNotEmpty;

  void _onNext() {
    if (!_isFormValid) return;

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
    final visibleBanks = _showAllBanks ? bankNames : bankNames.take(12).toList();

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '계좌 연동',
        showBack: true,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Bank selection
            Text(
              '은행 선택',
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.textPrimary,
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
            else ...[
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3,
                  crossAxisSpacing: 16.w,
                  mainAxisSpacing: 16.h,
                  childAspectRatio: 1.0,
                ),
                itemCount: visibleBanks.length,
                itemBuilder: (context, index) {
                  final bankName = visibleBanks[index];
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
                            : AppColors.bgSurface,
                        borderRadius: BorderRadius.circular(16.r),
                        border: Border.all(
                          color: isSelected
                              ? AppColors.orange500
                              : Colors.transparent,
                          width: 2,
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.04),
                            offset: Offset(0, 1.h),
                            blurRadius: 4.r,
                          ),
                        ],
                      ),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          BankIcon(bankName: bankName, size: 40.w),
                          SizedBox(height: 8.h),
                          Text(
                            bankName,
                            style: AppTextStyles.bodySm.copyWith(
                              color: isSelected
                                  ? AppColors.orange500
                                  : AppColors.textPrimary,
                              fontWeight: isSelected
                                  ? FontWeight.w600
                                  : FontWeight.w400,
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
              if (bankNames.length > 12) ...[
                SizedBox(height: 12.h),
                Center(
                  child: GestureDetector(
                    onTap: () => setState(() => _showAllBanks = !_showAllBanks),
                    child: Icon(
                      _showAllBanks
                          ? Icons.keyboard_arrow_up
                          : Icons.keyboard_arrow_down,
                      size: 28.w,
                      color: AppColors.textTertiary,
                    ),
                  ),
                ),
              ],
            ],

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
      bottomNavigationBar: MoreBottomButton(
        text: '다음',
        onPressed: _onNext,
        enabled: _isFormValid,
      ),
    );
  }
}
