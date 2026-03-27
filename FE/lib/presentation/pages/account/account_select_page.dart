import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/constants/bank_catalog.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/widgets/common/bank_icon.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class AccountSelectPage extends ConsumerStatefulWidget {
  final bool isOnboarding;

  const AccountSelectPage({super.key, this.isOnboarding = true});

  @override
  ConsumerState<AccountSelectPage> createState() => _AccountSelectPageState();
}

class _AccountSelectPageState extends ConsumerState<AccountSelectPage> {
  BankCatalogItem? _selectedBank;
  final TextEditingController _accountNumberController =
      TextEditingController();
  final FocusNode _accountFocusNode = FocusNode();
  final ScrollController _scrollController = ScrollController();
  bool _showAllBanks = false;

  final List<BankCatalogItem> _allBanks = BankCatalog.all;

  @override
  void dispose() {
    _accountNumberController.dispose();
    _accountFocusNode.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  bool _isAccountVerified = false;
  bool _isVerifying = false;

  bool get _isFormValid =>
      _selectedBank != null &&
      _accountNumberController.text.isNotEmpty &&
      _isAccountVerified;

  String? get _selectedBankCode => _selectedBank?.code;
  String? get _selectedBankName => _selectedBank?.name;

  bool _isRequesting = false;

  Future<void> _onNext() async {
    if (!_isFormValid || _isRequesting) return;

    setState(() => _isRequesting = true);
    try {
      await ref
          .read(accountRepositoryProvider)
          .requestVerification(_accountNumberController.text);

      if (!mounted) return;

      final route = widget.isOnboarding
          ? AppRoutes.onboardingAccountVerify
          : AppRoutes.accountVerify;

      context.push(
        route,
        extra: {
          'bankCode': _selectedBankCode,
          'bankName': _selectedBankName,
          'accountNumber': _accountNumberController.text,
          'isOnboarding': widget.isOnboarding,
        },
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('1원 인증 요청에 실패했습니다.')));
      }
    } finally {
      if (mounted) setState(() => _isRequesting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final displayBanks = _showAllBanks ? _allBanks : _allBanks.take(9).toList();

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: MoreAppBar(
        title: widget.isOnboarding ? '' : '계좌 연동',
        showBack: true,
      ),
      body: SingleChildScrollView(
        controller: _scrollController,
        physics: const ClampingScrollPhysics(),
        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (widget.isOnboarding) ...[
              Text(
                '계좌 연동',
                style: AppTextStyles.headingLg.copyWith(
                  color: colors.textPrimary,
                  fontSize: 24.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: 6.h),
              Text(
                '은행을 선택하고 계좌번호를 입력해주세요.',
                style: AppTextStyles.bodySm.copyWith(
                  color: colors.textTertiary,
                  fontSize: 14.sp,
                ),
              ),
              SizedBox(height: 24.h),
            ],
            Text(
              '은행 선택',
              style: AppTextStyles.headingMd.copyWith(
                color: colors.textPrimary,
                fontSize: 18.sp,
              ),
            ),
            SizedBox(height: 12.h),

            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                crossAxisSpacing: 8.w,
                mainAxisSpacing: 8.h,
                childAspectRatio: 0.95,
              ),
              itemCount: displayBanks.length,
              itemBuilder: (context, index) {
                final bank = displayBanks[index];
                final isSelected = _selectedBank?.code == bank.code;
                return GestureDetector(
                  onTap: _isAccountVerified
                      ? null
                      : () {
                          setState(() {
                            if (_selectedBank?.code != bank.code) {
                              _accountNumberController.clear();
                              _isAccountVerified = false;
                            }
                            _selectedBank = bank;
                          });
                          WidgetsBinding.instance.addPostFrameCallback((_) {
                            _accountFocusNode.requestFocus();
                            _scrollController.animateTo(
                              _scrollController.position.maxScrollExtent,
                              duration: const Duration(milliseconds: 300),
                              curve: Curves.easeOut,
                            );
                          });
                        },
                  child: Container(
                    decoration: BoxDecoration(
                      color: colors.bgSurface,
                      borderRadius: BorderRadius.circular(12.r),
                      border: Border.all(
                        color: isSelected
                            ? AppColors.orange500
                            : Colors.transparent,
                        width: 1.5,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: colors.cardShadow,
                          blurRadius: 4,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        ClipOval(
                          child: SizedBox(
                            width: 50.w,
                            height: 50.w,
                            child: BankIcon(bankName: bank.name, size: 50.w),
                          ),
                        ),
                        SizedBox(height: 8.h),
                        FittedBox(
                          fit: BoxFit.scaleDown,
                          child: Text(
                            bank.name,
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textPrimary,
                              fontSize: 11.sp,
                              fontWeight: isSelected
                                  ? FontWeight.bold
                                  : FontWeight.w500,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),

            SizedBox(height: 12.h),
            GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => setState(() => _showAllBanks = !_showAllBanks),
              child: Center(
                child: Icon(
                  _showAllBanks
                      ? Icons.keyboard_arrow_up
                      : Icons.keyboard_arrow_down,
                  color: colors.dimText,
                  size: 24.w,
                ),
              ),
            ),

            if (_selectedBank != null) ...[
              SizedBox(height: 20.h),
              Text(
                '계좌번호',
                style: AppTextStyles.bodyMd.copyWith(
                  color: colors.textPrimary,
                  fontWeight: FontWeight.bold,
                  fontSize: 15.sp,
                ),
              ),
              SizedBox(height: 12.h),
              TextField(
                controller: _accountNumberController,
                focusNode: _accountFocusNode,
                enabled: !_isAccountVerified,
                keyboardType: TextInputType.number,
                style: TextStyle(fontSize: 14.sp, color: colors.textPrimary),
                decoration: InputDecoration(
                  hintText: '계좌번호를 입력해주세요.',
                  hintStyle: AppTextStyles.bodySm.copyWith(
                    color: colors.textTertiary,
                    fontSize: 13.sp,
                  ),
                  filled: true,
                  fillColor: colors.bgSurface,
                  contentPadding: EdgeInsets.symmetric(
                    horizontal: 16.w,
                    vertical: 12.h,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.r),
                    borderSide: BorderSide(color: colors.borderDefault),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.r),
                    borderSide: BorderSide(color: colors.borderDefault),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.r),
                    borderSide: const BorderSide(
                      color: AppColors.orange500,
                      width: 1.2,
                    ),
                  ),
                  disabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.r),
                    borderSide: BorderSide(color: colors.borderDefault),
                  ),
                  suffixIcon: Padding(
                    padding: EdgeInsets.only(right: 8.w),
                    child: TextButton(
                      onPressed:
                          _accountNumberController.text.isNotEmpty &&
                              !_isVerifying &&
                              !_isAccountVerified
                          ? () async {
                              setState(() => _isVerifying = true);
                              try {
                                final confirmed = await ref
                                    .read(accountRepositoryProvider)
                                    .confirmAccount(
                                      _accountNumberController.text,
                                    );
                                if (mounted) {
                                  setState(() {
                                    _isAccountVerified = confirmed;
                                    _isVerifying = false;
                                  });
                                  if (!confirmed) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(
                                        content: Text('계좌 실소유 확인에 실패했습니다.'),
                                      ),
                                    );
                                  }
                                }
                              } catch (e) {
                                if (mounted) {
                                  setState(() => _isVerifying = false);
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content: Text('계좌 확인 중 오류가 발생했습니다.'),
                                    ),
                                  );
                                }
                              }
                              FocusScope.of(context).unfocus();
                            }
                          : null,
                      style: TextButton.styleFrom(
                        padding: EdgeInsets.symmetric(horizontal: 12.w),
                        minimumSize: Size.zero,
                        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      ),
                      child: Text(
                        _isVerifying
                            ? '확인중...'
                            : (_isAccountVerified ? '확인완료' : '확인하기'),
                        style: AppTextStyles.bodySm.copyWith(
                          color: _isAccountVerified
                              ? AppColors.green500
                              : (_accountNumberController.text.isNotEmpty
                                    ? AppColors.orange500
                                    : colors.textTertiary),
                          fontWeight: FontWeight.w600,
                          fontSize: 13.sp,
                        ),
                      ),
                    ),
                  ),
                ),
                onChanged: (_) => setState(() {
                  _isAccountVerified = false;
                }),
              ),
            ],
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: '다음',
        onPressed: _onNext,
        enabled: _isFormValid,
        progress: widget.isOnboarding ? 4 / 7 : null,
      ),
    );
  }
}
