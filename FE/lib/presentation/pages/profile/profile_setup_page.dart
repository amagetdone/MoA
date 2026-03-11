import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';
import 'package:more_app/presentation/widgets/common/more_chip.dart';

class ProfileSetupPage extends StatefulWidget {
  final bool isOnboarding;
  final Map<String, String>? initialData;

  const ProfileSetupPage({
    super.key,
    this.isOnboarding = true,
    this.initialData,
  });

  @override
  State<ProfileSetupPage> createState() => _ProfileSetupPageState();
}

class _ProfileSetupPageState extends State<ProfileSetupPage> {
  late final TextEditingController _nicknameController;
  late final TextEditingController _emailController;
  late final TextEditingController _birthDateController;
  String? _selectedIncome;

  // Agreement checkboxes
  bool _agreeAll = false;
  bool _agreeTerms = false;
  bool _agreePrivacy = false;
  bool _agreeMarketing = false;

  @override
  void initState() {
    super.initState();
    _nicknameController = TextEditingController(
      text: widget.initialData?['nickname'] ?? '',
    );
    _emailController = TextEditingController(
      text: widget.initialData?['email'] ?? '',
    );
    _birthDateController = TextEditingController(
      text: widget.initialData?['birthDate'] ?? '',
    );
    _selectedIncome = widget.initialData?['income'];
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _emailController.dispose();
    _birthDateController.dispose();
    super.dispose();
  }

  bool get _isFormValid {
    final hasBasicInfo = _nicknameController.text.isNotEmpty &&
        _emailController.text.isNotEmpty &&
        _birthDateController.text.isNotEmpty &&
        _selectedIncome != null;

    if (widget.isOnboarding) {
      return hasBasicInfo && _agreeTerms && _agreePrivacy;
    }
    return _nicknameController.text.isNotEmpty && _emailController.text.isNotEmpty;
  }

  void _onAgreeAllChanged(bool? value) {
    setState(() {
      _agreeAll = value ?? false;
      _agreeTerms = _agreeAll;
      _agreePrivacy = _agreeAll;
      _agreeMarketing = _agreeAll;
    });
  }

  void _updateAgreeAll() {
    setState(() {
      _agreeAll = _agreeTerms && _agreePrivacy && _agreeMarketing;
    });
  }

  void _onSubmit() {
    if (!_isFormValid) return;

    if (widget.isOnboarding) {
      context.go(AppRoutes.onboardingAccount);
    } else {
      context.pop();
    }
  }

