import 'package:dio/dio.dart';
import 'package:flutter/cupertino.dart';
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
  String? _selectedIncome;
  bool _isSubmitting = false;

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

  Future<void> _onSubmit() async {
    if (!_isFormValid || _isSubmitting) return;

    if (widget.isOnboarding) {
      setState(() => _isSubmitting = true);
      try {
        // birthDate: "2000.01.17" → "2000-01-17"
        final birthDate = _birthDateController.text.replaceAll('.', '-');
        final dio = ref.read(apiClientProvider).dio;

        await dio.patch(
          'v1/members/me/profile',
          data: {
            'nickname': _nicknameController.text,
            'email': _emailController.text,
            'birthDate': birthDate,
            'hasFixedIncome': _selectedIncome == '있어요',
          },
        );

        if (!mounted) return;
        context.go(AppRoutes.onboarding);
      } on DioException catch (e) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.message ?? '프로필 저장에 실패했습니다.')),
        );
      } finally {
        if (mounted) setState(() => _isSubmitting = false);
      }
    } else {
      context.pop();
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
      '1월', '2월', '3월', '4월', '5월', '6월',
      '7월', '8월', '9월', '10월', '11월', '12월',
    ];
    final years = List.generate(now.year - 1950 + 1, (i) => 1950 + i);

    int monthIndex = selectedMonth - 1;
    int dayIndex = selectedDay - 1;
    int yearIndex = years.indexOf(selectedYear);
    if (yearIndex < 0) yearIndex = years.indexOf(2000);

    final monthController = FixedExtentScrollController(initialItem: monthIndex);
    final dayController = FixedExtentScrollController(initialItem: dayIndex);
    final yearController = FixedExtentScrollController(initialItem: yearIndex);

    int daysInMonth(int year, int month) => DateUtils.getDaysInMonth(year, month);

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
                    padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        GestureDetector(
                          onTap: () => Navigator.pop(ctx),
                          child: Text('취소', style: AppTextStyles.bodyMd.copyWith(color: AppColors.textTertiary)),
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
                          child: Text('확인', style: AppTextStyles.bodyMd.copyWith(color: AppColors.orange500, fontWeight: FontWeight.w600)),
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
                            selectionOverlay: const CupertinoPickerDefaultSelectionOverlay(background: Colors.transparent),
                            onSelectedItemChanged: (i) => setModalState(() => yearIndex = i),
                            children: years.map((y) => Center(
                              child: Text('$y', style: TextStyle(fontSize: 22.sp, fontWeight: FontWeight.w500)),
                            )).toList(),
                          ),
                        ),
                        // 월
                        Expanded(
                          child: CupertinoPicker(
                            scrollController: monthController,
                            itemExtent: 44.h,
                            selectionOverlay: const CupertinoPickerDefaultSelectionOverlay(background: Colors.transparent),
                            onSelectedItemChanged: (i) => setModalState(() => monthIndex = i),
                            children: months.map((m) => Center(
                              child: Text(m, style: TextStyle(fontSize: 22.sp, fontWeight: FontWeight.w500)),
                            )).toList(),
                          ),
                        ),
                        // 일
                        Expanded(
                          child: CupertinoPicker(
                            scrollController: dayController,
                            itemExtent: 44.h,
                            selectionOverlay: const CupertinoPickerDefaultSelectionOverlay(background: Colors.transparent),
                            onSelectedItemChanged: (i) => setModalState(() => dayIndex = i),
                            children: List.generate(maxDay, (i) => Center(
                              child: Text('${i + 1}', style: TextStyle(fontSize: 22.sp, fontWeight: FontWeight.w500)),
                            )),
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
      appBar: MoreAppBar(
        title: widget.isOnboarding ? '프로필 설정' : '내 정보 수정',
        showBack: !widget.isOnboarding,
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
                    borderRadius: BorderRadius.circular(16.r)
                  ),
                  child: Icon(
                    Icons.camera_alt_outlined,
                    size: 32.w,
                    color: AppColors.gray500,
                  ),
                ),
              ),
            ),
            SizedBox(height: 24.h),

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
                  color: AppColors.textSecondary,
                ),
              ),
              SizedBox(height: 8.h),
              GestureDetector(
                onTap: _pickBirthDate,
                child: Container(
                  decoration: BoxDecoration(
                    color: AppColors.bgSurface,
                    borderRadius: BorderRadius.circular(16.r),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.06),
                        offset: Offset(0, 2.h),
                        blurRadius: 8.r,
                      ),
                    ],
                  ),
                  child: AbsorbPointer(
                    child: TextField(
                      controller: _birthDateController,
                      style: AppTextStyles.bodyMd,
                      decoration: InputDecoration(
                        hintText: 'YYYY.MM.DD',
                        hintStyle: AppTextStyles.bodyMd.copyWith(
                          color: AppColors.textTertiary,
                        ),
                        suffixIcon: Icon(
                          Icons.calendar_today_outlined,
                          size: 20.w,
                          color: AppColors.textTertiary,
                        ),
                        filled: true,
                        fillColor: Colors.transparent,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(16.r),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 16.w,
                          vertical: 12.h,
                        ),
                      ),
                    ),
                  ),
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
                child: Row(
                  children: [
                    _ToggleOption(
                      label: '있어요',
                      selected: _selectedIncome == '있어요',
                      onTap: () => setState(() => _selectedIncome = '있어요'),
                      isFirst: true,
                    ),
                    _ToggleOption(
                      label: '없어요',
                      selected: _selectedIncome == '없어요',
                      onTap: () => setState(() => _selectedIncome = '없어요'),
                      isFirst: false,
                    ),
                  ],
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
        text: _isSubmitting ? '저장 중...' : (widget.isOnboarding ? '프로필 설정 완료' : '저장'),
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

class _ToggleOption extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  final bool isFirst;

  const _ToggleOption({
    required this.label,
    required this.selected,
    required this.onTap,
    required this.isFirst,
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
              color: selected ? AppColors.bgSurface : Colors.transparent,
              borderRadius: BorderRadius.circular(12.r),
            ),
            alignment: Alignment.center,
            child: Text(
              label,
              style: AppTextStyles.bodyMd.copyWith(
                color: selected ? AppColors.gray900 : AppColors.textTertiary,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
