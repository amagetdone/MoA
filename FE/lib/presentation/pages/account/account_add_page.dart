import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

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

  final List<String> allBanks = [
    '국민은행', '신한은행', '우리은행',
    '하나은행', '기업은행', 'iM뱅크',
    '농협은행', '새마을금고', 'SC제일은행',
    '씨티은행', 'KDB산업은행', '경남은행',
    '전북은행', '제주은행'
  ];

  @override
  Widget build(BuildContext context) {
    final List<String> displayBanks = isExpanded ? allBanks : allBanks.take(9).toList();

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '', showBack: true),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
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
                      crossAxisSpacing: 10.w,
                      mainAxisSpacing: 10.h,
                      childAspectRatio: 0.9,
                    ),
                    itemCount: displayBanks.length,
                    itemBuilder: (context, index) {
                      final bank = displayBanks[index];
                      final isSelected = selectedBank == bank;
                      return GestureDetector(
                        onTap: () => setState(() => selectedBank = bank),
                        child: Container(
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(16.r),
                            border: Border.all(
                              color: isSelected ? AppColors.orange500 : Colors.transparent,
                              width: 2,
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.04),
                                blurRadius: 10,
                                offset: const Offset(0, 4),
                              ),
                            ],
                          ),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Container(
                                width: 44.w,
                                height: 44.w,
                                decoration: const BoxDecoration(color: Color(0xFFFFE812), shape: BoxShape.circle),
                                child: Center(child: Text('B', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20.sp, color: Colors.black87))),
                              ),
                              SizedBox(height: 8.h),
                              FittedBox(
                                fit: BoxFit.scaleDown,
                                child: Text(bank, style: AppTextStyles.caption.copyWith(color: AppColors.textPrimary, fontSize: 12.sp, fontWeight: FontWeight.w500)),
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
                      child: Padding(
                        padding: EdgeInsets.all(8.w),
                        child: Icon(
                          isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
                          color: Colors.black38,
                          size: 28.w,
                        ),
                      ),
                    ),
                  ),
                  SizedBox(height: 24.h),

                  Text('계좌번호', style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold, fontSize: 16.sp)),
                  SizedBox(height: 12.h),
                  TextField(
                    controller: _accountController,
                    keyboardType: TextInputType.number,
                    style: TextStyle(fontSize: 14.sp),
                    decoration: InputDecoration(
                      hintText: '계좌번호를 입력해주세요.',
                      hintStyle: AppTextStyles.bodySm.copyWith(color: const Color(0xFFCCCCCC), fontSize: 13.sp),
                      filled: true,
                      fillColor: Colors.white,
                      contentPadding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 16.h),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16.r),
                        borderSide: const BorderSide(color: Color(0xFFEEEEEE)),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16.r),
                        borderSide: const BorderSide(color: Color(0xFFEEEEEE)),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16.r),
                        borderSide: const BorderSide(color: AppColors.orange500, width: 1.5),
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
