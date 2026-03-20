import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/account_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
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
  String? _selectedAccountNo;
  int _periodIdx = 1; // 0:1개월, 1:3개월, 2:6개월, 3:지난달
  int _typeIdx = 0;   // 0:전체, 1:입금만, 2:출금만
  int _creditIdx = 0; // 0:포함, 1:미포함, 2:카드만
  String _filterLabel = '3개월·전체·전체계좌';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final accounts = ref.read(accountViewModelProvider).accounts;
      _loadPrimaryAccount(accounts);
      _fetchTransactions();
    });
  }

  void _loadPrimaryAccount(List<Account> accounts) {
    if (accounts.isEmpty) return;
    final primary = accounts.firstWhere(
      (a) => a.isPrimary,
      orElse: () => accounts.first,
    );
    setState(() => _selectedAccountNo = primary.accountNumber);
  }

  void _fetchTransactions() {
    final now = DateTime.now();
    String? fromDate;
    String? toDate;

    switch (_periodIdx) {
      case 0: // 1개월
        fromDate = _formatDate(DateTime(now.year, now.month - 1, now.day));
        break;
      case 1: // 3개월
        fromDate = _formatDate(DateTime(now.year, now.month - 3, now.day));
        break;
      case 2: // 6개월
        fromDate = _formatDate(DateTime(now.year, now.month - 6, now.day));
        break;
      case 3: // 지난달
        final lastMonth = DateTime(now.year, now.month - 1, 1);
        fromDate = _formatDate(lastMonth);
        toDate = _formatDate(DateTime(now.year, now.month, 0));
        break;
    }

    ref.read(accountViewModelProvider.notifier).loadTransactions(
      fromDate: fromDate,
      toDate: toDate,
      size: 100,
    );
  }

  String _formatDate(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')}';
  }

  void _showFilterBottomSheet(List<Account> accounts) {
    MoreBottomSheet.show(
      context: context,
      child: _AssetFilterBottomSheet(
        accounts: accounts,
        selectedAccountNo: _selectedAccountNo,
        initialPeriodIdx: _periodIdx,
        initialTypeIdx: _typeIdx,
        initialCreditIdx: _creditIdx,
        onApply: ({
          required int periodIdx,
          required String? accountNo,
          required int typeIdx,
          required int creditIdx,
        }) {
          setState(() {
            _periodIdx = periodIdx;
            _selectedAccountNo = accountNo;
            _typeIdx = typeIdx;
            _creditIdx = creditIdx;
            final periods = ['1개월', '3개월', '6개월', '지난달'];
            final types = ['전체', '입금만', '출금만'];
            final credits = ['포함', '미포함', '카드만'];
            _filterLabel = '${periods[periodIdx]}·${types[typeIdx]}·${credits[creditIdx]}';
          });
          _fetchTransactions();
        },
      ),
    );
  }

  Map<String, List<Transaction>> _groupByDate(List<Transaction> transactions) {
    final Map<String, List<Transaction>> grouped = {};
    for (final t in transactions) {
      final key = DateFormat('MM.dd').format(t.dateTime);
      grouped.putIfAbsent(key, () => []).add(t);
    }
    return grouped;
  }

  @override
  Widget build(BuildContext context) {
    final accountState = ref.watch(accountViewModelProvider);
    final accounts = accountState.accounts;

    var transactions = accountState.transactions;
    // 거래 유형 필터
    if (_typeIdx == 1) transactions = transactions.where((t) => t.type == TransactionType.income).toList();
    if (_typeIdx == 2) transactions = transactions.where((t) => t.type == TransactionType.expense).toList();
    // 신용카드 포함 여부 필터
    if (_creditIdx == 1) transactions = transactions.where((t) => t.sourceType != 'CARD').toList();
    if (_creditIdx == 2) transactions = transactions.where((t) => t.sourceType == 'CARD').toList();
    // 계좌 필터
    if (_selectedAccountNo != null) {
      transactions = transactions.where((t) => t.accountId == _selectedAccountNo).toList();
    }
    final grouped = _groupByDate(transactions);

    // 계좌 목록 로드 후 대표계좌 자동 선택
    ref.listen(accountViewModelProvider.select((s) => s.accounts), (_, next) {
      if (_selectedAccountNo == null && next.isNotEmpty) {
        _loadPrimaryAccount(next);
      }
    });

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
                                Text(CurrencyFormatter.formatKRW(ref.watch(homeViewModelProvider).totalAsset), style: AppTextStyles.headingLg),
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
                        // TODO: 신용카드 결제 예정 API 미지원으로 임시 숨김
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
                        onTap: () => _showFilterBottomSheet(accounts),
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
                              Text(_filterLabel, style: AppTextStyles.caption.copyWith(color: Colors.black38)),
                              const Icon(Icons.arrow_drop_down, size: 18, color: Colors.black38),
                            ],
                          ),
                        ),
                      ),
                      SizedBox(height: 24.h),
                      if (accountState.isLoading)
                        const Center(child: CircularProgressIndicator())
                      else if (transactions.isEmpty)
                        Center(
                          child: Padding(
                            padding: EdgeInsets.symmetric(vertical: 40.h),
                            child: Text('거래내역이 없습니다.', style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary)),
                          ),
                        )
                      else
                        ...grouped.entries.expand((entry) => [
                          _buildDateHeader(entry.key),
                          ...entry.value.map((t) => _buildTransactionItem(t)),
                          SizedBox(height: 16.h),
                        ]),
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

  Widget _buildDateHeader(String date) {
    return Padding(
      padding: EdgeInsets.only(bottom: 16.h),
      child: Text(date, style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary, fontWeight: FontWeight.bold)),
    );
  }

  Widget _buildTransactionItem(Transaction t) {
    final isPlus = t.type == TransactionType.income;
    final amountText = '${isPlus ? '+' : '-'}${CurrencyFormatter.formatKRW(t.amount)}';
    final balanceText = CurrencyFormatter.formatKRW(t.balanceAfter);
    final timeText = DateFormat('HH:mm').format(t.dateTime);

    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => context.push(AppRoutes.assetDetail, extra: t),
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
                      Text(
                        t.title.length > 10 ? '${t.title.substring(0, 10)}...' : t.title,
                        style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.bold),
                      ),
                      SizedBox(width: 8.w),
                      Text(timeText, style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary)),
                    ],
                  ),
                  SizedBox(height: 4.h),
                  Row(
                    children: [
                      Container(
                        padding: EdgeInsets.symmetric(horizontal: 6.w, vertical: 2.h),
                        decoration: BoxDecoration(color: const Color(0xFFFFCC91).withValues(alpha: 0.4), borderRadius: BorderRadius.circular(10.r)),
                        child: Text(t.category, style: AppTextStyles.caption.copyWith(color: AppColors.orange500, fontSize: 10.sp)),
                      ),
                      if (t.memo != null) ...[
                        SizedBox(width: 6.w),
                        Text(t.memo!, style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary)),
                      ],
                    ],
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(amountText, style: AppTextStyles.bodyMd.copyWith(
                  fontWeight: FontWeight.bold,
                  color: isPlus ? const Color(0xFF81C784) : Colors.black87,
                )),
                Text(balanceText, style: AppTextStyles.caption.copyWith(color: AppColors.textTertiary)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _AssetFilterBottomSheet extends StatefulWidget {
  final List<Account> accounts;
  final String? selectedAccountNo;
  final int initialPeriodIdx;
  final int initialTypeIdx;
  final int initialCreditIdx;
  final void Function({
    required int periodIdx,
    required String? accountNo,
    required int typeIdx,
    required int creditIdx,
  }) onApply;

  const _AssetFilterBottomSheet({
    required this.accounts,
    required this.selectedAccountNo,
    required this.onApply,
    this.initialPeriodIdx = 1,
    this.initialTypeIdx = 0,
    this.initialCreditIdx = 0,
  });

  @override
  State<_AssetFilterBottomSheet> createState() => _AssetFilterBottomSheetState();
}

class _AssetFilterBottomSheetState extends State<_AssetFilterBottomSheet> {
  late int _periodIdx;
  int _typeIdx = 0;
  int _creditIdx = 0;
  String? _selectedAccountNo;
  bool _isAccountExpanded = false;

  @override
  void initState() {
    super.initState();
    _periodIdx = widget.initialPeriodIdx;
    _typeIdx = widget.initialTypeIdx;
    _creditIdx = widget.initialCreditIdx;
    _selectedAccountNo = widget.selectedAccountNo;
  }

  @override
  Widget build(BuildContext context) {
    final accounts = widget.accounts;
    final firstRow = accounts.take(3).toList();
    final secondRow = accounts.length > 3 ? accounts.skip(3).take(3).toList() : <Account>[];

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
                    children: firstRow.map((a) => _buildAccountItem(a)).toList(),
                  ),
                  if (_isAccountExpanded && secondRow.isNotEmpty) ...[
                    SizedBox(height: 16.h),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: secondRow.map((a) => _buildAccountItem(a)).toList(),
                    ),
                  ],
                  if (accounts.length > 3) ...[
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
                ],
              ),
            ),
            SizedBox(height: 32.h),
            MoreBottomButton(
              text: '완료',
              onPressed: () {
                widget.onApply(
                  periodIdx: _periodIdx,
                  accountNo: _selectedAccountNo,
                  typeIdx: _typeIdx,
                  creditIdx: _creditIdx,
                );
                Navigator.pop(context);
              },
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

  Widget _buildAccountItem(Account account) {
    final isSelected = _selectedAccountNo == account.accountNumber;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => setState(() => _selectedAccountNo = account.accountNumber),
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
            Text(account.bankName, style: AppTextStyles.caption.copyWith(
              color: isSelected ? Colors.black87 : Colors.black38,
              fontSize: 11.sp,
            )),
          ],
        ),
      ),
    );
  }
}
