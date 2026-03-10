import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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
        appBar: MoreAppBar(title: '전체', showBack: false),
        body: Center(child: CircularProgressIndicator()),
      );
    }

    if (state.errorMessage != null) {
      return Scaffold(
        backgroundColor: AppColors.bgPrimary,
        appBar: const MoreAppBar(title: '전체', showBack: false),
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
      appBar: const MoreAppBar(title: '전체', showBack: false),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Profile card
            MoreCard(
              onTap: () => context.push(AppRoutes.profileEdit),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 24,
                    backgroundColor: AppColors.orange500,
                    child: Text(
                      state.userName.isNotEmpty
                          ? state.userName.substring(0, 1)
                          : '?',
                      style: AppTextStyles.headingMd.copyWith(
                        color: AppColors.white,
                      ),
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          state.userName,
                          style: AppTextStyles.headingMd.copyWith(
                            color: AppColors.textPrimary,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          state.userEmail,
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textTertiary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Text(
                    '내 정보 수정',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.textTertiary,
                    ),
                  ),
                  const SizedBox(width: 4),
                  const Icon(
                    Icons.chevron_right,
                    size: 20,
                    color: AppColors.textTertiary,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Stats row
            Row(
              children: [
                _StatBox(label: '진행중 목표', value: '${state.activeGoalCount}'),
                const SizedBox(width: 8),
                _StatBox(label: '달성한 목표', value: '${state.achievedGoalCount}'),
                const SizedBox(width: 8),
                _StatBox(
                  label: '누적 저축',
                  value: CurrencyFormatter.formatKRW(state.totalSaved),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Menu list
            MoreCard(
              padding: EdgeInsets.zero,
              child: Column(
                children: [
                  _MenuListTile(
                    icon: Icons.account_balance_wallet,
                    title: '내 자산',
                    onTap: () => context.push(AppRoutes.asset),
                  ),
                  const Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.credit_card,
                    title: '계좌 관리',
                    onTap: () => context.push(AppRoutes.accountManage),
                  ),
                  const Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.pie_chart,
                    title: '소비 패턴 분석',
                    onTap: () => context.push(AppRoutes.spendingTab),
                  ),
                  const Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.tune,
                    title: '주간 예산 설정',
                    onTap: () => context.push(AppRoutes.spendingBudget),
                  ),
                  const Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.sort,
                    title: '목표 우선순위',
                    onTap: () => context.push(AppRoutes.goalPriority),
                  ),
                  const Divider(height: 1, color: AppColors.borderDefault),
                  _MenuListTile(
                    icon: Icons.settings,
                    title: '설정',
                    onTap: () => context.push(AppRoutes.settings),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
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
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 10),
        decoration: BoxDecoration(
          color: AppColors.bgSurface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.borderDefault),
        ),
        child: Column(
          children: [
            Text(
              label,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w600,
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
  final VoidCallback onTap;

  const _MenuListTile({
    required this.icon,
    required this.title,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: AppColors.textSecondary, size: 22),
      title: Text(
        title,
        style: AppTextStyles.bodyMd.copyWith(color: AppColors.textPrimary),
      ),
      trailing: const Icon(
        Icons.chevron_right,
        color: AppColors.textTertiary,
        size: 20,
      ),
      onTap: onTap,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 2),
    );
  }
}
