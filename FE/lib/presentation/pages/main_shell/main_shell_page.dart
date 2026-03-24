import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/menu_viewmodel.dart';

class MainShellPage extends ConsumerWidget {
  final StatefulNavigationShell navigationShell;

  const MainShellPage({super.key, required this.navigationShell});

  void _onTabSelected(WidgetRef ref, int index) {
    // 탭 전환 시 해당 탭 데이터 갱신
    switch (index) {
      case 0:
        ref.read(homeViewModelProvider.notifier).loadData();
        break;
      case 1:
        ref.read(spendingViewModelProvider.notifier).loadData();
        break;
      case 2:
        ref.read(goalViewModelProvider.notifier).loadGoals();
        break;
      case 3:
        ref.read(menuViewModelProvider.notifier).loadData();
        break;
    }
    navigationShell.goBranch(
      index,
      initialLocation: true,
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final bottomInset = MediaQuery.of(context).padding.bottom;

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      body: navigationShell,
      bottomNavigationBar: Container(
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          color: colors.bgSurface,
          borderRadius: BorderRadius.only(
            topLeft: Radius.circular(16.r),
            topRight: Radius.circular(16.r),
          ),
          border: Border(
            top: BorderSide(color: colors.borderDefault, width: 0.5),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(height: 6.h),
            NavigationBar(
              backgroundColor: colors.bgSurface,
              indicatorColor: AppColors.orange50,
              elevation: 0,
              height: 58.h,
              selectedIndex: navigationShell.currentIndex,
              onDestinationSelected: (index) => _onTabSelected(ref, index),
              labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
              destinations: [
                NavigationDestination(
                  icon: Icon(Icons.home_outlined, size: 24.r),
                  selectedIcon: Icon(Icons.home, color: AppColors.orange500, size: 24.r),
                  label: '홈',
                ),
                NavigationDestination(
                  icon: Icon(Icons.bar_chart_outlined, size: 24.r),
                  selectedIcon: Icon(Icons.bar_chart, color: AppColors.orange500, size: 24.r),
                  label: '분석',
                ),
                NavigationDestination(
                  icon: Icon(Icons.flag_outlined, size: 24.r),
                  selectedIcon: Icon(Icons.flag, color: AppColors.orange500, size: 24.r),
                  label: '목표',
                ),
                NavigationDestination(
                  icon: Icon(Icons.menu_outlined, size: 24.r),
                  selectedIcon: Icon(Icons.menu, color: AppColors.orange500, size: 24.r),
                  label: '메뉴',
                ),
              ],
            ),
            SizedBox(height: bottomInset > 0 ? 0 : 8.h),
          ],
        ),
      ),
    );
  }
}
