import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class AccountVerifyPage extends StatefulWidget {
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
  State<AccountVerifyPage> createState() => _AccountVerifyPageState();
}

class _AccountVerifyPageState extends State<AccountVerifyPage> {
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

  bool get _isCodeComplete =>
      _digitControllers.every((controller) => controller.text.isNotEmpty);

  void _onVerify() {
    if (!_isCodeComplete) {
      return;
    }

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
  }

  void _onResend() {
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
      backgroundColor: Colors.white,
      appBar: MoreAppBar(title: '계좌인증', showBack: true),
      body: Padding(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            SizedBox(height: 24.h),
            Text(
              '입금자 명에 표기된 숫자를 입력하세요',
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.gray900,
                fontSize: 18.sp,
              ),
              textAlign: TextAlign.center,
            ),
            SizedBox(height: 40.h),

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
            SizedBox(height: 24.h),

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
            SizedBox(height: 16.h),

            // Resend link
            GestureDetector(
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
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: MoreBottomButton(
            text: '인증하기',
            onPressed: _onVerify,
            enabled: _isCodeComplete,
          ),
        ),
      ),
    );
  }
}
