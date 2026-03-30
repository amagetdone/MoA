import 'package:flutter/services.dart';
import 'package:intl/intl.dart';

abstract class CurrencyFormatter {
  static final _krwFormat = NumberFormat('#,###', 'ko_KR');

  static String formatKRW(int amount) {
    return '${_krwFormat.format(amount)}원';
  }

  static String formatCompact(int amount) {
    if (amount >= 10000) {
      final man = amount ~/ 10000;
      final remainder = amount % 10000;
      if (remainder == 0) return '$man만원';
      return '$man만${_krwFormat.format(remainder)}원';
    }
    return '${_krwFormat.format(amount)}원';
  }

  static String formatWithSign(int amount) {
    final prefix = amount >= 0 ? '+' : '';
    return '$prefix${_krwFormat.format(amount)}원';
  }

  /// 숫자만 남기고 콤마를 제거한 값을 반환
  static int? parseFormatted(String text) {
    final digits = text.replaceAll(',', '').trim();
    return int.tryParse(digits);
  }
}

/// TextField에 적용하면 입력 시 자동으로 콤마가 찍히는 포맷터
/// 예: 3000 → 3,000 / 1500000 → 1,500,000
class CurrencyInputFormatter extends TextInputFormatter {
  static final _format = NumberFormat('#,###', 'ko_KR');

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (newValue.text.isEmpty) return newValue;

    final digits = newValue.text.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.isEmpty) return const TextEditingValue();

    final number = int.tryParse(digits);
    if (number == null) return oldValue;

    final formatted = _format.format(number);

    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}
