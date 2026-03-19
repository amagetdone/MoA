import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';


class MainShellPage extends StatelessWidget {
  final StatefulNavigationShell navigationShell;

  const MainShellPage({super.key, required this.navigationShell});

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).padding.bottom;

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: navigationShell,
      bottomNavigationBar: Container(
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          color: AppColors.bgSurface,
          borderRadius: BorderRadius.only(
            topLeft: Radius.circular(16.r),
            topRight: Radius.circular(16.r),
          ),
          border: const Border(
            top: BorderSide(color: AppColors.borderDefault, width: 0.5),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(height: 6.h),
            NavigationBar(
              backgroundColor: AppColors.bgSurface,
              indicatorColor: AppColors.orange50,
              elevation: 0,
              height: 58.h,
              selectedIndex: navigationShell.currentIndex,
              onDestinationSelected: (index) {
                navigationShell.goBranch(
                  index,
                  initialLocation: true,
                );
              },
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
