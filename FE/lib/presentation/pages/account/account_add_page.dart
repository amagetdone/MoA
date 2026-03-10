import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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
        title: '계좌 연동',
        showBack: true,
        skipText: widget.isOnboarding ? '건너뛰기' : null,
        onSkip: widget.isOnboarding
            ? () => context.go(AppRoutes.onboardingGoal)
            : null,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '은행을 선택해주세요',
              style: AppTextStyles.headingMd.copyWith(color: AppColors.gray900),
            ),
            const SizedBox(height: 20),
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
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
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
                        borderRadius: BorderRadius.circular(12),
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
                          BankIcon(bankName: bankName, size: 36),
                          const SizedBox(height: 8),
                          Text(
                            bankName,
                            style: AppTextStyles.caption.copyWith(
                              color: isSelected
                                  ? AppColors.orange500
                                  : AppColors.gray700,
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
            if (_selectedBank != null) ...[
              const SizedBox(height: 32),
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
          padding: const EdgeInsets.all(24),
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
