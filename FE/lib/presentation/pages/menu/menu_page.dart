import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
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
        appBar: MoreAppBar(title: '내 정보', showBack: false),
        body: const SizedBox.shrink(),
      );
    }

    if (state.errorMessage != null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: const MoreAppBar(title: '내 정보', showBack: false),
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
      appBar: const MoreAppBar(title: '내 정보', showBack: false),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Profile card
            MoreCard(
              onTap: () => context.push(AppRoutes.profileEdit),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 24.r,
                    backgroundColor: AppColors.orange500,
                    child: Text(
                      state.userName.isNotEmpty
                          ? state.userName.substring(0, 1)
                          : '?',
                      style: AppTextStyles.headingMd.copyWith(
                        color: AppColors.white,
                        fontSize: 18.sp,
                      ),
                    ),
                  ),
                  SizedBox(width: 14.w),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          state.userName,
                          style: AppTextStyles.headingMd.copyWith(
                            color: AppColors.textPrimary,
                            fontSize: 16.sp,
                          ),
                        ),
                        SizedBox(height: 2.h),
                        Text(
                          '내 정보 수정하기',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textTertiary,
                            fontSize: 12.sp,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Icon(
                    Icons.chevron_right,
                    size: 20.w,
                    color: AppColors.textTertiary,
                  ),
                  SizedBox(width: 4.w),
                  GestureDetector(
                    onTap: () => context.push(AppRoutes.settings),
                    child: Icon(
                      Icons.settings,
                      size: 22.w,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 16.h),

            // Stats row
            Row(
              children: [
                _StatBox(label: '진행중 목표', value: '${state.activeGoalCount}개'),
                SizedBox(width: 8.w),
                _StatBox(label: '달성한 목표', value: '${state.achievedGoalCount}개'),
                SizedBox(width: 8.w),
                _StatBox(
                  label: '누적 저축액',
                  value: CurrencyFormatter.formatKRW(state.totalSaved),
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
                  fontSize: 16.sp,
                ),
              ),
            ),

            // Menu list
            MoreCard(
              padding: EdgeInsets.zero,
              child: Column(
                children: [
                  _MenuListTile(
                    icon: Icons.account_balance_wallet,
                    title: '내 자산',
                    subtitle: '자산 현황을 확인해보세요',
                    onTap: () => context.push(AppRoutes.asset),
                  ),
                  Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.credit_card,
                    title: '계좌 관리',
                    subtitle: '연결된 계좌를 관리합니다',
                    onTap: () => context.push(AppRoutes.accountManage),
                  ),
                  Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.pie_chart,
                    title: '소비패턴 분석',
                    subtitle: '소비 습관을 분석해보세요',
                    onTap: () => context.go(AppRoutes.spendingTab),
                  ),
                  Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.tune,
                    title: '주간 예산',
                    subtitle: '주간 예산을 설정합니다',
                    onTap: () => context.push(AppRoutes.spendingBudget),
                  ),
                  Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.sort,
                    title: '목표 우선순위',
                    subtitle: '목표의 우선순위를 조정합니다',
                    onTap: () => context.push(AppRoutes.goalPriority),
                  ),
                ],
              ),
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

  const _StatBox({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: EdgeInsets.symmetric(vertical: 14.h, horizontal: 10.w),
        decoration: BoxDecoration(
          color: AppColors.bgSurface,
          borderRadius: BorderRadius.circular(12.r),
          border: Border.all(color: AppColors.borderDefault),
        ),
        child: Column(
          children: [
            Text(
              label,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
                fontSize: 11.sp,
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
    );
  }
}

class _MenuListTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _MenuListTile({
    required this.icon,
    required this.title,
    required this.onTap,
    this.subtitle = '',
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: AppColors.textSecondary, size: 22.w),
      title: Text(
        title,
        style: AppTextStyles.bodyMd.copyWith(
          color: AppColors.textPrimary,
          fontSize: 14.sp,
        ),
      ),
      subtitle: subtitle.isNotEmpty
          ? Text(
              subtitle,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
                fontSize: 11.sp,
              ),
            )
          : null,
      trailing: Icon(
        Icons.chevron_right,
        color: AppColors.textTertiary,
        size: 20.w,
      ),
      onTap: onTap,
      contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 2.h),
    );
  }
}
