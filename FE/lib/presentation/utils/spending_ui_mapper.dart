import 'package:flutter/material.dart';

class SpendingUiMapper {
  static Color colorFrom(int value) => Color(value);

  static IconData iconFrom(String key) {
    switch (key) {
      case 'restaurant':
        return Icons.restaurant;
      case 'directions_bus':
        return Icons.directions_bus;
      case 'shopping_bag':
        return Icons.shopping_bag;
      case 'subscriptions':
        return Icons.subscriptions;
      case 'sports_esports':
        return Icons.sports_esports;
      case 'more_horiz':
      default:
        return Icons.more_horiz;
    }
  }
}
