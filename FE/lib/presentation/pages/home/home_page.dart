import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/pages/home/widgets/asset_card.dart';
import 'package:more_app/presentation/pages/home/widgets/goal_summary_card.dart';
import 'package:more_app/presentation/pages/home/widgets/savings_card.dart';
import 'package:more_app/presentation/pages/home/widgets/spending_card.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_empty_state.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  late final PageController _sectionController;
  int _sectionIndex = 0;

  @override
  void initState() {
    super.initState();
    _sectionController = PageController();
  }

  @override
  void dispose() {
    _sectionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(homeViewModelProvider);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(showBack: false, showNotification: true),
      body: _buildBody(context, state),
    );
  }

  Widget _buildBody(BuildContext context, HomeState state) {
    if (state.isLoading) {
      return const SizedBox.shrink();
    }

    if (state.errorMessage != null) {
      return Center(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('홈 데이터를 불러오지 못했습니다.', style: AppTextStyles.bodyLg),
              SizedBox(height: 8.h),
              Text(
                state.errorMessage!,
                textAlign: TextAlign.center,
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
              SizedBox(height: 16.h),
              FilledButton(
                onPressed: () {
                  ref.read(homeViewModelProvider.notifier).loadData();
                },
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      );
    }

    switch (state.status) {
      case HomeStatus.noAccount:
        return _buildNoAccount(context);
      case HomeStatus.noGoal:
        return _buildDashboard(context, state, showGoalEmptyState: true);
      case HomeStatus.ready:
        return _buildDashboard(context, state, showGoalEmptyState: false);
    }
  }

  Widget _buildNoAccount(BuildContext context) {
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: 24.w),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            Text(
              '계좌를 연결해주세요.',
              style: AppTextStyles.headingLg.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
            SizedBox(height: 12.h),
            Text(
              '계좌를 연결하면 소비 분석을 통해\n똑똑한 저축 계획을 세울게요!',
              textAlign: TextAlign.center,
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
            SizedBox(height: 32.h),
            GestureDetector(
              onTap: () => context.push(AppRoutes.onboardingAccount),
              child: MoreCard(
                child: Column(
                  children: [
                    Container(
                      width: 48.w,
                      height: 48.w,
                      decoration: BoxDecoration(
                        color: AppColors.orange500,
                        borderRadius: BorderRadius.circular(12.r),
                      ),
                      child: Icon(Icons.add, color: Colors.white, size: 28.w),
                    ),
                    SizedBox(height: 12.h),
                    Text(
                      '새로운 계좌 연결하기',
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    SizedBox(height: 4.h),
                    Text(
                      '계좌를 추가하고 자산을 관리하세요!',
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDashboard(
    BuildContext context,
    HomeState state, {
    required bool showGoalEmptyState,
  }) {
    final orderedAccounts = _orderedAccounts(state.accounts);
    final featuredGoal = _selectFeaturedGoal(state.activeGoals);

    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(24.w, 12.h, 24.w, 28.h),
      child: Column(
        children: [
          SpendingCard(
            weeklyBudget: state.weeklyBudget,
            weeklySpent: state.weeklySpent,
          ),
          SizedBox(height: 18.h),
          _SectionCarousel(
            controller: _sectionController,
            currentIndex: _sectionIndex,
            onPageChanged: (index) {
              setState(() => _sectionIndex = index);
            },
            firstPage: Column(
              children: [
                SavingsCard(weeklySaved: state.weeklySaved),
                SizedBox(height: 16.h),
                AssetCard(
                  totalAsset: state.totalAsset,
                  accounts: orderedAccounts,
                ),
              ],
            ),
            secondPage: showGoalEmptyState
                ? _buildNoGoal(context)
                : GoalSummaryCard(
                    goal: featuredGoal!,
                    onViewAll: () => context.go(AppRoutes.goalTab),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildNoGoal(BuildContext context) {
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: 24.w),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            Text(
              '첫 번째 저축 목표를 세워보세요.',
              style: AppTextStyles.headingLg.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
            SizedBox(height: 12.h),
            Text(
              '목표에 도달할 수 있게 제가 도와드릴게요!',
              textAlign: TextAlign.center,
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
            SizedBox(height: 32.h),
            GestureDetector(
              onTap: () => context.push(AppRoutes.goalCreate),
              child: MoreCard(
                child: Column(
                  children: [
                    Container(
                      width: 48.w,
                      height: 48.w,
                      decoration: BoxDecoration(
                        color: AppColors.orange500,
                        borderRadius: BorderRadius.circular(12.r),
                      ),
                      child: Icon(Icons.add, color: Colors.white, size: 28.w),
                    ),
                    SizedBox(height: 12.h),
                    Text(
                      '첫 목표 세우러 가기',
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    SizedBox(height: 4.h),
                    Text(
                      '목표를 세우고, 돈을 모아봐요!',
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<Account> _orderedAccounts(List<Account> accounts) {
    final items = List<Account>.from(accounts);
    items.sort((a, b) {
      if (a.isPrimary == b.isPrimary) {
        return a.id.compareTo(b.id);
      }
      return a.isPrimary ? -1 : 1;
    });
    return items;
  }

  Goal? _selectFeaturedGoal(List<Goal> goals) {
    if (goals.isEmpty) {
      return null;
    }
    return goals.first;
  }
}

class _SectionCarousel extends StatefulWidget {
  const _SectionCarousel({
    required this.controller,
    required this.currentIndex,
    required this.onPageChanged,
    required this.firstPage,
    required this.secondPage,
  });

  final PageController controller;
  final int currentIndex;
  final ValueChanged<int> onPageChanged;
  final Widget firstPage;
  final Widget secondPage;

  @override
  State<_SectionCarousel> createState() => _SectionCarouselState();
}

class _SectionCarouselState extends State<_SectionCarousel> {
  final List<double> _pageHeights = [0, 0];

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          height: _currentHeight,
          child: PageView(
            controller: widget.controller,
            onPageChanged: widget.onPageChanged,
            children: [
              _MeasuredPage(
                onSizeChanged: (height) => _updateHeight(0, height),
                child: Padding(
                  padding: EdgeInsets.symmetric(horizontal: 2.w),
                  child: widget.firstPage,
                ),
              ),
              _MeasuredPage(
                onSizeChanged: (height) => _updateHeight(1, height),
                child: Padding(
                  padding: EdgeInsets.symmetric(horizontal: 2.w),
                  child: widget.secondPage,
                ),
              ),
            ],
          ),
        ),
        SizedBox(height: 14.h),
        _SectionIndicator(
          currentIndex: widget.currentIndex,
          onSelected: (index) {
            widget.controller.animateToPage(
              index,
              duration: const Duration(milliseconds: 240),
              curve: Curves.easeOutCubic,
            );
          },
        ),
      ],
    );
  }

  double get _currentHeight {
    final currentHeight = _pageHeights[widget.currentIndex];
    if (currentHeight > 0) {
      return currentHeight;
    }

    final measuredHeights = _pageHeights.where((height) => height > 0);
    if (measuredHeights.isNotEmpty) {
      return measuredHeights.reduce(
        (value, element) => value > element ? value : element,
      );
    }

    return 420.h;
  }

  void _updateHeight(int index, double height) {
    if (!mounted || height <= 0) {
      return;
    }

    if ((_pageHeights[index] - height).abs() < 1) {
      return;
    }

    setState(() {
      _pageHeights[index] = height;
    });
  }
}

class _MeasuredPage extends StatefulWidget {
  const _MeasuredPage({required this.child, required this.onSizeChanged});

  final Widget child;
  final ValueChanged<double> onSizeChanged;

  @override
  State<_MeasuredPage> createState() => _MeasuredPageState();
}

class _MeasuredPageState extends State<_MeasuredPage> {
  @override
  Widget build(BuildContext context) {
    return OverflowBox(
      minHeight: 0,
      maxHeight: double.infinity,
      alignment: Alignment.topCenter,
      child: _ChildSizeReporter(
        onSizeChanged: widget.onSizeChanged,
        child: widget.child,
      ),
    );
  }
}

class _ChildSizeReporter extends StatefulWidget {
  const _ChildSizeReporter({required this.child, required this.onSizeChanged});

  final Widget child;
  final ValueChanged<double> onSizeChanged;

  @override
  State<_ChildSizeReporter> createState() => _ChildSizeReporterState();
}

class _ChildSizeReporterState extends State<_ChildSizeReporter> {
  Size? _oldSize;

  @override
  Widget build(BuildContext context) {
    WidgetsBinding.instance.addPostFrameCallback((_) => _notifySize());
    return widget.child;
  }

  void _notifySize() {
    if (!mounted) {
      return;
    }

    final contextSize = context.size;
    if (contextSize == null || contextSize == _oldSize) {
      return;
    }

    _oldSize = contextSize;
    widget.onSizeChanged(contextSize.height);
  }
}

class _SectionIndicator extends StatelessWidget {
  const _SectionIndicator({
    required this.currentIndex,
    required this.onSelected,
  });

  final int currentIndex;
  final ValueChanged<int> onSelected;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      alignment: WrapAlignment.center,
      spacing: 10.w,
      runSpacing: 8.h,
      children: [
        _SectionChip(
          label: '저축 · 자산',
          selected: currentIndex == 0,
          onTap: () => onSelected(0),
        ),
        _SectionChip(
          label: '목표',
          selected: currentIndex == 1,
          onTap: () => onSelected(1),
        ),
      ],
    );
  }
}

class _SectionChip extends StatelessWidget {
  const _SectionChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999.r),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFF0E1CF) : Colors.white,
          borderRadius: BorderRadius.circular(999.r),
          border: Border.all(
            color: selected ? const Color(0xFFE0B37E) : AppColors.borderDefault,
          ),
        ),
        child: Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: selected ? AppColors.orange500 : AppColors.textTertiary,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
          ),
        ),
      ),
    );
  }
}
