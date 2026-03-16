import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/bank_icon.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class AssetCard extends StatefulWidget {
  const AssetCard({
    super.key,
    required this.totalAsset,
    required this.accounts,
  });

  final int totalAsset;
  final List<Account> accounts;

  @override
  State<AssetCard> createState() => _AssetCardState();
}

class _AssetCardState extends State<AssetCard> {
  final _pageController = PageController();
  int _currentPage = 0;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final hasAccounts = widget.accounts.isNotEmpty;

    return MoreCard(
      padding: EdgeInsets.fromLTRB(22.w, 22.h, 22.w, 16.h),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 42.w,
                height: 42.w,
                decoration: BoxDecoration(
                  color: const Color(0xFFF1F1F1),
                  borderRadius: BorderRadius.circular(14.r),
                ),
                child: Icon(
                  Icons.monetization_on_outlined,
                  color: AppColors.textPrimary,
                  size: 22.w,
                ),
              ),
              SizedBox(width: 14.w),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '총 자산',
                      style: AppTextStyles.bodyLg.copyWith(
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    SizedBox(height: 2.h),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      alignment: Alignment.centerLeft,
                      child: Text(
                        CurrencyFormatter.formatKRW(widget.totalAsset),
                        style: AppTextStyles.headingLg.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          SizedBox(height: 16.h),
          if (hasAccounts) ...[
            LayoutBuilder(
              builder: (context, constraints) {
                final panelHeight = constraints.maxWidth * 0.24;
                return SizedBox(
                  height: panelHeight,
                  child: PageView.builder(
                    controller: _pageController,
                    itemCount: widget.accounts.length,
                    onPageChanged: (index) =>
                        setState(() => _currentPage = index),
                    itemBuilder: (context, index) => Padding(
                      padding: EdgeInsets.only(right: 10.w),
                      child: _AccountPanel(account: widget.accounts[index]),
                    ),
                  ),
                );
              },
            ),
            if (widget.accounts.length > 1) ...[
              SizedBox(height: 10.h),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(widget.accounts.length, (index) {
                  final isActive = index == _currentPage;
                  return AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    margin: EdgeInsets.symmetric(horizontal: 3.w),
                    width: isActive ? 16.w : 6.w,
                    height: 6.h,
                    decoration: BoxDecoration(
                      color: isActive
                          ? AppColors.textPrimary
                          : AppColors.gray500.withValues(alpha: 0.35),
                      borderRadius: BorderRadius.circular(999.r),
                    ),
                  );
                }),
              ),
            ],
          ] else
            Text(
              '대표 계좌를 연결하면 여기서 바로 확인할 수 있어요.',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          SizedBox(height: 12.h),
          Row(
            children: [
              Expanded(
                child: _FooterAction(
                  label: '거래내역',
                  onTap: () => context.push(AppRoutes.asset),
                ),
              ),
              SizedBox(width: 12.w),
              Expanded(
                child: _FooterAction(
                  label: '계좌관리',
                  onTap: () => context.push(AppRoutes.accountManage),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _AccountPanel extends StatelessWidget {
  const _AccountPanel({required this.account});

  final Account account;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.w),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F7F7),
        borderRadius: BorderRadius.circular(20.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 16.r,
            offset: Offset(0, 4.h),
          ),
        ],
      ),
      child: Row(
        children: [
          BankIcon(bankName: account.bankName, size: 44.w),
          SizedBox(width: 14.w),
          Expanded(child: _AccountMeta(account: account)),
          SizedBox(width: 12.w),
          Flexible(
            child: FittedBox(
              fit: BoxFit.scaleDown,
              alignment: Alignment.centerRight,
              child: Text(
                CurrencyFormatter.formatKRW(account.balance),
                style: AppTextStyles.headingLg.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _AccountMeta extends StatelessWidget {
  const _AccountMeta({required this.account});

  final Account account;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          account.bankName,
          style: AppTextStyles.bodyLg.copyWith(
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w600,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        SizedBox(height: 4.h),
        Text(
          account.isPrimary ? '주계좌' : '연결 계좌',
          style: AppTextStyles.bodySm.copyWith(
            color: AppColors.textTertiary,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }
}

class _FooterAction extends StatelessWidget {
  const _FooterAction({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12.r),
      child: Padding(
        padding: EdgeInsets.symmetric(vertical: 10.h),
        child: Center(
          child: Text(
            label,
            style: AppTextStyles.bodyLg.copyWith(
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}
