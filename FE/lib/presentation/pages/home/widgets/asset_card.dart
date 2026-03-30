import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class AssetCard extends StatelessWidget {
  const AssetCard({
    super.key,
    required this.totalAsset,
    required this.accounts,
  });

  final int totalAsset;
  final List<Account> accounts;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final hasAccounts = accounts.isNotEmpty;
    return MoreCard(
      onTap: () => context.push(hasAccounts ? AppRoutes.asset : AppRoutes.accountAdd),
      padding: EdgeInsets.all(16.w),
      child: hasAccounts
          ? Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '총 자산',
                        style: TextStyle(
                          color: colors.textTertiary,
                          fontSize: 11.sp,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      SizedBox(height: 2.h),
                      FittedBox(
                        fit: BoxFit.scaleDown,
                        alignment: Alignment.centerLeft,
                        child: Text(
                          CurrencyFormatter.formatKRW(totalAsset),
                          style: TextStyle(
                            fontSize: 22.sp,
                            fontWeight: FontWeight.w800,
                            color: colors.textPrimary,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                GestureDetector(
                  onTap: () => context.push(AppRoutes.accountManage),
                  child: Row(
                    children: [
                      Text(
                        '계좌 관리',
                        style: TextStyle(
                          color: colors.textTertiary,
                          fontSize: 12.sp,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      Icon(
                        Icons.chevron_right,
                        size: 16.w,
                        color: colors.textTertiary,
                      ),
                    ],
                  ),
                ),
              ],
            )
          : Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '총 자산',
                  style: TextStyle(
                    color: colors.textTertiary,
                    fontSize: 11.sp,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                SizedBox(height: 6.h),
                Text(
                  CurrencyFormatter.formatKRW(totalAsset),
                  style: TextStyle(
                    fontSize: 22.sp,
                    fontWeight: FontWeight.w800,
                    color: colors.textPrimary,
                  ),
                ),
                SizedBox(height: 10.h),
                Text(
                  '연결된 계좌가 없어요. 계좌를 추가해주세요.',
                  style: TextStyle(
                    color: colors.textTertiary,
                    fontSize: 12.sp,
                    height: 1.4,
                  ),
                ),
                SizedBox(height: 12.h),
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 8.h),
                  decoration: BoxDecoration(
                    color: AppColors.orange50,
                    borderRadius: BorderRadius.circular(999.r),
                  ),
                  child: Text(
                    '계좌 연결하기',
                    style: TextStyle(
                      color: AppColors.orange500,
                      fontSize: 11.sp,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}
