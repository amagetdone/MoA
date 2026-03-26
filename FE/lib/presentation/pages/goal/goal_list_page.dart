import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/tutorial_service.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/pages/goal/widgets/goal_card.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:showcaseview/showcaseview.dart';

class GoalListPage extends ConsumerStatefulWidget {
  const GoalListPage({super.key});

  @override
  ConsumerState<GoalListPage> createState() => _GoalListPageState();
}

class _GoalListPageState extends ConsumerState<GoalListPage> {
  BuildContext? _showcaseContext;
  final _keyFilterChips = GlobalKey();
  final _keyPriority = GlobalKey();
  final _keyFab = GlobalKey();

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(goalViewModelProvider.notifier).loadGoals();
    });
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final shown = await TutorialService.isShown(TutorialService.goalList);
      if (!shown && mounted) {
        await TutorialService.markShown(TutorialService.goalList);
        if (mounted && _showcaseContext != null) {
          ShowCaseWidget.of(_showcaseContext!).startShowCase([
            _keyFilterChips,
            _keyPriority,
            _keyFab,
          ]);
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final goalState = ref.watch(goalViewModelProvider);
    final viewModel = ref.read(goalViewModelProvider.notifier);
    final filteredGoals = goalState.filteredGoals;
    final totalCount = goalState.goals.length;

    return ShowCaseWidget(
      builder: (ctx) {
        _showcaseContext = ctx;
        return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(
        showBack: false,
        showLogo: true,
        showNotification: false,
      ),
      body: goalState.isLoading
          ? const SizedBox.shrink()
          : goalState.errorMessage != null
              ? _buildError(context, goalState.errorMessage!, viewModel)
              : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // ── 타이틀 + 서브타이틀 ─────────────────────────
                      Padding(
                        padding: EdgeInsets.fromLTRB(20.w, 0, 20.w, 0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '나의 목표',
                              style: AppTextStyles.headingMd.copyWith(
                                color: colors.textPrimary,
                                fontSize: 20.sp,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            SizedBox(height: 4.h),
                            Text(
                              '총 $totalCount개의 목표를 계획했어요.',
                              style: AppTextStyles.caption.copyWith(
                                color: colors.textTertiary,
                                fontSize: 12.sp,
                              ),
                            ),
                          ],
                        ),
                      ),

                      SizedBox(height: 16.h),

                      // ── 필터 칩 + 우선순위 설정 ──────────────────────
                      Padding(
                        padding: EdgeInsets.fromLTRB(20.w, 0, 20.w, 0),
                        child: Row(
                          children: [
                            Showcase(
                              key: _keyFilterChips,
                              title: '목표 필터',
                              description: '진행 중인 목표와 달성한 목표를 구분해서 볼 수 있어요. 원하는 탭을 눌러 목표를 관리해보세요.',
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  _FilterChip(
                                    label: '진행중인 목표',
                                    selected: !goalState.showAchieved,
                                    onTap: () => viewModel.toggleFilter(false),
                                  ),
                                  SizedBox(width: 8.w),
                                  _FilterChip(
                                    label: '달성한 목표',
                                    selected: goalState.showAchieved,
                                    onTap: () => viewModel.toggleFilter(true),
                                  ),
                                ],
                              ),
                            ),
                            const Spacer(),
                            Showcase(
                              key: _keyPriority,
                              title: '우선순위 설정',
                              description: '목표의 저축 우선순위를 직접 정할 수 있어요. 중요한 목표부터 저축되도록 순서를 설정해보세요.',
                              child: GestureDetector(
                                onTap: () => context.push(AppRoutes.goalPriority),
                                child: Text(
                                  '목표 우선순위 설정 >',
                                  style: AppTextStyles.bodySm.copyWith(
                                    color: colors.textTertiary,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),

                      SizedBox(height: 12.h),

                      // ── 목표 리스트 ────────────────────────────────
                      Expanded(
                        child: filteredGoals.isEmpty
                            ? _buildEmpty(goalState.showAchieved)
                            : ListView.separated(
                                padding: EdgeInsets.fromLTRB(
                                  20.w,
                                  4.h,
                                  20.w,
                                  24.h,
                                ),
                                itemCount: filteredGoals.length,
                                separatorBuilder: (context, index) =>
                                    SizedBox(height: 12.h),
                                itemBuilder: (context, index) =>
                                    GoalCard(goal: filteredGoals[index]),
                              ),
                      ),
                    ],
                  ),
      floatingActionButton: Showcase(
        key: _keyFab,
        title: '목표 추가',
        description: '새로운 목표를 추가할 수 있어요. 여행, 가전제품 등 원하는 것을 목표로 등록하고 저축을 시작해보세요!',
        child: FloatingActionButton(
          backgroundColor: AppColors.orange500,
          foregroundColor: Colors.white,
          onPressed: () => context.push(AppRoutes.goalCreate),
          child: const Icon(Icons.add),
        ),
      ),
      );
    },
    );
  }

  Widget _buildError(
    BuildContext context,
    String message,
    GoalViewModel viewModel,
  ) {
    final colors = AppColors.of(context);
    return Center(
      child: Padding(
        padding: EdgeInsets.all(24.w),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('목표 목록을 불러오지 못했습니다.', style: AppTextStyles.bodyLg.copyWith(color: colors.textPrimary)),
            SizedBox(height: 8.h),
            Text(
              message,
              textAlign: TextAlign.center,
              style: AppTextStyles.bodySm.copyWith(
                color: colors.textTertiary,
              ),
            ),
            SizedBox(height: 16.h),
            FilledButton(
              onPressed: viewModel.loadGoals,
              child: const Text('다시 시도'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmpty(bool showAchieved) {
    final colors = AppColors.of(context);
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.flag_outlined,
            size: 64.w,
            color: colors.textTertiary.withValues(alpha: 0.5),
          ),
          SizedBox(height: 16.h),
          Text(
            showAchieved ? '아직 달성한 목표가 없어요' : '진행 중인 목표가 없어요',
            style: AppTextStyles.bodyLg.copyWith(color: colors.textTertiary),
          ),
          SizedBox(height: 8.h),
          Text(
            '새로운 목표를 추가해보세요!',
            style: AppTextStyles.bodySm.copyWith(color: colors.textTertiary),
          ),
        ],
      ),
    );
  }
}

// ── 필터 칩 ────────────────────────────────────────────────
class _FilterChip extends StatelessWidget {
  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
        decoration: BoxDecoration(
          color: selected ? AppColors.orange500 : colors.bgSurface,
          borderRadius: BorderRadius.circular(999.r),
          border: Border.all(
            color: selected ? AppColors.orange500 : colors.borderDefault,
          ),
          boxShadow: selected
              ? [
                  BoxShadow(
                    color: AppColors.orange500.withValues(alpha: 0.25),
                    blurRadius: 6,
                    offset: const Offset(0, 2),
                  ),
                ]
              : null,
        ),
        child: Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: selected ? Colors.white : colors.textTertiary,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}
