import 'package:flutter/material.dart';
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
  late final TextEditingController _birthDateController;
  String? _selectedGender;

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
    _birthDateController = TextEditingController(
      text: widget.initialData?['birthDate'] ?? '',
    );
    _selectedGender = widget.initialData?['gender'];
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _birthDateController.dispose();
    super.dispose();
  }

  bool get _isFormValid {
    final hasBasicInfo = _nicknameController.text.isNotEmpty &&
        _birthDateController.text.isNotEmpty &&
        _selectedGender != null;

    if (widget.isOnboarding) {
      return hasBasicInfo && _agreeTerms && _agreePrivacy;
    }
    return hasBasicInfo;
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
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Nickname
            MoreTextField(
              label: '닉네임',
              hint: '닉네임을 입력해주세요',
              controller: _nicknameController,
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 24),

            // Birth date
            MoreTextField(
              label: '생년월일',
              hint: 'YYYY.MM.DD',
              controller: _birthDateController,
              keyboardType: TextInputType.datetime,
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 24),

            // Gender
            Text(
              '성별',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray700,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                MoreChip(
                  label: '남',
                  selected: _selectedGender == '남',
                  onTap: () {
                    setState(() {
                      _selectedGender = '남';
                    });
                  },
                ),
                const SizedBox(width: 12),
                MoreChip(
                  label: '여',
                  selected: _selectedGender == '여',
                  onTap: () {
                    setState(() {
                      _selectedGender = '여';
                    });
                  },
                ),
              ],
            ),

            // Agreement section (only for onboarding)
            if (widget.isOnboarding) ...[
              const SizedBox(height: 40),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.gray50,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  children: [
                    // Agree all
                    _AgreementCheckbox(
                      text: '전체 동의',
                      value: _agreeAll,
                      onChanged: _onAgreeAllChanged,
                      isBold: true,
                    ),
                    const Divider(height: 24),
                    // Terms of service
                    _AgreementCheckbox(
                      text: '서비스 이용약관 (필수)',
                      value: _agreeTerms,
                      onChanged: (value) {
                        setState(() {
                          _agreeTerms = value ?? false;
                        });
                        _updateAgreeAll();
                      },
                    ),
                    const SizedBox(height: 8),
                    // Privacy policy
                    _AgreementCheckbox(
                      text: '개인정보 처리방침 (필수)',
                      value: _agreePrivacy,
                      onChanged: (value) {
                        setState(() {
                          _agreePrivacy = value ?? false;
                        });
                        _updateAgreeAll();
                      },
                    ),
                    const SizedBox(height: 8),
                    // Marketing
                    _AgreementCheckbox(
                      text: '마케팅 정보 수신 (선택)',
                      value: _agreeMarketing,
                      onChanged: (value) {
                        setState(() {
                          _agreeMarketing = value ?? false;
                        });
                        _updateAgreeAll();
                      },
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
          padding: const EdgeInsets.all(24),
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

  const _AgreementCheckbox({
    required this.text,
    required this.value,
    required this.onChanged,
    this.isBold = false,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => onChanged(!value),
      child: Row(
        children: [
          SizedBox(
            width: 24,
            height: 24,
            child: Checkbox(
              value: value,
              onChanged: onChanged,
              activeColor: AppColors.orange500,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(4),
              ),
              side: BorderSide(
                color: value ? AppColors.orange500 : AppColors.gray100,
                width: 1.5,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray900,
                fontWeight: isBold ? FontWeight.w700 : FontWeight.w400,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
