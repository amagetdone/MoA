import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class OnboardingAnalysisPage extends StatelessWidget {
  const OnboardingAnalysisPage({super.key});

  static const List<_CategoryData> _categories = [
    _CategoryData(name: '식비', percentage: 38, color: Color(0xFFFF6B00)),
    _CategoryData(name: '교통', percentage: 11, color: Color(0xFF4CAF50)),
    _CategoryData(name: '쇼핑', percentage: 16, color: Color(0xFF2196F3)),
    _CategoryData(name: '구독', percentage: 3, color: Color(0xFF9C27B0)),
    _CategoryData(name: '여가', percentage: 9, color: Color(0xFFFF9800)),
    _CategoryData(name: '기타', percentage: 4, color: Color(0xFF607D8B)),
    _CategoryData(name: '저축가능', percentage: 19, color: Color(0xFF00BCD4)),
  ];

  @override
  Widget build(BuildContext context) {
    // Based on available amount of 2,650,000
    const int availableAmount = 2650000;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: MoreAppBar(
        title: '분석 결과',
        showBack: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '소비 패턴을 분석했어요',
              style: AppTextStyles.headingLg.copyWith(
                color: AppColors.gray900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '가용 금액 기준으로 분석한 결과입니다',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray500,
              ),
            ),
            const SizedBox(height: 32),

            // Simple bar chart representation
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: SizedBox(
                height: 32,
                child: Row(
                  children: _categories.map((category) {
                    return Expanded(
                      flex: category.percentage,
                      child: Container(
                        color: category.color,
                      ),
                    );
                  }).toList(),
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Category list
            ..._categories.map((category) {
              final amount =
                  (availableAmount * category.percentage / 100).round();
              return Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: Row(
                  children: [
                    // Color dot
                    Container(
                      width: 12,
                      height: 12,
                      decoration: BoxDecoration(
                        color: category.color,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 12),
                    // Category name
                    Expanded(
                      child: Text(
                        category.name,
                        style: AppTextStyles.bodyLg.copyWith(
                          color: AppColors.gray900,
                        ),
                      ),
                    ),
                    // Percentage
                    SizedBox(
                      width: 48,
                      child: Text(
                        '${category.percentage}%',
                        style: AppTextStyles.bodySm.copyWith(
                          color: AppColors.gray500,
                          fontWeight: FontWeight.w600,
                        ),
                        textAlign: TextAlign.right,
                      ),
                    ),
                    const SizedBox(width: 16),
                    // Amount
                    SizedBox(
                      width: 100,
                      child: Text(
                        _formatAmount(amount),
                        style: AppTextStyles.bodySm.copyWith(
                          color: AppColors.gray700,
                          fontWeight: FontWeight.w500,
                        ),
                        textAlign: TextAlign.right,
                      ),
                    ),
                  ],
                ),
              );
            }),

            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 16),

            // Total
            Row(
              children: [
                Expanded(
                  child: Text(
                    '가용 금액 합계',
                    style: AppTextStyles.bodyLg.copyWith(
                      color: AppColors.gray900,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                Text(
                  _formatAmount(availableAmount),
                  style: AppTextStyles.bodyLg.copyWith(
                    color: AppColors.orange500,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: MoreBottomButton(
            text: '다음',
            onPressed: () {
              context.go(AppRoutes.onboardingPlan);
            },
            enabled: true,
          ),
        ),
      ),
    );
  }

  static String _formatAmount(int amount) {
    final str = amount.toString();
    final buffer = StringBuffer();
    int count = 0;
    for (int i = str.length - 1; i >= 0; i--) {
      buffer.write(str[i]);
      count++;
      if (count % 3 == 0 && i > 0) {
        buffer.write(',');
      }
    }
    return '${buffer.toString().split('').reversed.join()}원';
  }
}

class _CategoryData {
  final String name;
  final int percentage;
  final Color color;

  const _CategoryData({
    required this.name,
    required this.percentage,
    required this.color,
  });
}
