import 'package:intl/intl.dart';

abstract class CurrencyFormatter {
  static final _krwFormat = NumberFormat('#,###', 'ko_KR');

  static String formatKRW(int amount) {
    return '${_krwFormat.format(amount)}원';
  }

  static String formatWithSign(int amount) {
    final prefix = amount >= 0 ? '+' : '';
    return '$prefix${_krwFormat.format(amount)}원';
  }
}
