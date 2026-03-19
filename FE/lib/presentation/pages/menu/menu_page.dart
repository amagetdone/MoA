import 'package:flutter/material.dart';
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

class MenuPage extends ConsumerWidget {
  const MenuPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(menuViewModelProvider);

    if (state.isLoading) {
      return const Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: MoreAppBar(showBack: false, showLogo: true, showNotification: true),
        body: SizedBox.shrink(),
      );
    }

    if (state.errorMessage != null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: const MoreAppBar(showBack: false, showLogo: true, showNotification: true),
        body: Center(
          child: Text(
            '메뉴 정보를 불러오지 못했습니다.\n${state.errorMessage!}',
            textAlign: TextAlign.center,
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(showBack: false, showLogo: true, showNotification: true),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 12.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Profile card
            MoreCard(
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 24.r,
                    backgroundColor: AppColors.gray100,
                    backgroundImage: state.profileImageUrl != null
                        ? NetworkImage(state.profileImageUrl!)
                        : null,
                    child: state.profileImageUrl == null
                        ? Icon(
                            Icons.person_outline,
                            size: 28.w,
                            color: AppColors.gray500,
                          )
                        : null,
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
                            if (state.profileImageUrl != null)
                              'profileImageUrl': state.profileImageUrl!,
                          },
                        );
                        ref.read(menuViewModelProvider.notifier).loadData();
                      },
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            state.userName,
                            style: AppTextStyles.headingMd.copyWith(
                              color: AppColors.textPrimary,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          SizedBox(height: 2.h),
                          Row(
                            children: [
                              Text(
                                '내 정보 수정하기',
                                style: AppTextStyles.bodySm.copyWith(
                                  color: AppColors.textTertiary,
                                ),
                              ),
                              SizedBox(width: 2.w),
                              Icon(
                                Icons.chevron_right,
                                size: 16.w,
                                color: AppColors.textTertiary,
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
                      color: AppColors.textSecondary,
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
                  backgroundColor: const Color(0xFFFFF8E1),
                  borderColor: const Color(0xFFFFECB3),
                  onTap: () {
                    ref.read(goalViewModelProvider.notifier).toggleFilter(false);
                    context.go(AppRoutes.goalTab);
                  },
                ),
                SizedBox(width: 8.w),
                _StatBox(
                  label: '달성한 목표',
                  value: '${state.achievedGoalCount}개',
                  backgroundColor: const Color(0xFFFFF0E0),
                  borderColor: const Color(0xFFFFD9B3),
                  onTap: () {
                    ref.read(goalViewModelProvider.notifier).toggleFilter(true);
                    context.go(AppRoutes.goalTab);
                  },
                ),
                SizedBox(width: 8.w),
                _StatBox(
                  label: '누적 저축액',
                  value: CurrencyFormatter.formatKRW(state.totalSaved),
                  backgroundColor: const Color(0xFFE8F5E9),
                  borderColor: const Color(0xFFC8E6C9),
                ),
              ],
            ),
            SizedBox(height: 24.h),

            // Menu section header
            Padding(
              padding: EdgeInsets.only(left: 4.w, bottom: 12.h),
              child: Text(
                '전체 메뉴',
                style: AppTextStyles.headingMd.copyWith(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),

            // Menu items
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
              subtitle: '지난 3개월 간 소비패턴 분석',
              onTap: () => context.go(AppRoutes.spendingTab),
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
                color: AppColors.textTertiary,
                fontSize: 12.5.sp,
              ),
            ),
            SizedBox(height: 4.h),
            Text(
              value,
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textPrimary,
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

  const _MenuListItem({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
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
                color: AppColors.gray100,
                borderRadius: BorderRadius.circular(12.r),
              ),
              child: Icon(icon, size: 20.w, color: Colors.white),
            ),
            SizedBox(width: 14.w),
            Expanded(
              child: Text(
                title,
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            Text(
              subtitle,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
                fontSize: 11.sp,
              ),
            ),
            SizedBox(width: 4.w),
            Icon(
              Icons.chevron_right,
              size: 18.w,
              color: AppColors.textTertiary,
            ),
          ],
        ),
      ),
    );
  }
}
