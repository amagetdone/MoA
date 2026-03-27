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
  bool _isAccountVerified = false;
  bool _isVerifying = false;
  bool _isRequesting = false;
  final TextEditingController _accountController = TextEditingController();
  final FocusNode _accountFocusNode = FocusNode();
  final ScrollController _scrollController = ScrollController();

  final List<BankCatalogItem> allBanks = BankCatalog.all;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final List<BankCatalogItem> displayBanks = isExpanded ? allBanks : allBanks.take(9).toList();

    return Scaffold(
      backgroundColor: colors.bgPrimary,
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
                  Text('은행 선택', style: AppTextStyles.headingLg.copyWith(color: colors.textPrimary, fontSize: 22.sp, fontWeight: FontWeight.bold)),
                  SizedBox(height: 4.h),
                  Text('은행을 선택하세요.', style: AppTextStyles.bodySm.copyWith(color: colors.textTertiary, fontSize: 13.sp)),
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
                        onTap: _isAccountVerified
                            ? null
                            : () {
                          setState(() {
                            if (selectedBank != bank.name) {
                              _accountController.clear();
                              _isAccountVerified = false;
                            }
                            selectedBank = bank.name;
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
                              color: isSelected ? AppColors.orange500 : Colors.transparent,
                              width: 1.5,
                            ),
                            boxShadow: [
                              BoxShadow(color: colors.cardShadow, blurRadius: 4, offset: const Offset(0, 2)),
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
                                    bank.logoPath ?? '',
                                    fit: BoxFit.cover,
                                    errorBuilder: (context, error, stackTrace) {
                                      return Container(
                                        color: colors.gray50,
                                        child: Center(child: Text('B', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14.sp, color: colors.textPrimary))),
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
                                        color: colors.textPrimary,
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
                        color: colors.dimText,
                        size: 24.w,
                      ),
                    ),
                  ),
                  SizedBox(height: 20.h),

                  Text('계좌번호', style: AppTextStyles.bodyMd.copyWith(color: colors.textPrimary, fontWeight: FontWeight.bold, fontSize: 15.sp)),
                  SizedBox(height: 12.h),
                  TextField(
                    controller: _accountController,
                    focusNode: _accountFocusNode,
                    enabled: selectedBank != null && !_isAccountVerified,
                    keyboardType: TextInputType.number,
                    style: TextStyle(fontSize: 14.sp, color: colors.textPrimary),
                    decoration: InputDecoration(
                      hintText: selectedBank == null
                          ? '은행을 먼저 선택해주세요.'
                          : '계좌번호를 입력해주세요.',
                      hintStyle: AppTextStyles.bodySm.copyWith(color: colors.textTertiary, fontSize: 13.sp),
                      filled: true,
                      fillColor: colors.bgSurface,
                      contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
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
                        borderSide: const BorderSide(color: AppColors.orange500, width: 1.2),
                      ),
                      disabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12.r),
                        borderSide: BorderSide(color: colors.borderDefault),
                      ),
                      suffixIcon: Padding(
                        padding: EdgeInsets.only(right: 8.w),
                        child: TextButton(
                          onPressed: selectedBank != null &&
                                  _accountController.text.isNotEmpty &&
                                  !_isVerifying &&
                                  !_isAccountVerified
                              ? () async {
                                  setState(() => _isVerifying = true);
                                  try {
                                    final confirmed = await ref
                                        .read(accountRepositoryProvider)
                                        .confirmAccount(_accountController.text);
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
                                  : (_accountController.text.isNotEmpty
                                      ? AppColors.orange500
                                      : colors.textTertiary),
                              fontWeight: FontWeight.w600,
                              fontSize: 13.sp,
                            ),
                          ),
                        ),
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
            text: _isRequesting ? '요청 중...' : '다음',
            onPressed: () async {
              if (selectedBank == null || _accountController.text.isEmpty || _isRequesting) return;

              setState(() => _isRequesting = true);
              try {
                await ref
                    .read(accountRepositoryProvider)
                    .requestVerification(_accountController.text);

                if (!mounted) return;

                final route = widget.isOnboarding
                    ? AppRoutes.onboardingAccountVerify
                    : AppRoutes.accountVerify;
                context.push(route, extra: {
                  'bankName': selectedBank,
                  'accountNumber': _accountController.text,
                  'isOnboarding': widget.isOnboarding,
                });
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('1원 인증 요청에 실패했습니다.')),
                  );
                }
              } finally {
                if (mounted) setState(() => _isRequesting = false);
              }
            },
            enabled: selectedBank != null && _accountController.text.isNotEmpty && _isAccountVerified && !_isRequesting,
          ),
        ],
      ),
    );
  }
}
