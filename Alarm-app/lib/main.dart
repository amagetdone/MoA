import 'dart:math';

import 'package:alarm_app/alarm_push_service.dart';
import 'package:alarm_app/alarm_notification_service.dart';
import 'package:flutter/material.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AlarmNotificationService.instance.initialize();
  await AlarmPushService.instance.initialize(
    onForegroundMessage: AlarmNotificationService.instance.showRemoteMessage,
  );
  runApp(const AlarmApp());
}

class AlarmApp extends StatelessWidget {
  const AlarmApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '입금 알림',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF3B82F6)),
        scaffoldBackgroundColor: const Color(0xFFF5F7FA),
        useMaterial3: true,
      ),
      home: const AlarmHomePage(),
    );
  }
}

class AlarmHomePage extends StatefulWidget {
  const AlarmHomePage({super.key});

  @override
  State<AlarmHomePage> createState() => _AlarmHomePageState();
}

class _AlarmHomePageState extends State<AlarmHomePage> {
  final TextEditingController _codeController = TextEditingController();
  final List<_AlarmLog> _logs = <_AlarmLog>[];
  String? _fcmToken;

  @override
  void initState() {
    super.initState();
    _codeController.text = _generateCode();
    _loadFcmToken();
  }

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _loadFcmToken() async {
    final token = await AlarmPushService.instance.getToken();
    if (!mounted) return;
    setState(() => _fcmToken = token);
  }

  String _generateCode() {
    final random = Random();
    return List.generate(4, (_) => random.nextInt(10)).join();
  }

  Future<void> _sendVerificationAlarm() async {
    final authCode = (_codeController.text.trim().isEmpty
            ? _generateCode()
            : _codeController.text.trim())
        .replaceAll(RegExp(r'[^0-9]'), '')
        .padRight(4, '0')
        .substring(0, 4);

    await AlarmNotificationService.instance.showVerificationDeposit(
      authCode: authCode,
    );

    if (!mounted) return;

    setState(() {
      _codeController.text = _generateCode();
      _logs.insert(
        0,
        _AlarmLog(
          title: '1원 입금',
          body: 'AMAGETDONE $authCode 입금',
          sentAt: DateTime.now(),
        ),
      );
    });

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('테스트 입금 알림을 발송했습니다.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('입금 알림')),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
              ),
              child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '시연용 알림 앱',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
                  ),
                  SizedBox(height: 8),
                  Text(
                    '버튼을 누르면 인증용 입금 알림을 즉시 발송합니다.\n'
                    '백엔드 연동 전에는 여기서 직접 테스트하고, 이후에는 푸시나 서버 트리거로 연결하면 됩니다.',
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _codeController,
              keyboardType: TextInputType.number,
              maxLength: 4,
              decoration: InputDecoration(
                labelText: '인증 코드(4자리)',
                hintText: '예: 4827',
                filled: true,
                fillColor: Colors.white,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: FilledButton(
                    onPressed: _sendVerificationAlarm,
                    child: const Text('테스트 입금 알림 발송'),
                  ),
                ),
                const SizedBox(width: 12),
                OutlinedButton(
                  onPressed: () {
                    setState(() {
                      _codeController.text = _generateCode();
                    });
                  },
                  child: const Text('랜덤'),
                ),
              ],
            ),
            const SizedBox(height: 24),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'FCM 상태',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 6),
                  const Text('구독 토픽: verification_alarm_demo'),
                  const SizedBox(height: 4),
                  Text(
                    _fcmToken == null
                        ? '토큰 로딩 중...'
                        : '토큰: ${_fcmToken!.length <= 16 ? _fcmToken! : '...${_fcmToken!.substring(_fcmToken!.length - 16)}'}',
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            const Text(
              '최근 발송 내역',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: _logs.isEmpty
                  ? const Center(
                      child: Text('아직 발송한 알림이 없습니다.'),
                    )
                  : ListView.separated(
                      itemCount: _logs.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final log = _logs[index];
                        return Container(
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(14),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                log.title,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              const SizedBox(height: 6),
                              Text(log.body),
                              const SizedBox(height: 6),
                              Text(
                                _formatTime(log.sentAt),
                                style: const TextStyle(color: Colors.black54),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatTime(DateTime dateTime) {
    final hour = dateTime.hour.toString().padLeft(2, '0');
    final minute = dateTime.minute.toString().padLeft(2, '0');
    final second = dateTime.second.toString().padLeft(2, '0');
    return '$hour:$minute:$second';
  }
}

class _AlarmLog {
  const _AlarmLog({
    required this.title,
    required this.body,
    required this.sentAt,
  });

  final String title;
  final String body;
  final DateTime sentAt;
}
