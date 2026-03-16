import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreDateField extends StatelessWidget {
  final String label;
  final DateTime? value;
  final ValueChanged<DateTime> onChanged;
  final DateTime? firstDate;
  final DateTime? lastDate;

  const MoreDateField({
    super.key,
    required this.label,
    this.value,
    required this.onChanged,
    this.firstDate,
    this.lastDate,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        SizedBox(height: 8.h),
        GestureDetector(
          onTap: () => _pickDate(context),
          child: Container(
            padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
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
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    value != null
                        ? DateFormat('yyyy.MM.dd').format(value!)
                        : 'YYYY.MM.DD',
                    style: AppTextStyles.bodyMd.copyWith(
                      color: value != null
                          ? AppColors.textPrimary
                          : AppColors.textTertiary,
                    ),
                  ),
                ),
                Icon(
                  Icons.calendar_today_outlined,
                  size: 20.w,
                  color: AppColors.textTertiary,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _pickDate(BuildContext context) async {
    final now = DateTime.now();
    final initial = value ?? DateTime(now.year, now.month, now.day);

    const months = [
      '1월', '2월', '3월', '4월', '5월', '6월',
      '7월', '8월', '9월', '10월', '11월', '12월',
    ];
    final minYear = firstDate?.year ?? 1950;
    final maxYear = lastDate?.year ?? (now.year + 10);
    final years = List.generate(maxYear - minYear + 1, (i) => minYear + i);

    int yearIndex = years.indexOf(initial.year);
    if (yearIndex < 0) yearIndex = years.indexOf(now.year);
    int monthIndex = initial.month - 1;
    int dayIndex = initial.day - 1;

    final yearController = FixedExtentScrollController(initialItem: yearIndex);
    final monthController = FixedExtentScrollController(initialItem: monthIndex);
    final dayController = FixedExtentScrollController(initialItem: dayIndex);

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
            final currentDay = (dayIndex + 1).clamp(1, maxDay);
            final currentSelected = DateTime(years[yearIndex], monthIndex + 1, currentDay);
            final isTooEarly = firstDate != null && currentSelected.isBefore(DateTime(firstDate!.year, firstDate!.month, firstDate!.day));
            final isTooLate = lastDate != null && currentSelected.isAfter(DateTime(lastDate!.year, lastDate!.month, lastDate!.day));
            final isInvalid = isTooEarly || isTooLate;
            return Container(
              height: 360.h,
              decoration: BoxDecoration(
                color: AppColors.bgSurface,
                borderRadius: BorderRadius.vertical(top: Radius.circular(20.r)),
              ),
              child: Column(
                children: [
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
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
                          onTap: isInvalid
                              ? null
                              : () {
                                  onChanged(currentSelected);
                                  Navigator.pop(ctx);
                                },
                          child: Text(
                            '확인',
                            style: AppTextStyles.bodyMd.copyWith(
                              color: isInvalid ? AppColors.gray100 : AppColors.orange500,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  Divider(height: 1, color: AppColors.borderDefault),
                  if (isInvalid)
                    Padding(
                      padding: EdgeInsets.only(top: 8.h),
                      child: Text(
                        isTooEarly
                            ? '${DateFormat('yyyy.MM.dd').format(firstDate!)} 이후 날짜를 선택해주세요.'
                            : '${DateFormat('yyyy.MM.dd').format(lastDate!)} 이전 날짜를 선택해주세요.',
                        style: AppTextStyles.bodySm.copyWith(
                          color: Colors.red,
                          fontSize: 12.sp,
                        ),
                      ),
                    ),
                  Expanded(
                    child: Row(
                      children: [
                        // 년
                        Expanded(
                          child: CupertinoPicker(
                            scrollController: yearController,
                            itemExtent: 44.h,
                            selectionOverlay: const CupertinoPickerDefaultSelectionOverlay(
                              background: Colors.transparent,
                            ),
                            onSelectedItemChanged: (i) =>
                                setModalState(() => yearIndex = i),
                            children: years
                                .map((y) => Center(
                                      child: Text(
                                        '$y',
                                        style: TextStyle(
                                          fontSize: 22.sp,
                                          fontWeight: FontWeight.w500,
                                        ),
                                      ),
                                    ))
                                .toList(),
                          ),
                        ),
                        // 월
                        Expanded(
                          child: CupertinoPicker(
                            scrollController: monthController,
                            itemExtent: 44.h,
                            selectionOverlay: const CupertinoPickerDefaultSelectionOverlay(
                              background: Colors.transparent,
                            ),
                            onSelectedItemChanged: (i) =>
                                setModalState(() => monthIndex = i),
                            children: months
                                .map((m) => Center(
                                      child: Text(
                                        m,
                                        style: TextStyle(
                                          fontSize: 22.sp,
                                          fontWeight: FontWeight.w500,
                                        ),
                                      ),
                                    ))
                                .toList(),
                          ),
                        ),
                        // 일
                        Expanded(
                          child: CupertinoPicker(
                            scrollController: dayController,
                            itemExtent: 44.h,
                            selectionOverlay: const CupertinoPickerDefaultSelectionOverlay(
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
}
