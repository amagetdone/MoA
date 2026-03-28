import 'dart:typed_data';

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
import 'package:more_app/presentation/viewmodels/menu_viewmodel.dart';
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
  static const int _maxProfileImageBytes = 3 * 1024 * 1024;

  late final TextEditingController _nicknameController;
  late final TextEditingController _emailController;
  late final TextEditingController _birthDateController;
  final FocusNode _nicknameFocusNode = FocusNode();
  final FocusNode _emailFocusNode = FocusNode();
  String? _selectedIncome;
  bool _isSubmitting = false;
  Uint8List? _selectedImageBytes;
  String? _selectedImageName;
  String? _existingImageUrl;
  bool _useDefaultImage = false;

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
      final bytes = await picked.readAsBytes();
      if (bytes.length > _maxProfileImageBytes) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('최대 3MB 이미지만 첨부할 수 있어요.')),
        );
        return;
      }
      setState(() {
        _selectedImageBytes = bytes;
        _selectedImageName = _resolvePickedImageName(picked);
        _useDefaultImage = false;
      });
    }
  }

  String _resolvePickedImageName(XFile picked) {
    if (picked.name.isNotEmpty && picked.name.contains('.')) {
      return picked.name;
    }
    final segments = picked.path.split(RegExp(r'[\\/]'));
    final fileName = segments.isNotEmpty ? segments.last : '';
    if (fileName.isNotEmpty && fileName.contains('.')) {
      return fileName;
    }
    return 'profile_image.jpg';
  }

  Future<void> _uploadProfileImage() async {
    final dio = ref.read(apiClientProvider).dio;
    final MultipartFile imageFile;
    if (_selectedImageBytes != null) {
      imageFile = MultipartFile.fromBytes(
        _selectedImageBytes!,
        filename: _selectedImageName ?? 'profile_image.jpg',
      );
    } else {
      final byteData = await DefaultAssetBundle.of(context).load('assets/images/default_profile.png');
      imageFile = MultipartFile.fromBytes(
        byteData.buffer.asUint8List(),
        filename: 'default_profile.png',
      );
    }
    final formData = FormData.fromMap({'image': imageFile});
    await dio.patch(
      'v1/members/me/profile-image',
      data: formData,
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

  void _showAgreementSheet(_AgreementDocument document) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) {
        final colors = AppColors.of(ctx);
        return Container(
          height: 620.h,
          decoration: BoxDecoration(
            color: colors.bgSurface,
            borderRadius: BorderRadius.vertical(top: Radius.circular(24.r)),
          ),
          child: SafeArea(
            top: false,
            child: Padding(
              padding: EdgeInsets.fromLTRB(24.w, 16.h, 24.w, 24.h),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 44.w,
                      height: 4.h,
                      decoration: BoxDecoration(
                        color: colors.gray100,
                        borderRadius: BorderRadius.circular(999.r),
                      ),
                    ),
                  ),
                  SizedBox(height: 20.h),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          document.title,
                          style: AppTextStyles.headingMd.copyWith(
                            color: colors.textPrimary,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      IconButton(
                        onPressed: () => Navigator.of(ctx).pop(),
                        icon: Icon(
                          Icons.close,
                          color: colors.textTertiary,
                          size: 22.w,
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 8.h),
                  Text(
                    document.summary,
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textSecondary,
                      height: 1.5,
                    ),
                  ),
                  SizedBox(height: 20.h),
                  Expanded(
                    child: SingleChildScrollView(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: document.sections
                            .map(
                              (section) => Padding(
                                padding: EdgeInsets.only(bottom: 20.h),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      section.title,
                                      style: AppTextStyles.bodyMd.copyWith(
                                        color: colors.textPrimary,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                    SizedBox(height: 8.h),
                                    Text(
                                      section.body,
                                      style: AppTextStyles.bodySm.copyWith(
                                        color: colors.textSecondary,
                                        height: 1.65,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            )
                            .toList(),
                      ),
                    ),
                  ),
                  SizedBox(height: 8.h),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () => Navigator.of(ctx).pop(),
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.orange500,
                        foregroundColor: Colors.white,
                        padding: EdgeInsets.symmetric(vertical: 16.h),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16.r),
                        ),
                      ),
                      child: Text(
                        '확인했어요',
                        style: AppTextStyles.bodyMd.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
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

      if (_selectedImageBytes != null || _useDefaultImage) {
        await _uploadProfileImage();
      }

      if (!mounted) return;

      ref.read(menuViewModelProvider.notifier).loadData();

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
    int selectedYear = now.year;
    int selectedMonth = now.month;
    int selectedDay = now.day;

    // 기존 값이 있으면 파싱
    if (_birthDateController.text.isNotEmpty) {
      final parts = _birthDateController.text.split('.');
      if (parts.length == 3) {
        selectedYear = int.tryParse(parts[0]) ?? now.year;
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
    if (yearIndex < 0) yearIndex = years.indexOf(now.year);

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
        final colors = AppColors.of(ctx);
        return StatefulBuilder(
          builder: (ctx, setModalState) {
            final maxDay = daysInMonth(years[yearIndex], monthIndex + 1);
            if (dayIndex >= maxDay) {
              dayIndex = maxDay - 1;
            }
            return Container(
              height: 320.h,
              decoration: BoxDecoration(
                color: colors.bgSurface,
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
                              color: colors.textTertiary,
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
                  Divider(height: 1, color: colors.borderDefault),
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
    final colors = AppColors.of(context);
    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: MoreAppBar(title: '', showBack: !widget.isOnboarding),
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '프로필 설정',
              style: AppTextStyles.headingLg.copyWith(
                color: colors.textPrimary,
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
                        color: colors.gray100,
                        borderRadius: BorderRadius.circular(16.r),
                        image: _selectedImageBytes != null
                            ? DecorationImage(
                                image: MemoryImage(_selectedImageBytes!),
                                fit: BoxFit.cover,
                              )
                            : _useDefaultImage
                                ? const DecorationImage(
                                    image: AssetImage('assets/images/default_profile.png'),
                                    fit: BoxFit.cover,
                                  )
                                : _existingImageUrl != null
                                    ? DecorationImage(
                                        image: NetworkImage(_existingImageUrl!),
                                        fit: BoxFit.cover,
                                      )
                                    : null,
                      ),
                      child: _selectedImageBytes == null && !_useDefaultImage && _existingImageUrl == null
                          ? Icon(
                              Icons.camera_alt_outlined,
                              size: 32.w,
                              color: colors.gray500,
                            )
                          : null,
                    ),
                  ),
                  SizedBox(height: 12.h),
                  if (widget.isOnboarding)
                    Text(
                      '등록하지 않으면 기본 이미지로 설정돼요.',
                      style: AppTextStyles.bodySm.copyWith(color: colors.textSecondary),
                    )
                  else
                    GestureDetector(
                      onTap: () {
                        setState(() {
                          _selectedImageBytes = null;
                          _selectedImageName = null;
                          _existingImageUrl = null;
                          _useDefaultImage = true;
                        });
                      },
                      child: Text(
                        '기본 이미지로 변경하기',
                        style: AppTextStyles.bodySm.copyWith(
                          color: colors.textTertiary,
                          decoration: TextDecoration.underline,
                        ),
                        ),
                      ),
                  SizedBox(height: 6.h),
                  Text(
                    '최대 3MB 이미지만 첨부할 수 있어요.',
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textTertiary,
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
                suffixIcon: Padding(
                  padding: EdgeInsets.only(right: 12.w),
                  child: Icon(
                    Icons.calendar_today,
                    size: 20.w,
                    color: colors.textTertiary,
                  ),
                ),
              ),
              SizedBox(height: 24.h),

              // 고정 수입 여부
              Text(
                '고정 수입 여부',
                style: AppTextStyles.bodySm.copyWith(
                  color: colors.textSecondary,
                ),
              ),
              SizedBox(height: 8.h),
              Container(
                decoration: BoxDecoration(
                  color: colors.gray100,
                  borderRadius: BorderRadius.circular(16.r),
                  border: Border.all(color: colors.borderDefault),
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
                                color: colors.bgSurface,
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
                  color: colors.bgSurface,
                  borderRadius: BorderRadius.circular(16.r),
                  border: Border.all(color: colors.borderDefault),
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
                      onDetailTap: () =>
                          _showAgreementSheet(_AgreementDocument.terms()),
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
                      onDetailTap: () =>
                          _showAgreementSheet(_AgreementDocument.privacy()),
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
                      onDetailTap: () =>
                          _showAgreementSheet(_AgreementDocument.marketing()),
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
  final VoidCallback? onDetailTap;

  const _AgreementCheckbox({
    required this.text,
    required this.value,
    required this.onChanged,
    this.isBold = false,
    this.showArrow = false,
    this.onDetailTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Row(
      children: [
        GestureDetector(
          onTap: () => onChanged(!value),
          child: SizedBox(
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
                color: value ? AppColors.orange500 : colors.gray100,
                width: 1.5,
              ),
            ),
          ),
        ),
        SizedBox(width: 12.w),
        Expanded(
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: showArrow ? onDetailTap : () => onChanged(!value),
            child: Padding(
              padding: EdgeInsets.symmetric(vertical: 8.h),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      text,
                      style: AppTextStyles.bodySm.copyWith(
                        color: colors.gray900,
                        fontWeight: isBold ? FontWeight.w700 : FontWeight.w400,
                        fontSize: 13.sp,
                        decoration: showArrow
                            ? TextDecoration.underline
                            : TextDecoration.none,
                        decorationColor: colors.textTertiary,
                      ),
                    ),
                  ),
                  if (showArrow)
                    Icon(
                      Icons.chevron_right,
                      size: 20.w,
                      color: colors.gray500,
                    ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _AgreementDocument {
  final String title;
  final String summary;
  final List<_AgreementSection> sections;

  const _AgreementDocument({
    required this.title,
    required this.summary,
    required this.sections,
  });

  factory _AgreementDocument.terms() {
    return const _AgreementDocument(
      title: '서비스 이용약관',
      summary:
          '모아 서비스 이용을 위한 기본 약관입니다. 계좌 연동, 소비 분석, 목표 관리, 알림, AI 코칭 기능 사용 시 적용됩니다.',
      sections: [
        _AgreementSection(
          title: '1. 서비스 제공 범위',
          body:
              '모아는 사용자의 금융 데이터를 기반으로 소비 현황, 예산 관리, 목표 저축 현황, 알림 및 AI 코칭 기능을 제공합니다. 서비스 세부 기능은 운영 정책에 따라 변경되거나 추가될 수 있습니다.',
        ),
        _AgreementSection(
          title: '2. 회원의 의무',
          body:
              '회원은 본인 명의의 정보를 정확하게 입력해야 하며, 타인의 정보를 무단으로 사용하거나 서비스 운영을 방해하는 행위를 해서는 안 됩니다. 부정 사용이 확인될 경우 서비스 이용이 제한될 수 있습니다.',
        ),
        _AgreementSection(
          title: '3. 연동 정보의 활용',
          body:
              '회원이 연결한 계좌 및 거래 정보는 소비 분석, 예산 추천, 목표 저축 추적, 맞춤형 피드백 제공을 위해 활용됩니다. 서비스는 회원이 동의한 범위에서만 정보를 처리합니다.',
        ),
        _AgreementSection(
          title: '4. 책임 및 제한',
          body:
              '모아가 제공하는 분석 및 추천 결과는 참고 정보이며, 실제 금융 의사결정에 대한 최종 책임은 회원 본인에게 있습니다. 시스템 점검, 외부 연동 장애 등으로 일시적으로 서비스 이용이 제한될 수 있습니다.',
        ),
      ],
    );
  }

  factory _AgreementDocument.privacy() {
    return const _AgreementDocument(
      title: '개인정보 처리방침',
      summary:
          '모아는 회원가입과 서비스 제공에 필요한 최소한의 개인정보를 수집하며, 관련 법령과 내부 정책에 따라 안전하게 관리합니다.',
      sections: [
        _AgreementSection(
          title: '1. 수집 항목',
          body:
              '회원가입 및 서비스 이용 과정에서 닉네임, 이메일, 생년월일, 프로필 이미지, 계좌 및 거래 정보, 목표 및 예산 설정 정보가 수집될 수 있습니다.',
        ),
        _AgreementSection(
          title: '2. 이용 목적',
          body:
              '수집한 정보는 회원 식별, 프로필 관리, 금융 데이터 분석, 맞춤형 예산 및 목표 관리, 알림 발송, AI 코칭 제공, 서비스 품질 향상을 위해 사용됩니다.',
        ),
        _AgreementSection(
          title: '3. 보관 및 파기',
          body:
              '회원 탈퇴 또는 목적 달성 후에는 관련 법령에 따라 일정 기간 보관이 필요한 경우를 제외하고 지체 없이 파기합니다. 파기 전까지는 접근 권한을 최소화하여 안전하게 보호합니다.',
        ),
        _AgreementSection(
          title: '4. 이용자 권리',
          body:
              '회원은 언제든지 본인의 개인정보를 열람, 수정, 삭제 요청할 수 있으며, 수집 및 이용 동의를 철회할 수 있습니다. 관련 요청은 서비스 내 문의 또는 운영 채널을 통해 접수할 수 있습니다.',
        ),
      ],
    );
  }

  factory _AgreementDocument.marketing() {
    return const _AgreementDocument(
      title: '마케팅 정보 수신 동의',
      summary:
          '선택 동의 시 서비스 업데이트, 이벤트, 맞춤형 추천 등 안내 메시지를 수신할 수 있습니다. 동의하지 않아도 기본 서비스 이용에는 제한이 없습니다.',
      sections: [
        _AgreementSection(
          title: '1. 수신 항목',
          body:
              '앱 푸시, 문자, 이메일 등으로 이벤트, 신규 기능, 프로모션, 개인화 추천 혜택과 같은 마케팅 정보를 전달할 수 있습니다.',
        ),
        _AgreementSection(
          title: '2. 활용 범위',
          body:
              '회원의 소비 패턴, 목표 설정 여부, 서비스 이용 이력 등을 바탕으로 관심도가 높을 것으로 예상되는 정보가 선별적으로 발송될 수 있습니다.',
        ),
        _AgreementSection(
          title: '3. 동의 철회',
          body:
              '마케팅 수신 동의는 언제든지 설정 화면 또는 고객 문의를 통해 철회할 수 있으며, 철회 이후에는 홍보성 메시지 발송이 중단됩니다.',
        ),
      ],
    );
  }
}

class _AgreementSection {
  final String title;
  final String body;

  const _AgreementSection({
    required this.title,
    required this.body,
  });
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
    final colors = AppColors.of(context);
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
                color: selected ? colors.gray900 : colors.textTertiary,
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
