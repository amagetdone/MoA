import 'package:flutter/material.dart';
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
          height: 64,
          selectedIndex: navigationShell.currentIndex,
          onDestinationSelected: (index) {
            navigationShell.goBranch(
              index,
              initialLocation: index == navigationShell.currentIndex,
            );
          },
          labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.home_outlined),
              selectedIcon: Icon(Icons.home, color: AppColors.orange500),
              label: '홈',
            ),
            NavigationDestination(
              icon: Icon(Icons.flag_outlined),
              selectedIcon: Icon(Icons.flag, color: AppColors.orange500),
              label: '목표',
            ),
            NavigationDestination(
              icon: Icon(Icons.pie_chart_outline),
              selectedIcon: Icon(Icons.pie_chart, color: AppColors.orange500),
              label: '소비',
            ),
            NavigationDestination(
              icon: Icon(Icons.menu),
              selectedIcon: Icon(Icons.menu, color: AppColors.orange500),
              label: '전체',
            ),
          ],
        ),
      ),
    );
  }
}
