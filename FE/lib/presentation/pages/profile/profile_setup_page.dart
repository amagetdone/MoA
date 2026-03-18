import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';

class ProfileSetupPage extends ConsumerStatefulWidget {
  final bool isOnboarding;
  final Map<String, String>? initialData;

  const ProfileSetupPage({
    super.key,
    this.isOnboarding = true,
    this.initialData,
  });

  @override
  ConsumerState<ProfileSetupPage> createState() => _ProfileSetupPageState();
}

class _ProfileSetupPageState extends ConsumerState<ProfileSetupPage> {
  late final TextEditingController _nicknameController;
  late final TextEditingController _emailController;
  late final TextEditingController _birthDateController;
  final FocusNode _nicknameFocusNode = FocusNode();
  final FocusNode _emailFocusNode = FocusNode();
  String? _selectedIncome;
  bool _isSubmitting = false;
  File? _selectedImage;
  String? _existingImageUrl;

  // Validation errors
  String? _nicknameError;
  String? _emailError;
  String? _birthDateError;

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
    _selectedIncome = widget.initialData?['income'] ?? '있어요';
    _existingImageUrl = widget.initialData?['profileImageUrl'];
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _emailController.dispose();
    _birthDateController.dispose();
    _nicknameFocusNode.dispose();
    _emailFocusNode.dispose();
    super.dispose();
  }

  Future<void> _pickImage() async {
    final picker = ImagePicker();
    final picked = await picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 80,
    );
    if (picked != null) {
      setState(() => _selectedImage = File(picked.path));
    }
  }

  Future<void> _uploadProfileImage() async {
    if (_selectedImage == null) return;
    final dio = ref.read(apiClientProvider).dio;
    final formData = FormData.fromMap({
      'image': await MultipartFile.fromFile(
        _selectedImage!.path,
        filename: _selectedImage!.path.split('/').last,
      ),
    });
    await dio.patch(
      'v1/members/me/profile-image',
      data: formData,
      options: Options(contentType: 'multipart/form-data'),
    );
  }

  bool get _isFormValid {
    final hasBasicInfo =
        _nicknameController.text.trim().isNotEmpty &&
        _emailController.text.trim().isNotEmpty &&
        _birthDateController.text.isNotEmpty &&
        _selectedIncome != null;

    if (widget.isOnboarding) {
      return hasBasicInfo && _agreeTerms && _agreePrivacy;
    }
    return _nicknameController.text.trim().isNotEmpty &&
        _emailController.text.trim().isNotEmpty;
  }

  String? _validateNickname(String value) {
    if (value.trim().isEmpty) return '닉네임을 입력해주세요';
    if (value.trim().length < 2) return '닉네임은 2자 이상이어야 합니다';
    return null;
  }

  String? _validateEmail(String value) {
    if (value.trim().isEmpty) return '이메일을 입력해주세요';
    final emailRegex = RegExp(r'^[\w\.-]+@[\w\.-]+\.\w{2,}$');
    if (!emailRegex.hasMatch(value.trim())) return '올바른 이메일 형식이 아닙니다';
    return null;
  }

  bool _validateAll() {
    final nicknameErr = _validateNickname(_nicknameController.text);
    final emailErr = _validateEmail(_emailController.text);
    final birthErr = widget.isOnboarding && _birthDateController.text.isEmpty
        ? '생년월일을 선택해주세요'
        : null;

    setState(() {
      _nicknameError = nicknameErr;
      _emailError = emailErr;
      _birthDateError = birthErr;
    });

    // 첫 번째 에러 필드에 포커스
    if (nicknameErr != null) {
      _nicknameFocusNode.requestFocus();
    } else if (emailErr != null) {
      _emailFocusNode.requestFocus();
    }

    return nicknameErr == null && emailErr == null && birthErr == null;
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

  Future<void> _onSubmit() async {
    if (_isSubmitting) return;
    if (!_validateAll() || !_isFormValid) return;

    setState(() => _isSubmitting = true);
    try {
      final dio = ref.read(apiClientProvider).dio;

      await dio.patch(
        'v1/members/me/profile',
        data: {
          'nickname': _nicknameController.text.trim(),
          'email': _emailController.text.trim(),
          'birthDate': _birthDateController.text.trim().replaceAll('.', '-'),
          'hasFixedIncome': _selectedIncome == '있어요',
        },
      );

      if (_selectedImage != null) {
        await _uploadProfileImage();
      }

      if (!mounted) return;

      if (widget.isOnboarding) {
        context.go(AppRoutes.onboarding);
      } else {
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('프로필 저장에 실패했습니다.')),
        );
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _pickBirthDate() async {
    final now = DateTime.now();
    int selectedYear = 2000;
    int selectedMonth = 1;
    int selectedDay = 17;

    // 기존 값이 있으면 파싱
    if (_birthDateController.text.isNotEmpty) {
      final parts = _birthDateController.text.split('.');
      if (parts.length == 3) {
        selectedYear = int.tryParse(parts[0]) ?? 2000;
        selectedMonth = int.tryParse(parts[1]) ?? 1;
        selectedDay = int.tryParse(parts[2]) ?? 1;
      }
    }

    const months = [
      '1월',
      '2월',
      '3월',
      '4월',
      '5월',
      '6월',
      '7월',
      '8월',
      '9월',
      '10월',
      '11월',
      '12월',
    ];
    final years = List.generate(now.year - 1950 + 1, (i) => 1950 + i);

    int monthIndex = selectedMonth - 1;
    int dayIndex = selectedDay - 1;
    int yearIndex = years.indexOf(selectedYear);
    if (yearIndex < 0) yearIndex = years.indexOf(2000);

    final monthController = FixedExtentScrollController(
      initialItem: monthIndex,
    );
    final dayController = FixedExtentScrollController(initialItem: dayIndex);
    final yearController = FixedExtentScrollController(initialItem: yearIndex);

    int daysInMonth(int year, int month) =>
        DateUtils.getDaysInMonth(year, month);

    await showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setModalState) {
            final maxDay = daysInMonth(years[yearIndex], monthIndex + 1);
            if (dayIndex >= maxDay) {
              dayIndex = maxDay - 1;
            }
            return Container(
              height: 320.h,
              decoration: BoxDecoration(
                color: AppColors.bgSurface,
                borderRadius: BorderRadius.vertical(top: Radius.circular(20.r)),
              ),
              child: Column(
                children: [
                  // 상단 바
                  Padding(
                    padding: EdgeInsets.symmetric(
                      horizontal: 16.w,
                      vertical: 12.h,
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        GestureDetector(
                          onTap: () => Navigator.pop(ctx),
                          child: Text(
                            '취소',
                            style: AppTextStyles.bodyMd.copyWith(
                              color: AppColors.textTertiary,
                            ),
                          ),
                        ),
                        GestureDetector(
                          onTap: () {
                            final day = (dayIndex + 1).clamp(1, maxDay);
                            setState(() {
                              _birthDateController.text =
                                  '${years[yearIndex]}.${(monthIndex + 1).toString().padLeft(2, '0')}.${day.toString().padLeft(2, '0')}';
                            });
                            Navigator.pop(ctx);
                          },
                          child: Text(
                            '확인',
                            style: AppTextStyles.bodyMd.copyWith(
                              color: AppColors.orange500,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  Divider(height: 1, color: AppColors.borderDefault),
                  // 피커
                  Expanded(
                    child: Row(
                      children: [
                        // 년
                        Expanded(
                          child: CupertinoPicker(
                            scrollController: yearController,
                            itemExtent: 44.h,
                            selectionOverlay:
                                const CupertinoPickerDefaultSelectionOverlay(
                                  background: Colors.transparent,
                                ),
                            onSelectedItemChanged: (i) =>
                                setModalState(() => yearIndex = i),
                            children: years
                                .map(
                                  (y) => Center(
                                    child: Text(
                                      '$y',
                                      style: TextStyle(
                                        fontSize: 22.sp,
                                        fontWeight: FontWeight.w500,
                                      ),
                                    ),
                                  ),
                                )
                                .toList(),
                          ),
                        ),
                        // 월
                        Expanded(
                          child: CupertinoPicker(
                            scrollController: monthController,
                            itemExtent: 44.h,
                            selectionOverlay:
                                const CupertinoPickerDefaultSelectionOverlay(
                                  background: Colors.transparent,
                                ),
                            onSelectedItemChanged: (i) =>
                                setModalState(() => monthIndex = i),
                            children: months
                                .map(
                                  (m) => Center(
                                    child: Text(
                                      m,
                                      style: TextStyle(
                                        fontSize: 22.sp,
                                        fontWeight: FontWeight.w500,
                                      ),
                                    ),
                                  ),
                                )
                                .toList(),
                          ),
                        ),
                        // 일
                        Expanded(
                          child: CupertinoPicker(
                            scrollController: dayController,
                            itemExtent: 44.h,
                            selectionOverlay:
                                const CupertinoPickerDefaultSelectionOverlay(
                                  background: Colors.transparent,
                                ),
                            onSelectedItemChanged: (i) =>
                                setModalState(() => dayIndex = i),
                            children: List.generate(
                              maxDay,
                              (i) => Center(
                                child: Text(
                                  '${i + 1}',
                                  style: TextStyle(
                                    fontSize: 22.sp,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(title: '', showBack: !widget.isOnboarding),
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '프로필 설정',
              style: AppTextStyles.headingLg.copyWith(
                fontSize: 24.sp,
                fontWeight: FontWeight.bold,
              ),
            ),
            SizedBox(height: 24.h),
            // Profile photo
            Center(
              child: Column(
                children: [
                  GestureDetector(
                    onTap: _pickImage,
                    child: Container(
                      width: 88.w,
                      height: 88.w,
                      decoration: BoxDecoration(
                        color: AppColors.gray100,
                        borderRadius: BorderRadius.circular(16.r),
                        image: _selectedImage != null
                            ? DecorationImage(
                                image: FileImage(_selectedImage!),
                                fit: BoxFit.cover,
                              )
                            : _existingImageUrl != null
                                ? DecorationImage(
                                    image: NetworkImage(_existingImageUrl!),
                                    fit: BoxFit.cover,
                                  )
                                : null,
                      ),
                      child: _selectedImage == null && _existingImageUrl == null
                          ? Icon(
                              Icons.camera_alt_outlined,
                              size: 32.w,
                              color: AppColors.gray500,
                            )
                          : null,
                    ),
                  ),
                  SizedBox(height: 12.h),
                  if (widget.isOnboarding)
                    Text('등록하지 않으면 기본 이미지로 설정돼요.', style: AppTextStyles.bodySm)
                  else
                    GestureDetector(
                      onTap: () {
                        setState(() {
                          _selectedImage = null;
                          _existingImageUrl = null;
                        });
                      },
                      child: Text(
                        '기본 이미지로 변경하기',
                        style: AppTextStyles.bodySm.copyWith(
                          color: AppColors.textTertiary,
                          decoration: TextDecoration.underline,
                        ),
                      ),
                    ),
                ],
              ),
            ),
            SizedBox(height: 24.h),

            // Nickname
            MoreTextField(
              label: '닉네임',
              hint: '닉네임을 입력해주세요',
              controller: _nicknameController,
              focusNode: _nicknameFocusNode,
              errorText: _nicknameError,
              onChanged: (value) => setState(() {
                _nicknameError = _validateNickname(value);
              }),
            ),
            SizedBox(height: 24.h),

            // Email
            MoreTextField(
              label: '이메일',
              hint: '이메일을 입력해주세요',
              controller: _emailController,
              focusNode: _emailFocusNode,
              keyboardType: TextInputType.emailAddress,
              errorText: _emailError,
              onChanged: (value) => setState(() {
                _emailError = _validateEmail(value);
              }),
            ),
            SizedBox(height: 24.h),

            if (widget.isOnboarding) ...[
              // Birth date with calendar icon
              MoreTextField(
                label: '생년월일',
                hint: 'YYYY.MM.DD',
                controller: _birthDateController,
                readOnly: true,
                onTap: () async {
                  await _pickBirthDate();
                  if (_birthDateController.text.isNotEmpty) {
                    setState(() => _birthDateError = null);
                  }
                },
                errorText: _birthDateError,
                suffixIcon: Icon(
                  Icons.calendar_today,
                  size: 20.w,
                  color: AppColors.textTertiary,
                ),
              ),
              SizedBox(height: 24.h),

              // 고정 수입 여부
              Text(
                '고정 수입 여부',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              SizedBox(height: 8.h),
              Container(
                decoration: BoxDecoration(
                  color: AppColors.gray100,
                  borderRadius: BorderRadius.circular(16.r),
                  border: Border.all(color: AppColors.borderDefault),
                ),
                child: IntrinsicHeight(
                  child: Stack(
                    children: [
                      // 슬라이딩 배경
                      AnimatedAlign(
                        duration: const Duration(milliseconds: 250),
                        curve: Curves.easeInOut,
                        alignment: _selectedIncome == '있어요'
                            ? Alignment.centerLeft
                            : Alignment.centerRight,
                        child: FractionallySizedBox(
                          widthFactor: 0.5,
                          heightFactor: 1.0,
                          child: Padding(
                            padding: EdgeInsets.all(4.w),
                            child: Container(
                              decoration: BoxDecoration(
                                color: AppColors.bgSurface,
                                borderRadius: BorderRadius.circular(12.r),
                              ),
                            ),
                          ),
                        ),
                      ),
                      // 텍스트 레이어
                      Row(
                        children: [
                          _ToggleOption(
                            label: '있어요',
                            selected: _selectedIncome == '있어요',
                            onTap: () =>
                                setState(() => _selectedIncome = '있어요'),
                          ),
                          _ToggleOption(
                            label: '없어요',
                            selected: _selectedIncome == '없어요',
                            onTap: () =>
                                setState(() => _selectedIncome = '없어요'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),

              // Agreement section
              SizedBox(height: 24.h),
              Container(
                padding: EdgeInsets.all(16.w),
                decoration: BoxDecoration(
                  color: AppColors.bgSurface,
                  borderRadius: BorderRadius.circular(16.r),
                  border: Border.all(color: AppColors.borderDefault),
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
      bottomNavigationBar: MoreBottomButton(
        text: _isSubmitting
            ? '저장 중...'
            : (widget.isOnboarding ? '프로필 설정 완료' : '저장'),
        onPressed: _onSubmit,
        enabled: _isFormValid && !_isSubmitting,
        progress: null,
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
            Icon(Icons.chevron_right, size: 20.w, color: AppColors.gray500),
        ],
      ),
    );
  }
}

class _ToggleOption extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _ToggleOption({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: Padding(
          padding: EdgeInsets.all(4.w),
          child: Container(
            padding: EdgeInsets.symmetric(vertical: 10.h),
            decoration: BoxDecoration(
              color: Colors.transparent,
              borderRadius: BorderRadius.circular(12.r),
            ),
            alignment: Alignment.center,
            child: AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 250),
              style: AppTextStyles.bodyMd.copyWith(
                color: selected ? AppColors.gray900 : AppColors.textTertiary,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
              ),
              child: Text(label),
            ),
          ),
        ),
      ),
    );
  }
}
