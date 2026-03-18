import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class _BankInfo {
  final String code;
  final String name;
  final String logoPath;
  _BankInfo(this.code, this.name, this.logoPath);
}

class AccountSelectPage extends ConsumerStatefulWidget {
  final bool isOnboarding;

  const AccountSelectPage({super.key, this.isOnboarding = true});

  @override
  ConsumerState<AccountSelectPage> createState() => _AccountSelectPageState();
}

class _AccountSelectPageState extends ConsumerState<AccountSelectPage> {
  _BankInfo? _selectedBank;
  final TextEditingController _accountNumberController =
      TextEditingController();
  final FocusNode _accountFocusNode = FocusNode();
  final ScrollController _scrollController = ScrollController();
  bool _showAllBanks = false;

  final List<_BankInfo> _allBanks = [
    _BankInfo('004', '국민은행', 'assets/images/logo/KB_logo.png'),
    _BankInfo('088', '신한은행', 'assets/images/logo/shinhan_logo.png'),
    _BankInfo('020', '우리은행', 'assets/images/logo/woori_logo.png'),
    _BankInfo('081', 'KEB하나은행', 'assets/images/logo/KEB_hana_logo.png'),
    _BankInfo('003', '기업은행', 'assets/images/logo/ibk_logo.png'),
    _BankInfo('090', '카카오뱅크', 'assets/images/logo/kakaobank.png'),
    _BankInfo('011', '농협은행', 'assets/images/logo/nonghyup_logo.png'),
    _BankInfo('045', '새마을금고', 'assets/images/logo/mg_logo.png'),
    _BankInfo('023', 'SC제일은행', 'assets/images/logo/sc_jeil_logo.png'),
    _BankInfo('027', '시티은행', 'assets/images/logo/citi_logo.png'),
    _BankInfo('002', '산업은행', 'assets/images/logo/industry_logo.png'),
    _BankInfo('039', '경남은행', 'assets/images/logo/kyung_nam_logo.png'),
    _BankInfo('037', '전북은행', 'assets/images/logo/gwangju_logo.jpg'),
    _BankInfo('032', '대구은행', ''),
    _BankInfo('034', '광주은행', ''),
    _BankInfo('035', '제주은행', ''),
    _BankInfo('001', '한국은행', ''),
    _BankInfo('999', '싸피은행', ''),
  ];

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
      _selectedBank != null && _accountNumberController.text.isNotEmpty && _isAccountVerified;

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
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('1원 인증 요청에 실패했습니다.')),
        );
      }
    } finally {
      if (mounted) setState(() => _isRequesting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final displayBanks = _showAllBanks ? _allBanks : _allBanks.take(9).toList();

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
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
                  fontSize: 24.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: 6.h),
              Text(
                '은행을 선택하고 계좌번호를 입력해주세요.',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                  fontSize: 14.sp,
                ),
              ),
              SizedBox(height: 24.h),
            ],
            Text(
              '은행 선택',
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.textPrimary,
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
                  onTap: () {
                    setState(() => _selectedBank = bank);
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
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12.r),
                      border: Border.all(
                        color: isSelected ? AppColors.orange500 : Colors.transparent,
                        width: 1.5,
                      ),
                      boxShadow: [
                        BoxShadow(color: Colors.black.withValues(alpha: 0.02), blurRadius: 4, offset: const Offset(0, 2)),
                      ],
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        ClipOval(
                          child: SizedBox(
                            width: 50.w,
                            height: 50.w,
                            child: Image.asset(
                              bank.logoPath,
                              fit: BoxFit.cover,
                              errorBuilder: (context, error, stackTrace) {
                                return Container(
                                  color: const Color(0xFFF5F5F5),
                                  child: Center(child: Text('B', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14.sp, color: Colors.black87))),
                                );
                              },
                            ),
                          ),
                        ),
                        SizedBox(height: 8.h),
                        FittedBox(
                          fit: BoxFit.scaleDown,
                          child: Text(
                            bank.name,
                            style: AppTextStyles.caption.copyWith(
                              color: AppColors.textPrimary,
                              fontSize: 11.sp,
                              fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
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
                  _showAllBanks ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
                  color: Colors.black38,
                  size: 24.w,
                ),
              ),
            ),

            if (_selectedBank != null) ...[
              SizedBox(height: 20.h),
              Text('계좌번호', style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold, fontSize: 15.sp)),
              SizedBox(height: 12.h),
              TextField(
                controller: _accountNumberController,
                focusNode: _accountFocusNode,
                keyboardType: TextInputType.number,
                style: TextStyle(fontSize: 14.sp),
                decoration: InputDecoration(
                  hintText: '계좌번호를 입력해주세요.',
                  hintStyle: AppTextStyles.bodySm.copyWith(color: const Color(0xFFCCCCCC), fontSize: 13.sp),
                  filled: true,
                  fillColor: Colors.white,
                  contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.r),
                    borderSide: const BorderSide(color: Color(0xFFEEEEEE)),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.r),
                    borderSide: const BorderSide(color: Color(0xFFEEEEEE)),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.r),
                    borderSide: const BorderSide(color: AppColors.orange500, width: 1.2),
                  ),
                  suffixIcon: Padding(
                    padding: EdgeInsets.only(right: 8.w),
                    child: TextButton(
                      onPressed: _accountNumberController.text.isNotEmpty && !_isVerifying
                          ? () async {
                              setState(() => _isVerifying = true);
                              try {
                                final confirmed = await ref
                                    .read(accountRepositoryProvider)
                                    .confirmAccount(_accountNumberController.text);
                                if (mounted) {
                                  setState(() {
                                    _isAccountVerified = confirmed;
                                    _isVerifying = false;
                                  });
                                  if (!confirmed) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(content: Text('계좌 실소유 확인에 실패했습니다.')),
                                    );
                                  }
                                }
                              } catch (e) {
                                if (mounted) {
                                  setState(() => _isVerifying = false);
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('계좌 확인 중 오류가 발생했습니다.')),
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
                        _isVerifying ? '확인중...' : (_isAccountVerified ? '확인완료' : '확인하기'),
                        style: AppTextStyles.bodySm.copyWith(
                          color: _isAccountVerified
                              ? AppColors.green500
                              : (_accountNumberController.text.isNotEmpty
                                  ? AppColors.orange500
                                  : AppColors.textTertiary),
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
