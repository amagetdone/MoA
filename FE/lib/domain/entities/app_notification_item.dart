class AppNotificationItem {
  const AppNotificationItem({
    required this.id,
    required this.title,
    required this.body,
    required this.createdAt,
    this.type = 'general',
    this.route,
    this.payload = const {},
    this.isRead = false,
  });

  final String id;
  final String title;
  final String body;
  final DateTime createdAt;
  final String type;
  final String? route;
  final Map<String, String> payload;
  final bool isRead;

  AppNotificationItem copyWith({
    String? id,
    String? title,
    String? body,
    DateTime? createdAt,
    String? type,
    String? route,
    Map<String, String>? payload,
    bool? isRead,
  }) {
    return AppNotificationItem(
      id: id ?? this.id,
      title: title ?? this.title,
      body: body ?? this.body,
      createdAt: createdAt ?? this.createdAt,
      type: type ?? this.type,
      route: route ?? this.route,
      payload: payload ?? this.payload,
      isRead: isRead ?? this.isRead,
    );
  }

  factory AppNotificationItem.fromJson(Map<String, dynamic> json) {
    return AppNotificationItem(
      id: json['id'] as String,
      title: json['title'] as String? ?? '',
      body: json['body'] as String? ?? '',
      createdAt: DateTime.parse(json['createdAt'] as String),
      type: json['type'] as String? ?? 'general',
      route: json['route'] as String?,
      payload: (json['payload'] as Map<String, dynamic>? ?? const {})
          .map((key, value) => MapEntry(key, value?.toString() ?? '')),
      isRead: json['isRead'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'body': body,
      'createdAt': createdAt.toIso8601String(),
      'type': type,
      'route': route,
      'payload': payload,
      'isRead': isRead,
    };
  }
}
