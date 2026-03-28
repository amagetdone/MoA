import 'package:flutter_test/flutter_test.dart';

import 'package:alarm_app/main.dart';

void main() {
  testWidgets('alarm app renders main actions', (WidgetTester tester) async {
    await tester.pumpWidget(const AlarmApp());

    expect(find.text('입금 알림'), findsWidgets);
    expect(find.text('테스트 입금 알림 발송'), findsOneWidget);
    expect(find.text('최근 발송 내역'), findsOneWidget);
  });
}
