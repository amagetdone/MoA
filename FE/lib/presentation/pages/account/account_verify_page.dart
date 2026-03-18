import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class AccountVerifyPage extends ConsumerStatefulWidget {
  final bool isOnboarding;
  final String bankName;
  final String accountNumber;

  const AccountVerifyPage({
    super.key,
    this.isOnboarding = true,
    required this.bankName,
    required this.accountNumber,
  });

  @override
  ConsumerState<AccountVerifyPage> createState() => _AccountVerifyPageState();
}

class _AccountVerifyPageState extends ConsumerState<AccountVerifyPage> {
  final List<TextEditingController> _digitControllers = List.generate(
    4,
    (_) => TextEditingController(),
  );
  final List<FocusNode> _focusNodes = List.generate(4, (_) => FocusNode());

  late Timer _timer;
  int _remainingSeconds = 292; // 4:52

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_remainingSeconds > 0) {
        setState(() => _remainingSeconds--);
      } else {
        timer.cancel();
      }
    });
  }

  @override
  void dispose() {
    _timer.cancel();
    for (final controller in _digitControllers) {
      controller.dispose();
    }
    for (final node in _focusNodes) {
      node.dispose();
    }
    super.dispose();
  }

  String get _timerText {
    final minutes = _remainingSeconds ~/ 60;
    final seconds = _remainingSeconds % 60;
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }

  bool _isVerifying = false;

  bool get _isCodeComplete =>
      _digitControllers.every((controller) => controller.text.isNotEmpty);

  Future<void> _onVerify() async {
    if (!_isCodeComplete || _isVerifying) return;

    final authCode = _digitControllers.map((c) => c.text).join();
    setState(() => _isVerifying = true);

    try {
      final verified = await ref
          .read(accountRepositoryProvider)
          .checkVerification(
            accountNo: widget.accountNumber,
            authCode: authCode,
          );

      if (!mounted) return;

      if (verified) {
        final route = widget.isOnboarding
            ? AppRoutes.onboardingAccountComplete
            : AppRoutes.accountComplete;

        context.push(
          route,
          extra: {
            'bankName': widget.bankName,
            'accountNumber': widget.accountNumber,
            'isOnboarding': widget.isOnboarding,
          },
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('인증번호가 일치하지 않습니다.')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('인증 확인 중 오류가 발생했습니다.')),
        );
      }
    } finally {
      if (mounted) setState(() => _isVerifying = false);
    }
  }

  Future<void> _onResend() async {
    try {
      await ref
          .read(accountRepositoryProvider)
          .requestVerification(widget.accountNumber);
    } catch (_) {}
    if (!mounted) return;
    setState(() {
      _remainingSeconds = 292;
      for (final c in _digitControllers) {
        c.clear();
      }
    });
    _focusNodes[0].requestFocus();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(title: widget.isOnboarding ? '' : '계좌인증', showBack: true),
      body: Padding(
        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
        child: Column(
          crossAxisAlignment: widget.isOnboarding ? CrossAxisAlignment.start : CrossAxisAlignment.center,
          children: [
            if (widget.isOnboarding) ...[
              Text(
                '계좌인증',
                style: AppTextStyles.headingLg.copyWith(
                  fontSize: 24.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: 6.h),
              Text(
                '1원을 보내드렸어요.',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                  fontSize: 14.sp,
                ),
              ),
              SizedBox(height: 24.h),
            ] else
              SizedBox(height: 24.h),
            Text(
              '입금자 명에 표기된 숫자를 입력하세요',
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.gray900,
                fontSize: 18.sp,
              ),
              textAlign: TextAlign.center,
            ),
            SizedBox(height: 24.h),

            // 4 digit input boxes
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(4, (index) {
                return Container(
                  width: 56.w,
                  height: 64.h,
                  margin: EdgeInsets.symmetric(horizontal: 6.w),
                  child: TextField(
                    controller: _digitControllers[index],
                    focusNode: _focusNodes[index],
                    keyboardType: TextInputType.number,
                    textAlign: TextAlign.center,
                    maxLength: 1,
                    style: AppTextStyles.headingLg.copyWith(fontSize: 24.sp),
                    decoration: InputDecoration(
                      counterText: '',
                      filled: true,
                      fillColor: AppColors.gray50,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12.r),
                        borderSide: BorderSide.none,
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12.r),
                        borderSide: BorderSide(
                          color: AppColors.orange500,
                          width: 2,
                        ),
                      ),
                    ),
                    onChanged: (value) {
                      if (value.isNotEmpty && index < 3) {
                        _focusNodes[index + 1].requestFocus();
                      }
                      setState(() {});
                    },
                  ),
                );
              }),
            ),
            SizedBox(height: 12.h),

            // Timer
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.timer_outlined, size: 18.w, color: AppColors.gray500),
                SizedBox(width: 4.w),
                Text(
                  _timerText,
                  style: AppTextStyles.bodyMd.copyWith(
                    color: AppColors.gray500,
                    fontSize: 14.sp,
                  ),
                ),
              ],
            ),
            SizedBox(height: 6.h),

            // Resend link
            Center(
              child: GestureDetector(
                onTap: _onResend,
                child: Text(
                  '인증번호 재전송',
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.orange500,
                    fontSize: 13.sp,
                    decoration: TextDecoration.underline,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: _isVerifying ? '확인 중...' : '인증하기',
        onPressed: _onVerify,
        enabled: _isCodeComplete && !_isVerifying,
        progress: widget.isOnboarding ? 4 / 7 : null,
      ),
    );
  }
}
