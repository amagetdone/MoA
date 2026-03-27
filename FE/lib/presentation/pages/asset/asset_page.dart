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
  const AssetPage({super.key, this.initialSelectedAccountNo});

  final String? initialSelectedAccountNo;

  @override
  ConsumerState<AssetPage> createState() => _AssetPageState();
}

class _AssetPageState extends ConsumerState<AssetPage> {
  String? _selectedAccountNo;
  int _periodIdx = 1; // 0:1개월, 1:3개월, 2:6개월, 3:지난달
  int _typeIdx = 0; // 0:전체, 1:입금만, 2:출금만
  int _creditIdx = 0; // 0:포함, 1:미포함, 2:카드만
  String _filterLabel = '3개월·전체·전체계좌';

  @override
  void initState() {
    super.initState();
    _selectedAccountNo = widget.initialSelectedAccountNo;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final accounts = ref.read(accountViewModelProvider).accounts;
      if (_selectedAccountNo == null) {
        _loadPrimaryAccount(accounts);
      }
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

    ref
        .read(accountViewModelProvider.notifier)
        .loadTransactions(fromDate: fromDate, toDate: toDate, size: 100);
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
        onApply:
            ({
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
                final types = ['전체', '입금', '출금'];
                final credits = ['계좌+카드', '계좌', '카드'];
                _filterLabel =
                    '${periods[periodIdx]}·${types[typeIdx]}·${credits[creditIdx]}';
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
    final colors = AppColors.of(context);
    final accountState = ref.watch(accountViewModelProvider);
    final accounts = accountState.accounts;
    final isTransactionsLoading =
        accountState.isTransactionsLoading ||
        !accountState.hasLoadedTransactions;

    var transactions = accountState.transactions;
    // 거래 유형 필터
    if (_typeIdx == 1) {
      transactions = transactions
          .where((t) => t.type == TransactionType.income)
          .toList();
    }
    if (_typeIdx == 2) {
      transactions = transactions
          .where((t) => t.type == TransactionType.expense)
          .toList();
    }
    // 신용카드 포함 여부 필터
    if (_creditIdx == 1) {
      transactions = transactions.where((t) => t.sourceType != 'CARD').toList();
    }
    if (_creditIdx == 2) {
      transactions = transactions.where((t) => t.sourceType == 'CARD').toList();
    }
    // 계좌 필터 (카드 거래는 accountId가 없으므로 제외)
    if (_selectedAccountNo != null) {
      transactions = transactions
          .where(
            (t) => t.accountId == _selectedAccountNo || t.sourceType == 'CARD',
          )
          .toList();
    }
    final grouped = _groupByDate(transactions);

    // 계좌 목록 로드 후 대표계좌 자동 선택
    ref.listen(accountViewModelProvider.select((s) => s.accounts), (_, next) {
      if (_selectedAccountNo == null && next.isNotEmpty) {
        _loadPrimaryAccount(next);
      }
    });

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: MoreAppBar(
        title: '',
        backgroundColor: colors.bgAccent,
        showBack: true,
        showNotification: false,
      ),
      body: Stack(
        children: [
          Container(
            height: 120.h,
            width: double.infinity,
            decoration: BoxDecoration(color: colors.bgAccent),
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
                      padding: EdgeInsets.symmetric(
                        horizontal: 24.w,
                        vertical: 10.h,
                      ),
                      decoration: BoxDecoration(
                        color: colors.bgSurface.withValues(alpha: 0.9),
                        borderRadius: BorderRadius.circular(30.r),
                        boxShadow: [
                          BoxShadow(
                            color: colors.cardShadow,
                            blurRadius: 10,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.lightbulb_outline,
                            size: 18,
                            color: colors.textPrimary,
                          ),
                          SizedBox(width: 8.w),
                          Text(
                            '소비패턴 알아보기',
                            style: AppTextStyles.bodyMd.copyWith(
                              color: colors.textPrimary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
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
                              decoration: BoxDecoration(
                                color: colors.gray50,
                                shape: BoxShape.circle,
                              ),
                              child: Icon(
                                Icons.monetization_on_outlined,
                                size: 20,
                                color: colors.textTertiary,
                              ),
                            ),
                            SizedBox(width: 12.w),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  '총 자산',
                                  style: AppTextStyles.caption.copyWith(
                                    color: colors.textTertiary,
                                  ),
                                ),
                                Text(
                                  CurrencyFormatter.formatKRW(
                                    ref.watch(homeViewModelProvider).totalAsset,
                                  ),
                                  style: AppTextStyles.headingMd.copyWith(
                                    color: colors.textPrimary,
                                  ),
                                ),
                              ],
                            ),
                            const Spacer(),
                            IconButton(
                              onPressed: () =>
                                  context.push(AppRoutes.accountManage),
                              icon: Icon(
                                Icons.menu,
                                size: 24,
                                color: colors.textPrimary,
                              ),
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
                    color: colors.bgSurface,
                    borderRadius: BorderRadius.vertical(
                      top: Radius.circular(24.r),
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '거래내역',
                        style: AppTextStyles.bodyMd.copyWith(
                          color: colors.textPrimary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(height: 16.h),
                      GestureDetector(
                        behavior: HitTestBehavior.opaque,
                        onTap: () => _showFilterBottomSheet(accounts),
                        child: Container(
                          padding: EdgeInsets.symmetric(
                            horizontal: 16.w,
                            vertical: 8.h,
                          ),
                          decoration: BoxDecoration(
                            color: colors.gray50,
                            borderRadius: BorderRadius.circular(12.r),
                          ),
                          child: Row(
                            children: [
                              Icon(
                                Icons.search,
                                size: 18,
                                color: colors.dimText,
                              ),
                              const Spacer(),
                              Text(
                                _filterLabel,
                                style: AppTextStyles.caption.copyWith(
                                  color: colors.dimText,
                                ),
                              ),
                              Icon(
                                Icons.arrow_drop_down,
                                size: 18,
                                color: colors.dimText,
                              ),
                            ],
                          ),
                        ),
                      ),
                      SizedBox(height: 24.h),
                      if (isTransactionsLoading)
                        const Center(child: CircularProgressIndicator())
                      else if (transactions.isEmpty)
                        Center(
                          child: Padding(
                            padding: EdgeInsets.symmetric(vertical: 40.h),
                            child: Text(
                              '거래내역이 없습니다.',
                              style: AppTextStyles.bodySm.copyWith(
                                color: colors.textTertiary,
                              ),
                            ),
                          ),
                        )
                      else
                        ...grouped.entries.expand(
                          (entry) => [
                            _buildDateHeader(entry.key),
                            ...entry.value.map((t) => _buildTransactionItem(t)),
                            SizedBox(height: 16.h),
                          ],
                        ),
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
    final colors = AppColors.of(context);
    return Padding(
      padding: EdgeInsets.only(bottom: 16.h),
      child: Text(
        date,
        style: AppTextStyles.caption.copyWith(
          color: colors.textTertiary,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Color _categoryColor(String name) {
    switch (name) {
      case '교육비':
        return const Color(0xFFB39DDB);
      case '교통비':
        return const Color(0xFF90CAF9);
      case '구독료':
        return const Color(0xFF80CBC4);
      case '금융/보험':
        return const Color(0xFFA5D6A7);
      case '기타서비스':
        return const Color(0xFFB0BEC5);
      case '문화/여가/여행':
        return const Color(0xFFF48FB1);
      case '미용':
        return const Color(0xFFF8BBD0);
      case '생활/반려동물':
        return const Color(0xFFC5E1A5);
      case '세금/공과금':
        return const Color(0xFFFFCC80);
      case '쇼핑/패션':
        return const Color(0xFFCE93D8);
      case '식비':
        return const Color(0xFFFFAB91);
      case '의료/건강':
        return const Color(0xFF80DEEA);
      case '주거/통신':
        return const Color(0xFF90A4AE);
      case '카페':
        return const Color(0xFFBCAAA4);
      case '편의점':
        return const Color(0xFFFFF176);
      default:
        return const Color(0xFFB0BEC5);
    }
  }

  Color _categoryTextColor(String name, AppColorScheme colors) {
    final chipBackground = Color.alphaBlend(
      _categoryColor(name).withValues(alpha: 0.35),
      colors.bgSurface,
    );
    return chipBackground.computeLuminance() > 0.5
        ? colors.textPrimary
        : Colors.white;
  }

  Widget _buildTransactionItem(Transaction t) {
    final colors = AppColors.of(context);
    final isPlus = t.type == TransactionType.income;
    final showCategoryChip = !isPlus && t.category.trim().isNotEmpty;
    final showBalanceText = t.balanceAfter > 0;
    final showMemoText =
        t.memo != null &&
        t.memo!.toUpperCase() != 'VARIABLE' &&
        t.memo!.toUpperCase() != 'FIXED';
    final showMetaRow = showCategoryChip || showMemoText;
    final amountText =
        '${isPlus ? '+' : '-'}${CurrencyFormatter.formatKRW(t.amount)}';
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
                        t.title.length > 10
                            ? '${t.title.substring(0, 10)}...'
                            : t.title,
                        style: AppTextStyles.bodyMd.copyWith(
                          color: colors.textPrimary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(width: 8.w),
                      Text(
                        timeText,
                        style: AppTextStyles.caption.copyWith(
                          color: colors.textTertiary,
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 4.h),
                  if (showMetaRow)
                    Row(
                      children: [
                        if (showCategoryChip)
                          Container(
                            padding: EdgeInsets.symmetric(
                              horizontal: 6.w,
                              vertical: 2.h,
                            ),
                            decoration: BoxDecoration(
                              color: _categoryColor(
                                t.category,
                              ).withValues(alpha: 0.35),
                              borderRadius: BorderRadius.circular(10.r),
                            ),
                            child: Text(
                              t.category,
                              style: AppTextStyles.caption.copyWith(
                                color: _categoryTextColor(t.category, colors),
                                fontSize: 10.sp,
                              ),
                            ),
                          ),
                        if (showMemoText) ...[
                          if (showCategoryChip) SizedBox(width: 6.w),
                          Text(
                            t.memo!,
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                            ),
                          ),
                        ],
                      ],
                    ),
                  if (!showMetaRow) SizedBox(height: 20.h),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  amountText,
                  style: AppTextStyles.bodyMd.copyWith(
                    fontWeight: FontWeight.bold,
                    color: isPlus ? AppColors.green500 : colors.textPrimary,
                  ),
                ),
                if (showBalanceText)
                  Text(
                    CurrencyFormatter.formatKRW(t.balanceAfter),
                    style: AppTextStyles.caption.copyWith(
                      color: colors.textTertiary,
                    ),
                  ),
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
  })
  onApply;

  const _AssetFilterBottomSheet({
    required this.accounts,
    required this.selectedAccountNo,
    required this.onApply,
    this.initialPeriodIdx = 1,
    this.initialTypeIdx = 0,
    this.initialCreditIdx = 0,
  });

  @override
  State<_AssetFilterBottomSheet> createState() =>
      _AssetFilterBottomSheetState();
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
    final colors = AppColors.of(context);
    final accounts = widget.accounts;
    final firstRow = accounts.take(3).toList();
    final secondRow = accounts.length > 3
        ? accounts.skip(3).take(3).toList()
        : <Account>[];

    return ConstrainedBox(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.85,
      ),
      child: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        padding: EdgeInsets.symmetric(horizontal: 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(height: 12.h),
            _buildFilterSection(
              '조회 기간',
              ['1개월', '3개월', '6개월', '지난달'],
              _periodIdx,
              (i) => setState(() => _periodIdx = i),
            ),
            SizedBox(height: 24.h),
            _buildFilterSection(
              '거래 유형',
              ['전체', '입금', '출금'],
              _typeIdx,
              (i) => setState(() => _typeIdx = i),
            ),
            SizedBox(height: 24.h),
            _buildFilterSection(
              '거래 내역',
              ['계좌+카드', '계좌', '카드'],
              _creditIdx,
              (i) => setState(() => _creditIdx = i),
            ),
            SizedBox(height: 24.h),
            Text(
              '거래 계좌',
              style: AppTextStyles.bodyMd.copyWith(
                color: colors.textPrimary,
                fontWeight: FontWeight.bold,
              ),
            ),
            SizedBox(height: 16.h),
            Container(
              padding: EdgeInsets.all(16.w),
              decoration: BoxDecoration(
                color: colors.bgAccent,
                borderRadius: BorderRadius.circular(16.r),
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: firstRow
                        .map((a) => _buildAccountItem(a))
                        .toList(),
                  ),
                  if (_isAccountExpanded && secondRow.isNotEmpty) ...[
                    SizedBox(height: 16.h),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: secondRow
                          .map((a) => _buildAccountItem(a))
                          .toList(),
                    ),
                  ],
                  if (accounts.length > 3) ...[
                    SizedBox(height: 12.h),
                    GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: () => setState(
                        () => _isAccountExpanded = !_isAccountExpanded,
                      ),
                      child: Icon(
                        _isAccountExpanded
                            ? Icons.arrow_drop_up
                            : Icons.arrow_drop_down,
                        size: 32.w,
                        color: colors.textSecondary,
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

  Widget _buildFilterSection(
    String title,
    List<String> options,
    int selectedIdx,
    Function(int) onTap,
  ) {
    final colors = AppColors.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: AppTextStyles.bodyMd.copyWith(
            color: colors.textPrimary,
            fontWeight: FontWeight.bold,
          ),
        ),
        SizedBox(height: 12.h),
        Container(
          padding: EdgeInsets.all(4.w),
          decoration: BoxDecoration(
            color: colors.borderDefault,
            borderRadius: BorderRadius.circular(12.r),
          ),
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
                        color: colors.bgSurface,
                        borderRadius: BorderRadius.circular(10.r),
                        boxShadow: [
                          BoxShadow(color: colors.cardShadow, blurRadius: 4),
                        ],
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
                                color: isSelected
                                    ? colors.textPrimary
                                    : colors.dimText,
                                fontWeight: isSelected
                                    ? FontWeight.bold
                                    : FontWeight.normal,
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
    final colors = AppColors.of(context);
    final isSelected = _selectedAccountNo == account.accountNumber;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => setState(() => _selectedAccountNo = account.accountNumber),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 6.h),
        decoration: BoxDecoration(
          color: isSelected ? colors.bgSurface : Colors.transparent,
          borderRadius: BorderRadius.circular(10.r),
          boxShadow: isSelected
              ? [BoxShadow(color: colors.cardShadow, blurRadius: 4)]
              : [],
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.circle, size: 14, color: Colors.blueAccent),
            SizedBox(width: 6.w),
            Text(
              account.bankName,
              style: AppTextStyles.caption.copyWith(
                color: isSelected ? colors.textPrimary : colors.dimText,
                fontSize: 11.sp,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
