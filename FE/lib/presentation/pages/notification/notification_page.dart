import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/notifications/notification_inbox_persistence.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/app_notification_item.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class NotificationPage extends ConsumerWidget {
  const NotificationPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final notificationsAsync = ref.watch(notificationInboxProvider);

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: MoreAppBar(
        title: '알림',
        actions: [
          Padding(
            padding: EdgeInsets.only(right: 12.w),
            child: TextButton(
              onPressed: () =>
                  ref.read(notificationInboxRepositoryProvider).markAllAsRead(),
              child: Text(
                '모두 읽음',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.orange500,
                ),
              ),
            ),
          ),
        ],
      ),
      body: notificationsAsync.when(
        data: (items) {
          if (items.isEmpty) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.notifications_none_outlined,
                    size: 44.w,
                    color: colors.textTertiary,
                  ),
                  SizedBox(height: 12.h),
                  Text(
                    '받은 알림이 없습니다',
                    style: AppTextStyles.bodyMd.copyWith(
                      color: colors.textSecondary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            );
          }

          return ListView.separated(
            padding: EdgeInsets.fromLTRB(20.w, 12.h, 20.w, 20.h),
            itemCount: items.length,
            separatorBuilder: (context, index) => SizedBox(height: 10.h),
            itemBuilder: (context, index) {
              final item = items[index];
              return _NotificationCard(
                item: item,
                onTap: () async {
                  await ref
                      .read(notificationInboxRepositoryProvider)
                      .markAsRead(item.id);

                  if (!context.mounted) {
                    return;
                  }

                  final route =
                      NotificationInboxPersistence.resolveRouteForItem(item);
                  if (route != null && route.isNotEmpty) {
                    context.push(route);
                  }
                },
                onDelete: () => ref
                    .read(notificationInboxRepositoryProvider)
                    .deleteNotification(item.id),
              );
            },
          );
        },
        loading: () => const SizedBox.shrink(),
        error: (error, _) => Center(
          child: Text(
            '알림을 불러오지 못했습니다.\n$error',
            textAlign: TextAlign.center,
            style: AppTextStyles.bodyMd.copyWith(color: colors.textSecondary),
          ),
        ),
      ),
    );
  }
}

class _NotificationCard extends StatelessWidget {
  const _NotificationCard({
    required this.item,
    required this.onTap,
    required this.onDelete,
  });

  final AppNotificationItem item;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final timeLabel = DateFormat('M월 d일 HH:mm', 'ko_KR').format(item.createdAt);

    return MoreCard(
      onTap: onTap,
      padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 16.h),
      color: item.isRead
          ? colors.bgSurface
          : AppColors.orange500.withValues(alpha: 0.05),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 10.w,
            height: 10.w,
            margin: EdgeInsets.only(top: 5.h),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: item.isRead ? colors.gray100 : AppColors.orange500,
            ),
          ),
          SizedBox(width: 12.w),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.title,
                  style: AppTextStyles.bodyMd.copyWith(
                    color: colors.textPrimary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                if (item.body.isNotEmpty) ...[
                  SizedBox(height: 6.h),
                  Text(
                    item.body,
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textSecondary,
                      height: 1.45,
                    ),
                  ),
                ],
                SizedBox(height: 10.h),
                Text(
                  timeLabel,
                  style: AppTextStyles.caption.copyWith(
                    color: colors.textTertiary,
                    fontSize: 11.sp,
                  ),
                ),
              ],
            ),
          ),
          SizedBox(width: 8.w),
          IconButton(
            onPressed: onDelete,
            splashRadius: 18.w,
            padding: EdgeInsets.zero,
            constraints: BoxConstraints.tight(Size(28.w, 28.w)),
            icon: Icon(
              Icons.close_rounded,
              size: 18.w,
              color: colors.textTertiary,
            ),
          ),
        ],
      ),
    );
  }
}
