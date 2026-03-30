import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/menu_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/pages/chatbot/chatbot_sheet.dart';

class MenuPage extends ConsumerStatefulWidget {
  const MenuPage({super.key});

  @override
  ConsumerState<MenuPage> createState() => _MenuPageState();
}

class _MenuPageState extends ConsumerState<MenuPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(menuViewModelProvider.notifier).loadData();
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(menuViewModelProvider);
    final profileImageUrl = state.profileImageUrl?.trim();
    final hasProfileImage =
        profileImageUrl != null && profileImageUrl.isNotEmpty;

    if (state.isLoading) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(
          showBack: false,
          showLogo: true,
          showNotification: true,
        ),
        body: const SizedBox.shrink(),
      );
    }

    if (state.errorMessage != null && !state.hasLoadedSummary) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(
          showBack: false,
          showLogo: true,
          showNotification: true,
        ),
        body: Center(
          child: Text(
            '메뉴 정보를 불러오지 못했습니다.\n${state.errorMessage!}',
            textAlign: TextAlign.center,
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(
        showBack: false,
        showLogo: true,
        showNotification: true,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 8.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Profile card
            MoreCard(
              child: Row(
                children: [
                  _ProfileAvatar(
                    imageUrl: hasProfileImage ? profileImageUrl : null,
                  ),
                  SizedBox(width: 14.w),
                  Expanded(
                    child: GestureDetector(
                      onTap: () async {
                        await context.push(
                          AppRoutes.profileEdit,
                          extra: <String, String>{
                            'nickname': state.userName,
                            'email': state.userEmail,
                            if (hasProfileImage)
                              'profileImageUrl': profileImageUrl,
                          },
                        );
                        ref.read(menuViewModelProvider.notifier).loadData();
                      },
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            state.userName,
                            style: AppTextStyles.bodyLg.copyWith(
                              color: colors.textPrimary,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          SizedBox(height: 2.h),
                          Row(
                            children: [
                              Text(
                                '내 정보 수정하기',
                                style: AppTextStyles.bodySm.copyWith(
                                  color: colors.textTertiary,
                                ),
                              ),
                              SizedBox(width: 2.w),
                              Icon(
                                Icons.chevron_right,
                                size: 16.w,
                                color: colors.textTertiary,
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                  GestureDetector(
                    onTap: () => context.push(AppRoutes.settings),
                    child: Icon(
                      Icons.settings_outlined,
                      size: 24.w,
                      color: colors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 14.h),

            // Stats row
            Row(
              children: [
                _StatBox(
                  label: '진행중 목표',
                  value: '${state.activeGoalCount}개',
                  backgroundColor: colors.bgInsight,
                  borderColor: colors.borderDefault,
                  onTap: () {
                    ref
                        .read(goalViewModelProvider.notifier)
                        .toggleFilter(false);
                    context.go(AppRoutes.goalTab);
                  },
                ),
                SizedBox(width: 8.w),
                _StatBox(
                  label: '달성한 목표',
                  value: '${state.achievedGoalCount}개',
                  backgroundColor: colors.bgAccent,
                  borderColor: colors.borderDefault,
                  onTap: () {
                    ref.read(goalViewModelProvider.notifier).toggleFilter(true);
                    context.go(AppRoutes.goalTab);
                  },
                ),
                SizedBox(width: 8.w),
                _StatBox(
                  label: '누적 저축액',
                  value: CurrencyFormatter.formatKRW(state.totalSaved),
                  backgroundColor: colors.statusSuccess.withValues(alpha: 0.1),
                  borderColor: colors.statusSuccess.withValues(alpha: 0.3),
                ),
              ],
            ),
            SizedBox(height: 20.h),

            Padding(
              padding: EdgeInsets.only(left: 4.w, bottom: 10.h),
              child: Text(
                '전체 메뉴',
                style: AppTextStyles.bodyLg.copyWith(
                  color: colors.textPrimary,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),

            // Menu items
            _MenuListItem(
              icon: Icons.auto_awesome,
              title: 'AI 코치',
              subtitle: '예산·소비·목표에 대해 AI에게 물어보세요',
              onTap: () => showChatbotSheet(context),
              iconColor: AppColors.green500,
            ),
            _MenuListItem(
              icon: Icons.account_balance_wallet_outlined,
              title: '내 자산',
              subtitle: '총 자산·거래내역',
              onTap: () => context.push(AppRoutes.asset),
            ),
            _MenuListItem(
              icon: Icons.credit_card_outlined,
              title: '계좌 관리',
              subtitle: '연결된 계좌 목록·계좌 추가·계좌 삭제',
              onTap: () => context.push(AppRoutes.accountManage),
            ),
            _MenuListItem(
              icon: Icons.pie_chart_outline,
              title: '소비패턴 분석',
              subtitle: '지난 달 소비패턴 분석',
              onTap: () => context.push(AppRoutes.spendingPattern),
            ),
            _MenuListItem(
              icon: Icons.tune_outlined,
              title: '주간 예산 설정',
              subtitle: '주간 예산 설정',
              onTap: () => context.push(AppRoutes.spendingBudget),
            ),
            _MenuListItem(
              icon: Icons.sort,
              title: '목표 우선순위 설정',
              subtitle: '목표 우선순위 설정',
              onTap: () => context.push(AppRoutes.goalPriority),
            ),
            SizedBox(height: 24.h),
          ],
        ),
      ),
    );
  }
}

class _ProfileAvatar extends StatelessWidget {
  const _ProfileAvatar({required this.imageUrl});

  final String? imageUrl;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    Widget fallback() =>
        Icon(Icons.person_outline, size: 28.w, color: colors.textSecondary);

    return Container(
      width: 48.w,
      height: 48.w,
      decoration: BoxDecoration(color: colors.gray100, shape: BoxShape.circle),
      clipBehavior: Clip.antiAlias,
      child: imageUrl == null
          ? Center(child: fallback())
          : Image.network(
              imageUrl!,
              fit: BoxFit.cover,
              webHtmlElementStrategy: kIsWeb
                  ? WebHtmlElementStrategy.prefer
                  : WebHtmlElementStrategy.never,
              errorBuilder: (context, error, stackTrace) {
                return Center(child: fallback());
              },
              loadingBuilder: (context, child, loadingProgress) {
                if (loadingProgress == null) {
                  return child;
                }

                return Center(
                  child: SizedBox(
                    width: 18.w,
                    height: 18.w,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: colors.gray500,
                    ),
                  ),
                );
              },
            ),
    );
  }
}

class _StatBox extends StatelessWidget {
  final String label;
  final String value;
  final Color backgroundColor;
  final Color borderColor;
  final VoidCallback? onTap;

  const _StatBox({
    required this.label,
    required this.value,
    required this.backgroundColor,
    required this.borderColor,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          padding: EdgeInsets.symmetric(vertical: 14.h, horizontal: 10.w),
          decoration: BoxDecoration(
            color: backgroundColor,
            borderRadius: BorderRadius.circular(12.r),
            border: Border.all(color: borderColor),
          ),
          child: Column(
            children: [
              Text(
                label,
                style: AppTextStyles.caption.copyWith(
                  color: colors.textTertiary,
                  fontSize: 12.5.sp,
                ),
              ),
              SizedBox(height: 4.h),
              Text(
                value,
                style: AppTextStyles.bodyMd.copyWith(
                  color: colors.textPrimary,
                  fontWeight: FontWeight.w600,
                  fontSize: 14.sp,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MenuListItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final Color? iconColor;

  const _MenuListItem({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.iconColor,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return GestureDetector(
      onTap: onTap,
      child: Padding(
        padding: EdgeInsets.symmetric(vertical: 8.h),
        child: Row(
          children: [
            Container(
              width: 40.w,
              height: 40.w,
              decoration: BoxDecoration(
                color: colors.gray100,
                borderRadius: BorderRadius.circular(12.r),
              ),
              child: Icon(icon, size: 20.w, color: iconColor ?? colors.white),
            ),
            SizedBox(width: 14.w),
            Expanded(
              child: Text(
                title,
                style: AppTextStyles.bodyMd.copyWith(
                  color: colors.textPrimary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            Text(
              subtitle,
              style: AppTextStyles.caption.copyWith(
                color: colors.textTertiary,
                fontSize: 11.sp,
              ),
            ),
            SizedBox(width: 4.w),
            Icon(Icons.chevron_right, size: 18.w, color: colors.textTertiary),
          ],
        ),
      ),
    );
  }
}
