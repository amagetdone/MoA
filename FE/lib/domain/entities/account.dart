class Account {
  final String id;
  final String bankName;
  final String accountNumber;
  final String accountName;
  final int balance;
  final bool isPrimary;
  final bool verified;

  const Account({
    required this.id,
    required this.bankName,
    required this.accountNumber,
    required this.accountName,
    required this.balance,
    this.isPrimary = false,
    this.verified = true,
  });

  Account copyWith({
    String? id,
    String? bankName,
    String? accountNumber,
    String? accountName,
    int? balance,
    bool? isPrimary,
    bool? verified,
  }) {
    return Account(
      id: id ?? this.id,
      bankName: bankName ?? this.bankName,
      accountNumber: accountNumber ?? this.accountNumber,
      accountName: accountName ?? this.accountName,
      balance: balance ?? this.balance,
      isPrimary: isPrimary ?? this.isPrimary,
      verified: verified ?? this.verified,
    );
  }
}
