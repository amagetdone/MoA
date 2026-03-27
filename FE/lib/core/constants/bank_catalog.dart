class BankCatalogItem {
  final String? code;
  final String name;
  final String? logoPath;
  final List<String> aliases;

  const BankCatalogItem({
    required this.name,
    this.code,
    this.logoPath,
    this.aliases = const [],
  });
}

class BankCatalog {
  static const List<BankCatalogItem> all = [
    BankCatalogItem(
      code: '004',
      name: '국민은행',
      logoPath: 'assets/images/logo/KB_logo.png',
      aliases: ['KB국민은행', 'KB은행'],
    ),
    BankCatalogItem(
      code: '088',
      name: '신한은행',
      logoPath: 'assets/images/logo/shinhan_logo.png',
    ),
    BankCatalogItem(
      code: '020',
      name: '우리은행',
      logoPath: 'assets/images/logo/woori_logo.png',
    ),
    BankCatalogItem(
      code: '081',
      name: '하나은행',
      logoPath: 'assets/images/logo/KEB_hana_logo.png',
      aliases: ['KEB하나은행'],
    ),
    BankCatalogItem(
      code: '003',
      name: '기업은행',
      logoPath: 'assets/images/logo/ibk_logo.png',
      aliases: ['IBK기업은행'],
    ),
    BankCatalogItem(
      code: '032',
      name: 'iM뱅크',
      logoPath: 'assets/images/logo/iM_logo.png',
      aliases: ['iM은행', '대구은행'],
    ),
    BankCatalogItem(
      code: '090',
      name: '카카오뱅크',
      logoPath: 'assets/images/logo/kakaobank.png',
    ),
    BankCatalogItem(
      code: '011',
      name: '농협은행',
      logoPath: 'assets/images/logo/nonghyup_logo.png',
      aliases: ['NH농협은행'],
    ),
    BankCatalogItem(
      code: '045',
      name: '새마을금고',
      logoPath: 'assets/images/logo/mg_logo.png',
    ),
    BankCatalogItem(
      code: '023',
      name: 'SC제일은행',
      logoPath: 'assets/images/logo/sc_jeil_logo.png',
    ),
    BankCatalogItem(
      code: '027',
      name: '씨티은행',
      logoPath: 'assets/images/logo/citi_logo.png',
      aliases: ['시티은행', '한국씨티은행'],
    ),
    BankCatalogItem(
      code: '002',
      name: '산업은행',
      logoPath: 'assets/images/logo/industry_logo.png',
      aliases: ['KDB산업은행'],
    ),
    BankCatalogItem(
      code: '039',
      name: '경남은행',
      logoPath: 'assets/images/logo/kyung_nam_logo.png',
    ),
    BankCatalogItem(
      code: '034',
      name: '광주은행',
      logoPath: 'assets/images/logo/gwangju_logo.jpg',
    ),
    BankCatalogItem(
      code: '037',
      name: '전북은행',
      logoPath: 'assets/images/logo/gwangju_logo.jpg',
      aliases: ['JB전북은행'],
    ),
    BankCatalogItem(
      code: '035',
      name: '제주은행',
      logoPath: 'assets/images/logo/shinhan_logo.png',
    ),
    BankCatalogItem(
      code: '007',
      name: '수협은행',
      logoPath: 'assets/images/logo/suhyup_logo.png',
      aliases: ['SH수협은행'],
    ),
    BankCatalogItem(
      code: '001',
      name: '한국은행',
    ),
    BankCatalogItem(
      code: '999',
      name: '싸피은행',
    ),
  ];

  static BankCatalogItem? findByCode(String? code) {
    if (code == null || code.isEmpty) {
      return null;
    }

    for (final bank in all) {
      if (bank.code == code) {
        return bank;
      }
    }
    return null;
  }

  static BankCatalogItem? findByName(String? bankName) {
    if (bankName == null || bankName.trim().isEmpty) {
      return null;
    }

    final normalized = _normalize(bankName);
    for (final bank in all) {
      if (_normalize(bank.name) == normalized) {
        return bank;
      }
      for (final alias in bank.aliases) {
        if (_normalize(alias) == normalized) {
          return bank;
        }
      }
    }
    return null;
  }

  static String _normalize(String value) {
    return value.replaceAll(' ', '').trim().toLowerCase();
  }
}
