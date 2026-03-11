import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';


class MainShellPage extends StatelessWidget {
  final StatefulNavigationShell navigationShell;

  const MainShellPage({super.key, required this.navigationShell});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          border: Border(top: BorderSide(color: AppColors.borderDefault, width: 0.5)),
        ),
        child: NavigationBar(
          backgroundColor: AppColors.bgSurface,
          indicatorColor: AppColors.orange50,
          elevation: 0,
          height: 56.h,
          selectedIndex: navigationShell.currentIndex,
          onDestinationSelected: (index) {
            navigationShell.goBranch(
              index,
              initialLocation: index == navigationShell.currentIndex,
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
              icon: Icon(Icons.person_outline, size: 24.r),
              selectedIcon: Icon(Icons.person, color: AppColors.orange500, size: 24.r),
              label: '내 정보',
            ),
          ],
        ),
      ),
    );
  }
}
