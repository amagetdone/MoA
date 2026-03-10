import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreDateField extends StatelessWidget {
  final String label;
  final DateTime? value;
  final ValueChanged<DateTime> onChanged;

  const MoreDateField({
    super.key,
    required this.label,
    this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: AppTextStyles.bodySm.copyWith(color: AppColors.textSecondary)),
        const SizedBox(height: 8),
        GestureDetector(
          onTap: () => _pickDate(context),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 12),
            decoration: const BoxDecoration(
              border: Border(bottom: BorderSide(color: AppColors.borderDefault)),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    value != null
                        ? DateFormat('yyyy.MM.dd').format(value!)
                        : 'YYYY.MM.DD',
                    style: AppTextStyles.bodyMd.copyWith(
                      color: value != null ? AppColors.textPrimary : AppColors.textTertiary,
                    ),
                  ),
                ),
                const Icon(Icons.calendar_today, size: 20, color: AppColors.textTertiary),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _pickDate(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: value ?? DateTime.now(),
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: Theme.of(context).colorScheme.copyWith(
              primary: AppColors.orange500,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      onChanged(picked);
    }
  }
}
