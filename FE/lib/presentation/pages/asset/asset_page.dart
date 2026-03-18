import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_sheet.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class AssetPage extends ConsumerStatefulWidget {
  const AssetPage({super.key});

  @override
  ConsumerState<AssetPage> createState() => _AssetPageState();
}

class _AssetPageState extends ConsumerState<AssetPage> {
  bool _isCreditExpanded = false;

  void _showFilterBottomSheet() {
    MoreBottomSheet.show(
      context: context,
      child: const _AssetFilterBottomSheet(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: MoreAppBar(
        title: '',
        backgroundColor: const Color(0xFFFFCC91).withValues(alpha: 0.6),
        showBack: true,
        showNotification: true,
      ),
      body: Stack(
        children: [
          Container(
            height: 120.h,
            width: double.infinity,
            decoration: BoxDecoration(
              color: const Color(0xFFFFCC91).withValues(alpha: 0.6),
            ),
          ),
          SingleChildScrollView(
            physics: const BouncingScrollPhysics(),
            child: Column(
              children: [
                SizedBox(height: 20.h),
                Center(
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () => context.go(AppRoutes.spendingTab),
                    child: Container(
                      padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 10.h),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.9),
                        borderRadius: BorderRadius.circular(30.r),
                        boxShadow: [
                          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 4)),
                        ],
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.lightbulb_outline, size: 18, color: Colors.black87),
                          SizedBox(width: 8.w),
                          Text('소비패턴 알아보기', style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.w600)),
                        ],
                      ),
                    ),
                  ),
                ),
                SizedBox(height: 24.h),
                Padding(
                  padding: EdgeInsets.symmetric(horizontal: 20.w),
                  child: MoreCard(
                    padding: EdgeInsets.all(24.w),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Container(
                              padding: EdgeInsets.all(8.w),
                              decoration: const BoxDecoration(color: Color(0xFFF5F5F5), shape: BoxShape.circle),
                              child: const Icon(Icons.monetization_on_outlined, size: 20, color: Colors.black54),
                            ),
                            SizedBox(width: 12.w),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('총 자산', style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary)),
                                Text(CurrencyFormatter.formatKRW(3250000), style: AppTextStyles.headingLg),
                              ],
                            ),
                            const Spacer(),
                            IconButton(
                              onPressed: () => context.push(AppRoutes.accountManage),
                              icon: const Icon(Icons.menu, size: 24, color: Colors.black87),
                              padding: EdgeInsets.zero,
                              constraints: const BoxConstraints(),
                            ),
                          ],
                        ),
                        SizedBox(height: 24.h),
                        const Divider(color: Color(0xFFEEEEEE)),
                        SizedBox(height: 16.h),
                        GestureDetector(
                          behavior: HitTestBehavior.opaque,
                          onTap: () => setState(() => _isCreditExpanded = !_isCreditExpanded),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Row(
                                children: [
                                  Icon(_isCreditExpanded ? Icons.arrow_drop_down : Icons.arrow_right, size: 24),
                                  Text('신용카드 결제 예정', style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.w500)),
                                ],
                              ),
                              Text('- 1,200,000원', style: AppTextStyles.bodyMd.copyWith(color: const Color(0xFFE57373), fontWeight: FontWeight.w600)),
                            ],
                          ),
                        ),
                        if (_isCreditExpanded) ...[
                          SizedBox(height: 16.h),
                          _buildCreditItem('신한은행', '-550,000원', '03.25 결제 예정'),
                          _buildCreditItem('기업은행', '-400,000원', '04.05 결제 예정'),
                          _buildCreditItem('카카오뱅크', '-250,000원', '04.05 결제 예정'),
                        ],
                      ],
                    ),
                  ),
                ),
                SizedBox(height: 20.h),
                Container(
                  padding: EdgeInsets.all(24.w),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.vertical(top: Radius.circular(24.r)),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('거래내역', style: AppTextStyles.bodyLg.copyWith(fontWeight: FontWeight.bold)),
                      SizedBox(height: 16.h),
                      GestureDetector(
                        behavior: HitTestBehavior.opaque,
                        onTap: _showFilterBottomSheet,
                        child: Container(
                          padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
                          decoration: BoxDecoration(
                            color: const Color(0xFFF8F8F8),
                            borderRadius: BorderRadius.circular(12.r),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.search, size: 18, color: Colors.black38),
                              const Spacer(),
                              Text('3개월·전체·계좌', style: AppTextStyles.caption.copyWith(color: Colors.black38)),
                              const Icon(Icons.arrow_drop_down, size: 18, color: Colors.black38),
                            ],
                          ),
                        ),
                      ),
                      SizedBox(height: 24.h),
                      _buildDateHeader('03.05'),
                      _buildTransactionItem('홍길동', '14:00', '카페', '토스뱅크', '-30,000원(신용)', '03.25 결제 예정', isCredit: true, id: 'credit_1'),
                      _buildTransactionItem('홍길동', '14:00', '카페', '토스뱅크', '90,000원', '3,250,000원', isPlus: true, id: 'income_1'),
                      _buildTransactionItem('홍길동', '14:00', '카페', '토스뱅크', '-90,000원', '3,160,000원', id: 'expense_1'),
                      SizedBox(height: 16.h),
                      _buildDateHeader('03.04'),
                      _buildTransactionItem('홍길동', '14:00', '카페', '토스뱅크', '90,000원', '3,250,000원', isPlus: true, id: 'income_2'),
                      _buildTransactionItem('홍길동', '14:00', '카페', '토스뱅크', '-90,000원', '3,160,000원', id: 'expense_2'),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCreditItem(String bank, String amount, String date) {
    return Padding(
      padding: EdgeInsets.only(left: 32.w, bottom: 12.h),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(bank, style: AppTextStyles.bodySm.copyWith(color: AppColors.textSecondary)),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(amount, style: AppTextStyles.bodySm.copyWith(fontWeight: FontWeight.w600, color: const Color(0xFFAAAAAA))),
              Text(date, style: AppTextStyles.caption.copyWith(color: const Color(0xFFCCCCCC), fontSize: 10.sp)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDateHeader(String date) {
    return Padding(
      padding: EdgeInsets.only(bottom: 16.h),
      child: Text(date, style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary, fontWeight: FontWeight.bold)),
    );
  }

  Widget _buildTransactionItem(String name, String time, String category, String bank, String amount, String balance, {bool isPlus = false, bool isCredit = false, required String id}) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => context.push(AppRoutes.assetDetailLocation(id)),
      child: Padding(
        padding: EdgeInsets.only(bottom: 20.h),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(name, style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold)),
                      SizedBox(width: 8.w),
                      Text(time, style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary)),
                    ],
                  ),
                  SizedBox(height: 4.h),
                  Row(
                    children: [
                      Container(
                        padding: EdgeInsets.symmetric(horizontal: 6.w, vertical: 2.h),
                        decoration: BoxDecoration(color: const Color(0xFFFFCC91).withValues(alpha: 0.4), borderRadius: BorderRadius.circular(10.r)),
                        child: Text(category, style: AppTextStyles.caption.copyWith(color: AppColors.orange500, fontSize: 10.sp)),
                      ),
                      SizedBox(width: 6.w),
                      const Icon(Icons.circle, size: 12, color: Colors.blueAccent),
                      SizedBox(width: 4.w),
                      Text(bank, style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary)),
                    ],
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(amount, style: AppTextStyles.bodyMd.copyWith(
                  fontWeight: FontWeight.bold,
                  color: isPlus ? const Color(0xFF81C784) : (isCredit ? const Color(0xFF64B5F6) : Colors.black87)
                )),
                Text(balance, style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _AssetFilterBottomSheet extends StatefulWidget {
  const _AssetFilterBottomSheet();

  @override
  State<_AssetFilterBottomSheet> createState() => _AssetFilterBottomSheetState();
}

class _AssetFilterBottomSheetState extends State<_AssetFilterBottomSheet> {
  int _periodIdx = 1;
  int _typeIdx = 0;
  int _creditIdx = 0;
  int _accountIdx = 0;
  bool _isAccountExpanded = false;

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: BoxConstraints(maxHeight: MediaQuery.of(context).size.height * 0.85),
      child: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        padding: EdgeInsets.symmetric(horizontal: 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(height: 12.h),
            _buildFilterSection('조회 기간', ['1개월', '3개월', '6개월', '지난달'], _periodIdx, (i) => setState(() => _periodIdx = i)),
            SizedBox(height: 24.h),
            _buildFilterSection('거래 유형', ['전체', '입금만', '출금만'], _typeIdx, (i) => setState(() => _typeIdx = i)),
            SizedBox(height: 24.h),
            _buildFilterSection('신용카드 거래내역 포함 여부', ['포함', '미포함', '신용카드만'], _creditIdx, (i) => setState(() => _creditIdx = i)),
            SizedBox(height: 24.h),
            Text('거래 계좌', style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold)),
            SizedBox(height: 16.h),
            
            Container(
              padding: EdgeInsets.all(16.w),
              decoration: BoxDecoration(
                color: const Color(0xFFF0F0F0),
                borderRadius: BorderRadius.circular(16.r),
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      _buildAccountItem('토스뱅크', 0),
                      _buildAccountItem('토스뱅크', 1),
                      _buildAccountItem('토스뱅크', 2),
                    ],
                  ),
                  if (_isAccountExpanded) ...[
                    SizedBox(height: 16.h),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _buildAccountItem('토스뱅크', 3),
                        _buildAccountItem('토스뱅크', 4),
                        _buildAccountItem('토스뱅크', 5),
                      ],
                    ),
                  ],
                  SizedBox(height: 12.h),
                  GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () => setState(() => _isAccountExpanded = !_isAccountExpanded),
                    child: Icon(
                      _isAccountExpanded ? Icons.arrow_drop_up : Icons.arrow_drop_down,
                      size: 32.w,
                      color: Colors.black54,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 32.h),
            MoreBottomButton(
              text: '완료',
              onPressed: () => Navigator.pop(context),
            ),
            SizedBox(height: 24.h),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterSection(String title, List<String> options, int selectedIdx, Function(int) onTap) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold)),
        SizedBox(height: 12.h),
        Container(
          padding: EdgeInsets.all(4.w),
          decoration: BoxDecoration(color: const Color(0xFFEEEEEE), borderRadius: BorderRadius.circular(12.r)),
          child: IntrinsicHeight(
            child: Stack(
              children: [
                AnimatedAlign(
                  duration: const Duration(milliseconds: 250),
                  curve: Curves.easeInOut,
                  alignment: Alignment(
                    -1.0 + (2.0 * selectedIdx / (options.length - 1)),
                    0.0,
                  ),
                  child: FractionallySizedBox(
                    widthFactor: 1.0 / options.length,
                    heightFactor: 1.0,
                    child: Container(
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(10.r),
                        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 4)],
                      ),
                    ),
                  ),
                ),
                Row(
                  children: options.asMap().entries.map((e) {
                    final isSelected = e.key == selectedIdx;
                    return Expanded(
                      child: GestureDetector(
                        behavior: HitTestBehavior.opaque,
                        onTap: () => onTap(e.key),
                        child: Padding(
                          padding: EdgeInsets.symmetric(vertical: 8.h),
                          child: Center(
                            child: AnimatedDefaultTextStyle(
                              duration: const Duration(milliseconds: 200),
                              style: AppTextStyles.caption.copyWith(
                                color: isSelected ? Colors.black87 : Colors.black38,
                                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                              ),
                              child: Text(e.value),
                            ),
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildAccountItem(String name, int index) {
    final isSelected = _accountIdx == index;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => setState(() => _accountIdx = index),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 6.h),
        decoration: BoxDecoration(
          color: isSelected ? Colors.white : Colors.transparent,
          borderRadius: BorderRadius.circular(10.r),
          boxShadow: isSelected ? [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 4)] : [],
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.circle, size: 14, color: Colors.blueAccent),
            SizedBox(width: 6.w),
            Text(name, style: AppTextStyles.caption.copyWith(
              color: isSelected ? Colors.black87 : Colors.black38,
              fontSize: 11.sp
            )),
          ],
        ),
      ),
    );
  }
}