  Future<void> _pickBirthDate() async {
    final date = await showDatePicker(
      context: context,
      initialDate: DateTime(2000, 1, 1),
      firstDate: DateTime(1950),
      lastDate: DateTime.now(),
    );
    if (date != null) {
      setState(() {
        _birthDateController.text =
            '${date.year}.${date.month.toString().padLeft(2, '0')}.${date.day.toString().padLeft(2, '0')}';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: MoreAppBar(
        title: widget.isOnboarding ? '프로필 설정' : '내 정보 수정',
        showBack: true,
        skipText: widget.isOnboarding ? '건너뛰기' : null,
        onSkip: widget.isOnboarding
            ? () => context.go(AppRoutes.onboardingAccount)
            : null,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Profile photo
            Center(
              child: GestureDetector(
                onTap: () {
                  // TODO: pick profile image
                },
                child: Container(
                  width: 88.w,
                  height: 88.w,
                  decoration: BoxDecoration(
                    color: AppColors.gray100,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.camera_alt_outlined,
                    size: 32.w,
                    color: AppColors.gray500,
                  ),
                ),
              ),
            ),
            SizedBox(height: 32.h),

            // Nickname
            MoreTextField(
              label: '닉네임',
              hint: '닉네임을 입력해주세요',
              controller: _nicknameController,
              onChanged: (_) => setState(() {}),
            ),
            SizedBox(height: 24.h),

            // Email
            MoreTextField(
              label: '이메일',
              hint: '이메일을 입력해주세요',
              controller: _emailController,
              keyboardType: TextInputType.emailAddress,
              onChanged: (_) => setState(() {}),
            ),
            SizedBox(height: 24.h),

            if (widget.isOnboarding) ...[
              // Birth date with calendar icon
              Text(
                '생년월일',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.gray700,
                  fontWeight: FontWeight.w600,
                  fontSize: 13.sp,
                ),
              ),
              SizedBox(height: 8.h),
              GestureDetector(
                onTap: _pickBirthDate,
                child: AbsorbPointer(
                  child: TextField(
                    controller: _birthDateController,
                    decoration: InputDecoration(
                      hintText: 'YYYY.MM.DD',
                      hintStyle: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.gray500,
                        fontSize: 15.sp,
                      ),
                      suffixIcon: Icon(
                        Icons.calendar_today_outlined,
                        size: 20.w,
                        color: AppColors.gray500,
                      ),
                      filled: true,
                      fillColor: AppColors.gray50,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12.r),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: EdgeInsets.symmetric(
                        horizontal: 16.w,
                        vertical: 16.h,
                      ),
                    ),
                    style: AppTextStyles.bodyMd.copyWith(
                      color: AppColors.gray900,
                      fontSize: 15.sp,
                    ),
                  ),
                ),
              ),
              SizedBox(height: 24.h),

              // 고정 수입 여부
              Text(
                '고정 수입 여부',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.gray700,
                  fontWeight: FontWeight.w600,
                  fontSize: 13.sp,
                ),
              ),
              SizedBox(height: 8.h),
              Row(
                children: [
                  MoreChip(
                    label: '네',
                    selected: _selectedIncome == '네',
                    onTap: () {
                      setState(() {
                        _selectedIncome = '네';
                      });
                    },
                  ),
                  SizedBox(width: 12.w),
                  MoreChip(
                    label: '아니요',
                    selected: _selectedIncome == '아니요',
                    onTap: () {
                      setState(() {
                        _selectedIncome = '아니요';
                      });
                    },
                  ),
                  SizedBox(width: 12.w),
                  MoreChip(
                    label: '모르겠요',
                    selected: _selectedIncome == '모르겠요',
                    onTap: () {
                      setState(() {
                        _selectedIncome = '모르겠요';
                      });
                    },
                  ),
                ],
              ),

              // Agreement section
              SizedBox(height: 40.h),
              Container(
                padding: EdgeInsets.all(16.w),
                decoration: BoxDecoration(
                  color: AppColors.gray50,
                  borderRadius: BorderRadius.circular(12.r),
                ),
                child: Column(
                  children: [
                    // Agree all
                    _AgreementCheckbox(
                      text: '전체동의',
                      value: _agreeAll,
                      onChanged: _onAgreeAllChanged,
                      isBold: true,
                    ),
                    Divider(height: 24.h),
                    // Terms of service
                    _AgreementCheckbox(
                      text: '[필수] 서비스 이용약관',
                      value: _agreeTerms,
                      onChanged: (value) {
                        setState(() {
                          _agreeTerms = value ?? false;
                        });
                        _updateAgreeAll();
                      },
                      showArrow: true,
                    ),
                    SizedBox(height: 8.h),
                    // Privacy policy
                    _AgreementCheckbox(
                      text: '[필수] 개인정보 처리 방침',
                      value: _agreePrivacy,
                      onChanged: (value) {
                        setState(() {
                          _agreePrivacy = value ?? false;
                        });
                        _updateAgreeAll();
                      },
                      showArrow: true,
                    ),
                    SizedBox(height: 8.h),
                    // Marketing
                    _AgreementCheckbox(
                      text: '[선택] 마케팅 정보 수신 동의',
                      value: _agreeMarketing,
                      onChanged: (value) {
                        setState(() {
                          _agreeMarketing = value ?? false;
                        });
                        _updateAgreeAll();
                      },
                      showArrow: true,
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: MoreBottomButton(
            text: widget.isOnboarding ? '다음' : '저장',
            onPressed: _onSubmit,
            enabled: _isFormValid,
          ),
        ),
      ),
    );
  }
}

class _AgreementCheckbox extends StatelessWidget {
  final String text;
  final bool value;
  final ValueChanged<bool?> onChanged;
  final bool isBold;
  final bool showArrow;

  const _AgreementCheckbox({
    required this.text,
    required this.value,
    required this.onChanged,
    this.isBold = false,
    this.showArrow = false,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => onChanged(!value),
      child: Row(
        children: [
          SizedBox(
            width: 24.w,
            height: 24.w,
            child: Checkbox(
              value: value,
              onChanged: onChanged,
              activeColor: AppColors.orange500,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(4.r),
              ),
              side: BorderSide(
                color: value ? AppColors.orange500 : AppColors.gray100,
                width: 1.5,
              ),
            ),
          ),
          SizedBox(width: 12.w),
          Expanded(
            child: Text(
              text,
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray900,
                fontWeight: isBold ? FontWeight.w700 : FontWeight.w400,
                fontSize: 13.sp,
              ),
            ),
          ),
          if (showArrow)
            Icon(
              Icons.chevron_right,
              size: 20.w,
              color: AppColors.gray500,
            ),
        ],
      ),
    );
  }
}
