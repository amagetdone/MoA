import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class BankInfo {
  final String name;
  final String logoPath;
  BankInfo(this.name, this.logoPath);
}

class AccountAddPage extends ConsumerStatefulWidget {
  final bool isOnboarding;

  const AccountAddPage({super.key, this.isOnboarding = true});

  @override
  ConsumerState<AccountAddPage> createState() => _AccountAddPageState();
}

class _AccountAddPageState extends ConsumerState<AccountAddPage> {
  String? selectedBank;
  bool isExpanded = false;
  final TextEditingController _accountController = TextEditingController();
  final FocusNode _accountFocusNode = FocusNode();
  final ScrollController _scrollController = ScrollController();

  final List<BankInfo> allBanks = [
    BankInfo('국민은행', 'assets/images/logo/KB_logo.png'),
    BankInfo('신한은행', 'assets/images/logo/shinhan_logo.png'),
    BankInfo('우리은행', 'assets/images/logo/woori_logo.png'),
    BankInfo('하나은행', 'assets/images/logo/KEB_hana_logo.png'),
    BankInfo('기업은행', 'assets/images/logo/ibk_logo.png'),
    BankInfo('iM뱅크', 'assets/images/logo/iM_logo.png'),
    BankInfo('카카오뱅크', 'assets/images/logo/kakaobank.png'),
    BankInfo('농협은행', 'assets/images/logo/nonghyup_logo.png'),
    BankInfo('새마을금고', 'assets/images/logo/mg_logo.png'),
    BankInfo('SC제일은행', 'assets/images/logo/sc_jeil_logo.png'),
    BankInfo('씨티은행', 'assets/images/logo/citi_logo.png'),
    BankInfo('KDB산업은행', 'assets/images/logo/industry_logo.png'),
    BankInfo('경남은행', 'assets/images/logo/kyung_nam_logo.png'),
    BankInfo('전북은행', 'assets/images/logo/gwangju_logo.jpg'),
    BankInfo('제주은행', 'assets/images/logo/shinhan_logo.png'),
  ];

  @override
  Widget build(BuildContext context) {
    final List<BankInfo> displayBanks = isExpanded ? allBanks : allBanks.take(9).toList();

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '', showBack: true),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              controller: _scrollController,
              physics: const ClampingScrollPhysics(),
              padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 8.h),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('은행 선택', style: AppTextStyles.headingLg.copyWith(fontSize: 22.sp, fontWeight: FontWeight.bold)),
                  SizedBox(height: 4.h),
                  Text('은행을 선택하세요.', style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary, fontSize: 13.sp)),
                  SizedBox(height: 20.h),

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
                      final isSelected = selectedBank == bank.name;
                      return GestureDetector(
                        onTap: () {
                          setState(() => selectedBank = bank.name);
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
                                        fontWeight: isSelected ? FontWeight.bold : FontWeight.w500
                                    )
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
                    onTap: () => setState(() => isExpanded = !isExpanded),
                    child: Center(
                      child: Icon(
                        isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
                        color: Colors.black38,
                        size: 24.w,
                      ),
                    ),
                  ),
                  SizedBox(height: 20.h),

                  Text('계좌번호', style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold, fontSize: 15.sp)),
                  SizedBox(height: 12.h),
                  TextField(
                    controller: _accountController,
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
                    ),
                    onChanged: (_) => setState(() {}),
                  ),
                  SizedBox(height: 20.h),
                ],
              ),
            ),
          ),
          MoreBottomButton(
            text: '다음',
            onPressed: () {
              if (selectedBank != null && _accountController.text.isNotEmpty) {
                final route = widget.isOnboarding
                    ? AppRoutes.onboardingAccountVerify
                    : AppRoutes.accountVerify;
                context.push(route, extra: {
                  'bankName': selectedBank,
                  'accountNumber': _accountController.text,
                  'isOnboarding': widget.isOnboarding,
                });
              }
            },
            enabled: selectedBank != null && _accountController.text.isNotEmpty,
          ),
        ],
      ),
    );
  }
}