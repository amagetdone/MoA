import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/presentation/pages/chatbot/chatbot_page.dart';

Future<T?> showChatbotSheet<T>(BuildContext context) {
  return showModalBottomSheet<T>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.transparent,
    builder: (context) {
      return FractionallySizedBox(
        heightFactor: 0.9,
        child: ClipRRect(
          borderRadius: BorderRadius.vertical(top: Radius.circular(28.r)),
          child: const ChatbotPage(embedded: true),
        ),
      );
    },
  );
}
